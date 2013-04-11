package nl.ru.bio.algorithm.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * @author niklas
 *	The Travelling Salesperson (TSP) implementation of the abstract antibody class
 */
public class TSPAntibody extends Antibody {
	
	// random number generator used for all antibodies so that mutations are random across the whole population
	static Random rand = new Random((long) 42);
	
	/**
	 * Copy constructor
	 * @param original, the original to be copied
	 */
	public TSPAntibody(TSPAntibody original){
		super(original.data.clone());
		affinity = original.affinity;
	}
	
	public TSPAntibody(int[] data){
		super(data);
	}
	
	@Override
	public void hypermutate() {
		// mutation factor in relation to the affinity
		int mutationFactor = 10;
		// TODO: make mutation factor depend on affinity. idea: (int) Math.abs(Math.round(Math.pow(Math.E, affinity)));
		int nrOfMutations = minNrMutations*mutationFactor;
		for(int i = 0; i < nrOfMutations; i++){
			TSPAntibody.randomArrSwap(data);
		}
	}

	@Override
	public int[] getData() {
		return data;
	}

	@Override
	public ArrayList<Antibody> createClones(int i) {
		ArrayList<Antibody> out = new ArrayList<Antibody>(i);
		for (int h = 0; h < i; h++){
			out.add(new TSPAntibody(this));
		}
		return out;
	}

	@Override
	public ArrayList<Antibody> generateRandom(int i) {
		ArrayList<Antibody> out = createClones(i);
		for(int h = 0; h < i; h++){
			Antibody current = out.get(h);
			TSPAntibody.arrayShuffle((current.getData()));
		}
		return out;
	}
	
	/**
	 * Randomly shuffle the given array, performs 3*arr.length swaps of entries
	 * @param is
	 */
	private static void arrayShuffle(int[] is){
		int n = is.length*3;
		for(int shuffleRound = 0; shuffleRound <n; shuffleRound++){
			TSPAntibody.randomArrSwap(is);
		}
		return;
	}

	/**
	 * Randomly swap to entries of the given array
	 * @param data
	 */
	private static void randomArrSwap(int[] data) {
		int n = data.length;
		int i, j;
		i = rand.nextInt(n);
		j = rand.nextInt(n);
		int temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TSPAntibody [affinity=" + affinity + ", data="
				+ Arrays.toString(data) + "]";
	}

}
