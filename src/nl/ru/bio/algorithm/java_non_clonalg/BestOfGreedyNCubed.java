/**
 * 
 */
package nl.ru.bio.algorithm.java_non_clonalg;

/**
 * @authors Jan Groothijse, Niklas Weber, Rob Tiemens
 */
public class BestOfGreedyNCubed extends GreedyNSquared {
	/**
	 * @see nl.ru.bio.algorithm.IAlgorithm#findSolution(int, float[])
	 * @param graphSize must not be higher than 64
	 * @param graph must be float[graphSize^2]
	 * @return a path consisting of vertices
	 */
	@Override
	public int[] findSolution(int graphSize, float[] graph) {
		int[] bestPath = new int[graphSize];
		
		float distance = Float.MAX_VALUE;
		for (int i = 0; i < graphSize; i++) {
			int[] path = findSolution(graphSize, graph, i);
			float localDistance = nl.ru.bio.model.Graph.distance(graphSize, graph, path);
			if (localDistance <= distance) {
				distance = localDistance;
				bestPath = path;
			}
		}
		
		return bestPath; 
	}
}
