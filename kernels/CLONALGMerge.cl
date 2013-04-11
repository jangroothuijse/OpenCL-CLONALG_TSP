#define PATH_LAST_INDEX		PATH_LENGTH - 1
#define DISTANCE(d, i, j)	d[i * PATH_LENGTH + j]
#define DIST(i, j)			d[i * PATH_LENGTH + j]
// Starting population, selected population to clone Must be 2^n
#define BETA 				(POP_MAX - SELECTION_SIZE) / SELECTION_SIZE		

// Setup for 24 bit radix sort
#define RADIX_BPB			6		// number of bits per bucket
#define RADIX_I				4		// number of iterations
#define RADIX_P				262144	// (0.0, 64.0]) * RADIX_P = maximum precision

#define RANDA 				4294883355UL	// @See MWC Mark Goresky and Andrew Klapper
// http://cas.ee.ic.ac.uk/people/dt10/research/rngs-gpu-mwc64x.html
// calculates a new random number, based on a seed (old ramdom number)
#define RAND(last)			( (last & 0xFFFFFFFF) * RANDA + (last >> 32) )
// very specific macro...depends on long variable random beeing present in scope 
#define RANDOM				( random = RAND(random) )

typedef struct Path {
	float distance;
	uchar vertices[PATH_LENGTH];
} Path;

float pathDist(float* distances, int* path) {
	float result = 0;
	int start = 0;
	for (int i = 1; i < PATH_LENGTH; i++) {
		result += DISTANCE(distances, start, i);
		start++;
	}
	result += DISTANCE(distances, start, 0);
	return result;
}

