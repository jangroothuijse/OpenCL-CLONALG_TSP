package nl.ru.bio.algorithm;

/**
 *  @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public interface IAlgorithm {
	/**
	 * Calculates a path
	 * @param graphSize number of vertices in graph
	 * @param graph must be float[graphSize^2]
	 * @return path consisting of vertices
	 */
	public int[] findSolution(int graphSize, float[] graph);
}
