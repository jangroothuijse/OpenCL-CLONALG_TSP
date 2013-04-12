package nl.ru.bio.model;

/**
 * Utility class to generate graphs, @see Graph * 
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public class GraphGenerator {
	
	private GraphGenerator() {}
	
	/**
	 * Given a number of vertices, generates a fully connected graph
	 * which is returned as an array of float, length of the array is size^2
	 * @param graphSize
	 * @return the graph
	 */
	public static float[] fullyConnectedSymmetric(int graphSize) {
		float[] result = new float[graphSize * graphSize];
		
		for (int i = 0; i < graphSize; i++) {
			for (int j = 0; j < i; j++) {
				result[i * graphSize + j] = result[j * graphSize + i] = (float) Math.random();				          
			}
		}
		
		return result;
	}

	/**
	 * Test this class, test generation of graphs.
	 * @param args
	 */
	public static void main(String[] args) {
		int size = 16;
		System.out.println("fullyConnectedSymmetric:");
		float[] result = fullyConnectedSymmetric(size);
		Graph.printGraph(size, result);
	}

}