__kernel void CLONALG(
		global int *output, 				// 0 
		global const float *d, 				// 1
		global const ulong* randomSeeds,	// 2
		global Path* paths, 				// 3
		global ushort* t1, 					// 4
		global ushort* t2, 					// 5
		uint const popSize					// 6
		) {
			
	// global memory layout
	global int* bestSolution;
	global float* bestAffinity;
	
	// local memory layout
	
	// private memory layout
	private int globalId = get_global_id(0);
	private int globalSize = get_global_size(0);
	private int localId = get_local_id(0);
	private int localSize = get_local_size(0);
	private long random = RAND(randomSeeds[globalId]);
	
	////	INIT
	for (int i = 0; i < POP_MAX; i++) t1[i] = t2[i] = i;	
	
	// Generate random population...Translation table = id
	ushort randNr = globalId;
	for (; randNr < PATH_LENGTH; randNr += globalSize) {
		for (int i = 0; i < PATH_LENGTH; i++) paths[randNr].vertices[i] = i;
		float a = 0;
		uint tmp = paths[randNr].vertices[0];
		uint current = paths[randNr].vertices[0] = randNr;
		paths[randNr].vertices[randNr] = tmp;
		uint next = 0;
		for (uint i = 0; i < PATH_LAST_INDEX; i++) {
			current = paths[randNr].vertices[i];
			float distance = 1000000.0f;	
			for (uint j = i+1; j < PATH_LENGTH; j++) {
				int node = paths[randNr].vertices[j];
				if	(d[current * PATH_LENGTH + node] < distance) {
					next = j;
					distance = d[current * PATH_LENGTH + node];
				}
			}
			// swap i with j
			int tmp = paths[randNr].vertices[next];
			paths[randNr].vertices[next] = paths[randNr].vertices[i+1];
			paths[randNr].vertices[i+1] = tmp;
			a += distance;
			current = next;
		}
		paths[randNr].distance = a + DIST(paths[randNr].vertices[0], 
								paths[randNr].vertices[PATH_LAST_INDEX]);
	}
	
	for (; randNr < POP_MAX; randNr += globalSize)  {
		int i = randNr;
		t1[i] = i;
		float a = 0;
		for (uchar j = 0; j < PATH_LENGTH; j++) {
			paths[i].vertices[j] = j;
		}
		
		for (uchar j = 0; j < PATH_LENGTH; j++) {
			uchar range = PATH_LENGTH - j;
			uchar randomIndex = j + (convert_uchar(RANDOM) % range);
			uchar tmpElement = paths[i].vertices[randomIndex];
			paths[i].vertices[randomIndex] = paths[i].vertices[j];
			paths[i].vertices[j] = tmpElement;
			if (j > 0) {
				a += DISTANCE(d, paths[i].vertices[j-1], tmpElement);
			}
		}		
		paths[i].distance = a + DISTANCE(d, paths[i].vertices[PATH_LAST_INDEX], paths[i].vertices[0]);
	}
	
	barrier(CLK_GLOBAL_MEM_FENCE);
	
	////	GENERATIONS
	
	bool sane = true;
	
	for (int i = 0; i <= GENERATIONS + 1 && sane; i++) {
		///		Sort
		// 2 Element presort to kick of the merge sort
		for (ushort j = globalId; j < POP_MAX; j += 2 * globalSize) {
			ushort tmp;
			if (paths[t1[j + 1]].distance < paths[t1[j]].distance) {
				tmp = t1[j];
				t1[j] = t1[j + 1];
				t1[j + 1] = tmp;
			}
		}
		barrier(CLK_GLOBAL_MEM_FENCE);
		// Merge sort
		for (int chunkSize = 2; chunkSize < POP_MAX; chunkSize <<= 1) {
			// Max chunckSize = size / 2
			int totalSize = 2 * chunkSize;
			int shiftSize = globalSize * totalSize;
			for (int offset = globalId * totalSize; offset < POP_MAX; offset += shiftSize) {
				int split = offset + chunkSize;	
				int end = offset + totalSize;
				
				// Init buffer
				for (int j = offset; j < end; j++) {
					t2[j] = t1[j];
				}
				
				int ai = 0;	// pointer in first chunk
				int bi = 0; // pointer in second chunk
				
				for (int j = offset; j < end; j++) {
					if (ai >= chunkSize) {
						t1[j] = t2[split+bi];
						bi++;
					} else if (bi >= chunkSize) {
						t1[j] = t2[offset+ai];
						ai++;
					} else if (paths[t2[offset+ai]].distance < paths[t2[split+bi]].distance) {
						t1[j] = t2[offset+ai];
						ai++;
					} else {
						t1[j] = t2[split+bi];
						bi++;
					}
				}
			}
			barrier(CLK_GLOBAL_MEM_FENCE);
		}
		
		barrier(CLK_GLOBAL_MEM_FENCE);
		
		if (i < GENERATIONS) {
			///		Selection
			//		t1 contains addresses of sorted paths
			//		after index SELECTION_SIZE space will be reclaimed for clones
			//		so no explicit selection is needed.
			
			///		Clone (Use affinities and beta's)
			for (uint j = globalId; j < SELECTION_SIZE; j += globalSize) {
				// Only 1 worker has value j
				ushort realIndex = t1[j];
				uchar cpy[PATH_LENGTH];
				for (int k = 0; k < PATH_LENGTH; k++) {
					cpy[k] = paths[realIndex].vertices[k];
				}
				
				for (uint k = 0; k < BETA; k++) {
					uchar clone[PATH_LENGTH];
					for (int l = 0; l < PATH_LENGTH; l++) {
						clone[l] = cpy[l];
					}
										
					//		Actual mutations
					for (uchar l = 0; l < MUTATIONS; l++) {
						uchar ri1 = convert_uchar(RANDOM % PATH_LENGTH) % PATH_LENGTH;
						uchar ri2 = convert_uchar(RANDOM % PATH_LENGTH) % PATH_LENGTH;
						uint tmp = clone[ri2];						
						clone[ri2] = clone[ri1]; 
						clone[ri1] = tmp;
					}
					
					private ushort cloneRealIndex = t1[SELECTION_SIZE + (BETA * j) + k];

					///		Calculate affinities
					
					for (uchar l = 0; l < PATH_LENGTH; l++) {
						paths[cloneRealIndex].vertices[l] = clone[l];
					}
					
					float a = 0;
					uchar current = 0;
					uchar last;
					uchar first;
					for (uchar l = 0; l < PATH_LENGTH; l++) {	
						current = clone[l];
						if (l > 0) {
							a += DISTANCE(d, last, current);
						} else {
							first = current;
						}
						last = current;
					}
					paths[cloneRealIndex].distance = a + DISTANCE(d, last, first);
				}
			}
			barrier(CLK_GLOBAL_MEM_FENCE);
		}
		barrier(CLK_GLOBAL_MEM_FENCE);
	}	
	
	if (globalId == 0) {
		for (int i = 0; i < PATH_LENGTH; i++) {
			output[i] = convert_int(paths[t1[0]].vertices[i]);
		}
	}
}