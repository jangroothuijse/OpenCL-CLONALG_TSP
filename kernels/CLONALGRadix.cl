#define PATH_LAST_INDEX		PATH_LENGTH - 1
#define DISTANCE(d, i, j)	d[i * PATH_LENGTH + j]
#define DIST(i, j)			d[i * PATH_LENGTH + j]
// Starting population, selected population to clone Must be 2^n
#define BETA 				(POP_MAX - SELECTION_SIZE) / SELECTION_SIZE		

// Radix sort related
#define PRECISION			16384
#define PRECISION_BITCOUNT	20
#define SORT_MASK		3
#define SORT_BIT_COUNT	4
#define SORT_BIT_SHIFT_STEP	2

#define RANDA 				4294883355UL	// @See MWC Mark Gornesky and Andrew Klapper
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
		global uint* t1, 					// 4
		global uint* t2, 					// 5
		uint const popSize,					// 6
		global uint	*counts					// 7
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
	
	// Needed for Radix sort	
	int chunkSize = POP_MAX / globalSize;
	int start = globalId * chunkSize;
	int end = start + chunkSize;	
	local uint subTotal[SORT_BIT_COUNT];		
	int countOffset = SORT_BIT_COUNT * globalId;
	int countEnd = countOffset + SORT_BIT_COUNT;
	int countSize = globalSize * SORT_BIT_COUNT;
	
	uint patternCount[SORT_BIT_COUNT];
	
	for (int i = 0; i <= GENERATIONS && sane; i++) {
		///		Sort
		// Marshall
		for (int j = start; j < end; j++) {
			t1[j] = t2[j] = (0xFFF00000 & (j << PRECISION_BITCOUNT))
				   | (0x000FFFFF & convert_uint(paths[j].distance * PRECISION));
		}
		barrier(CLK_GLOBAL_MEM_FENCE);
		// Radix sort
		
		for (int i2 = 0; i2 < PRECISION_BITCOUNT; i2 += 2) {
			patternCount[0] = 0;
			patternCount[1] = 0;
			patternCount[2] = 0;
			patternCount[3] = 0;
			
			// Count how many of each bit we have in our part		
			for (int j = start; j < end; j++) {
				patternCount[(((t2[j] = t1[j]) >> i2) & SORT_MASK)]++;
			}		
			
			for (int j = 0; j < SORT_BIT_COUNT; j++) {
				counts[j + countOffset] = patternCount[j];
			}
			
			barrier(CLK_GLOBAL_MEM_FENCE);
			
			for (int j = localId; j < SORT_BIT_COUNT; j += localSize) {
				int sum = 0;
				for (int k = j; k < countSize; k += SORT_BIT_COUNT) {
					int tmp = counts[k];
					counts[k] = sum;
					sum += tmp;
				}
				subTotal[j] = sum;
			}	
			
			barrier(CLK_LOCAL_MEM_FENCE);
			
			patternCount[0] = counts[countOffset];
			patternCount[1] = subTotal[0] + counts[countOffset + 1];
			patternCount[2] = subTotal[0] + subTotal[1] + counts[countOffset + 2];
			patternCount[3] = subTotal[0] + subTotal[1] + subTotal[2] + counts[countOffset + 3];
			
			for (int j = start; j < end; j++) {
				uint num = t2[j];
				uint pattern = ((num >> i2) & SORT_MASK);
				uint address = patternCount[pattern]++;
				t1[address] = num;
			}
			
			barrier(CLK_GLOBAL_MEM_FENCE);
		}
		
		// Unmarschall
		for (uint j = start; j < end; j++) t1[j] = (0xFFF00000 & t1[j]) >> PRECISION_BITCOUNT;
		barrier(CLK_GLOBAL_MEM_FENCE);
		
		if (i < GENERATIONS) {
			///		Selection
			//		t1 contains addresses of sorted paths
			//		after index SELECTION_SIZE space will be reclaimed for clones
			//		so no explicit selection is needed.
			
			///		Clone (Use affinities and beta's)
			for (uint j = globalId; j < SELECTION_SIZE; j += globalSize) {
				// Only 1 worker has value j
				uint realIndex = t1[j];
				uchar cpy[PATH_LENGTH];
				for (int k = 0; k < PATH_LENGTH; k++) {
					cpy[k] = paths[realIndex].vertices[k];
					//cpy[k] = k;
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
						uchar tmp = clone[ri2];						
						clone[ri2] = clone[ri1]; 
						clone[ri1] = tmp;
					}
					
					uint cloneRealIndex = t1[SELECTION_SIZE + (BETA * j) + k];

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
		
		// Repeat with new memory
	}	
	
	if (globalId == 0) {
		for (int i = 0; i < PATH_LENGTH; i++) {
			output[i] = convert_int(paths[t1[0]].vertices[i]);
		}
	}
}
