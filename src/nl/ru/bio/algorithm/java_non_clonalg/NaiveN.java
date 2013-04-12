/**
 * 
 */
package nl.ru.bio.algorithm.java_non_clonalg;

import nl.ru.bio.algorithm.IAlgorithm;

/**
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public class NaiveN implements IAlgorithm {

	/** (non-Javadoc)
	 * @see nl.ru.bio.algorithm.IAlgorithm#findSolution(int, float[])
	 * Trivial implementation
	 */
	@Override
	public int[] findSolution(int graphSize, float[] graph) {
		int[] result = new int[graphSize];
		for (int i = 0; i < graphSize; i++) result[i] = i;
		return result;
	}

}
