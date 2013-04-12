/**
 * 
 */
package nl.ru.bio.algorithm.java;

import nl.ru.bio.model.Graph;

/**
 * @authors Jan Groothijse, Niklas Weber, Rob Tiemens
 * Given a graph, this score function gives affinities to antibodies
 */
public class TSPGraphScoreFunction extends ScoreFunction {
	
	private float[] graph;

	/**
	 * @param graph
	 */
	public TSPGraphScoreFunction(float[] graph) {
		this.graph = graph;
	}

	/* (non-Javadoc)
	 * @see nl.ru.bio.model.ScoreFunction#score(nl.ru.bio.model.Antibody)
	 */
	@Override
	public float score(Antibody antibody) {
		int[] path = antibody.getData();
		// score is the inverse of the path length
		// this might be revisited. it could be beneficial to only allow positive affinities
		// TODO check whether there is a good way to make this positive
		return -Graph.distance(path.length, graph, path);
	}

}
