package nl.ru.bio.algorithm.java_non_clonalg;

import nl.ru.bio.algorithm.IAlgorithm;
import nl.ru.bio.model.Graph;
/**
 * Code explains itself
 * @authors Jan Groothijse, Niklas Weber, Rob Tiemens
 * 64 vertices max
 */
public class HeuristicNCubed implements IAlgorithm {

	/**
	 * Calculates a path
	 * @param graphSize number of vertices in graph must be <= 64
	 * @param graph must be float[graphSize^2]
	 * @return path consisting of vertices
	 */
	@Override
	public int[] findSolution(int graphSize, float[] graph) {
		int[] result = makeCycles(graphSize, graph);
		int[] endResult = new int[graphSize];
		long pointer = 0;
		endResult[0] = 0;
		long visited = 1;
		for (long i = 1; i < graphSize; i++) {			
			pointer = result[(int)pointer];
			if ((((long)1 << pointer) & visited) == ((long)1 << pointer)) {
				// Cycle detected, now we jump to the closest 
				// other cycle that has not been visited				
				float distance = Float.MAX_VALUE;
				int closest = 0;
				for (long j = 0; j < graphSize; j++) {					
					if ((((long)1 << j) & visited) != ((long)1 << j) && 
							graph[(int)pointer * graphSize + (int)j] < distance) {
						closest = (int)j;
					}
				}
				pointer = closest;
			}
			visited |= (long)1 << pointer;
			endResult[(int)i] = (int)pointer;
		}
		
		return endResult;
	}
	/*
	 * N^4 to connect cycles sort of optimal.
	 
	private int[] connectCycles(final int graphSize, final float[] graph, int[] cycles) {
		long visited = 0;
		int[] path = new int[graphSize];
		
		int next = 0;
		// N
		for (int i = 0; i < graphSize; i++) {
			visited |= (long)1 << (long)cycles[next];
			path[i] = next;
			next = cycles[next]; // next goes to the next
			if ((((long)1 << (long)cycles[next]) & visited)
					== ((long)1 << (long)cycles[next])){
				// path is now a cycle
				// we need to find where to split it
				int endNode1 = 0, endNode2 = 0, endTarget1 = 0, endTarget2 = 0;
				float totalDistance = Float.MAX_VALUE;
				// we need jump somewhere else				
				// loop through current 
				for (int j = 0; j < i; j++) {
					int node1 = path[i], node2 = path[i+1], target1 = 0, target2 = 0;
					float distance1 = Float.MAX_VALUE, distance2 = Float.MAX_VALUE;					
					for (int k = 0; k < graphSize; k++) {
						if ((((long)1 << (long)k) & visited) == ((long)1 << (long)k)) {
							target1 = cycles[k];
							target2 = cycles[cycles[k]];
							// only nodes we have not visited
							if (graph[(int)node1 * graphSize + (int)k] < distance1) {
								node
							}
						}
					}
					
					if (distance1 + distance2 < totalDistance) {
						totalDistance = distance1 + distance2;
						endNode1 = node1;
						endNode2 = node2;
					}
				}
			}
		}
		
		return cycles;
	}
	*/
	private int[] makeCycles(final int graphSize, final float[] graph) {		
		float[] localGraph = graph.clone();
		int[] result = new int[graphSize];
		long usedFrom = 0, usedTo = 0;
		// k = N
		for (int k = 0; k < graphSize; k++) {		
			float smallestDistance = Float.MAX_VALUE;
			long smallestFrom = Integer.MAX_VALUE;
			long smallestTo = Integer.MAX_VALUE;
			// i * k = N^2
			for (long i = 0; i < graphSize; i++) {
				if ((((long)1 << i) & usedFrom) == 0) {
					// j * i * k = N^3
					for (long j = 0; j < graphSize; j++) {
						if (i != j && (((long)1 << j) & usedTo) == 0
								&& localGraph[(int)i * graphSize + (int)j] >= 0
								&& localGraph[(int)i * graphSize + (int)j] < smallestDistance) {
							smallestDistance = graph[(int)i * graphSize + (int)j];
							smallestFrom = i;
							smallestTo = j;						
						}
					}
				}
			}
			// Last one can be hard to find (has to return directly or be connected to self)
			if (smallestFrom == Integer.MAX_VALUE) {
				for (long i = 0; i < graphSize; i++) {
					if ((((long)1 << i) & usedFrom) == 0) {
						// j * i * k = N^3
						for (long j = 0; j < graphSize; j++) {
							if ((((long)1 << j) & usedTo) == 0
									&& graph[(int)i * graphSize + (int)j] >= 0
									&& graph[(int)i * graphSize + (int)j] < smallestDistance) {
								smallestDistance = graph[(int)i * graphSize + (int)j];
								smallestFrom = i;
								smallestTo = j;						
							}
						}
					}
				}
			}		
			
			result[(int)smallestFrom] = (int)smallestTo;
			
			// Make sure there are no reverse edges:
			localGraph[(int)smallestFrom * graphSize + (int)smallestTo] = -1;
			localGraph[(int)smallestTo * graphSize + (int)smallestFrom] = -1;
			
			// Make sure every vertex has only one incoming edge
			usedFrom |= (long)1 << smallestFrom;
			// Make sure every vertex has only one outgoing edge
			usedTo |= (long)1 << smallestTo;
		}
		return result;
	}
	
	public static void main(String[] args) {
		int graphSize = 64;
		float[] graph = nl.ru.bio.model.GraphGenerator.fullyConnectedSymmetric(graphSize);
		HeuristicNCubed h1 = new HeuristicNCubed();
		int[] path = h1.findSolution(graphSize, graph);
		System.out.println("Found path:");
		for (int i : path) System.out.print(i + "\t");
		
		System.out.println();
		System.out.println("path validness: " + (nl.ru.bio.model.Graph.pathIsValid(graphSize, path)));
		System.out.println("Graph:");
		Graph.printGraph(graphSize, graph);
		int[] naivePath = new int[graphSize];
		for (int i = 0; i < graphSize; i++) naivePath[i] = i;
		System.out.println("NaivePath: " + Graph.distance(graphSize, graph, naivePath));
		System.out.println("HeuristicNCubedPath: " + Graph.distance(graphSize, graph, path));
	}
}
