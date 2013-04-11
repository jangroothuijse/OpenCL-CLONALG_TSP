package nl.ru.bio;

import java.util.ArrayList;

import nl.ru.bio.algorithm.java.Antibody;
import nl.ru.bio.algorithm.java.ClonalgOptimController;
import nl.ru.bio.algorithm.java.TSPAntibody;
import nl.ru.bio.algorithm.java.TSPGraphScoreFunction;
import nl.ru.bio.model.Graph;
import nl.ru.bio.model.GraphGenerator;

public class PureJavaTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int graphsize = 4;
		float[] graph = GraphGenerator.fullyConnectedSymmetric(graphsize);
		
		TSPGraphScoreFunction scorefunc = new TSPGraphScoreFunction(graph);
		int nSelection = graphsize;
		int beta = 3;
		int d = graphsize;
		
		int initialSize = graphsize*10;
		ArrayList<Antibody> initialAntibodiesList = new ArrayList<Antibody>(initialSize);
		for(int i = 0; i < initialSize; i++){
			initialAntibodiesList.add(new TSPAntibody(Graph.createRandomPath(graphsize)));
			//System.out.println(initialAntibodiesList.get(i).toString());
		}
		
		ClonalgOptimController optimController = new ClonalgOptimController(scorefunc, nSelection, beta, d, initialAntibodiesList);
		Antibody result = optimController.clonalgOptim();
		System.out.println(result.toString());
	}

}
