package nl.ru.bio.algorithm.java_non_clonalg;

import nl.ru.bio.algorithm.IAlgorithm;

/**
 * @author jan
 * Standard greedy algororithm, implemented using bitwise operators which limits
 * the graphsize to 64.
 */
public class GreedyNSquared implements IAlgorithm {

	/**
	 * @see nl.ru.bio.algorithm.IAlgorithm#findSolution(int, float[])
	 * @param graphSize must not be higher than 64
	 * @param graph must be float[graphSize^2]
	 * @return a path consisting of vertices
	 */
	@Override
	public int[] findSolution(int graphSize, float[] graph) {
		return findSolution(graphSize, graph, 0);
	}
	
	protected final int[] findSolution(int graphSize, float[] graph, int start) {
		int[] result = new int[graphSize];
		long current = start;
		long next = 0;
		long visited = (long)1 << start;
		result[0] = start;
		for (long i = 1; i < graphSize; i++) {
			float distance = Float.MAX_VALUE;
			for (long j = 0; j < graphSize; j++) {
				if ((((long)1 << j) & visited) == 0 &&
					graph[(int)current * graphSize + (int)j] < distance) {
					next = j;
					distance = graph[(int)current * graphSize + (int)j];
				}				
			}
			current = next;
			result[(int)i] = (int)current;
			visited |= ((long)1) << current;			
		}
		return result;
	}

	public static void main(String[] args) {
		int graphSize = 16;
		float[] graph = nl.ru.bio.model.GraphGenerator.fullyConnectedSymmetric(graphSize);
		GreedyNSquared alg = new GreedyNSquared();
		int[] path = alg.findSolution(graphSize, graph);
		System.out.println("path validness: " + (nl.ru.bio.model.Graph.pathIsValid(graphSize, path)));
		for (int i : path) System.out.print(i + "\t");
		System.out.println();
		System.out.println();
		nl.ru.bio.model.Graph.printGraph(graphSize, graph);
	}
	
}
