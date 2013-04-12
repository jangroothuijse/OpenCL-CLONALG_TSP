package nl.ru.bio.algorithm.java;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public class ClonalgOptimController {

	// the current set of antibodies (after initialization)
	NavigableSet<Antibody> antibodies;
	// a function to give affinity scores to antibodies
	ScoreFunction scorefunc;
	// the current generation
	private int generation = 0;
	// how many antibodies (clones) are selected to be cloned (create the new antibody set)
	private int nSelection;
	// controls how many clones are created per selected antibody
	private float beta;
	// how many random antibodies are created, which then replace the worst antibodies
	private int d;
	// we start with these antibodies
	private ArrayList<Antibody> initialAntibodiesList;

	/**
	 * Calculate the affinities for the given antibodies
	 * 
	 * @param antibodies2
	 * @return void
	 */
	private void calcAffinities(ArrayList<Antibody> antibodies2) {
		int n = antibodies2.size();
		for (int i = 0; i < n; i++) {
			antibodies2.get(i).affinity = this.scorefunc.score(antibodies2.get(i));
		}
		return;
	}

	


	/**
	 * @param scorefunc gives scores per antibody
	 * @param nSelection select this many antibodies/clones
	 * @param beta influence how many clones are created
	 * @param d create this many random antibodies
	 * @param initialAntibodiesList start with these antibodies
	 */
	public ClonalgOptimController(ScoreFunction scorefunc, int nSelection,
			float beta, int d, ArrayList<Antibody> initialAntibodiesList) {
		this.scorefunc = scorefunc;
		this.nSelection = nSelection;
		this.beta = beta;
		this.d = d;
		this.initialAntibodiesList = initialAntibodiesList;
	}




	/**
	 * runs the optimization variant of CLONALG implemented according to: de
	 * Castro, Leandro N. and Von Zuben, Fernando J., Learning and Optimization
	 * Using the Clonal Selection Principle IEEE Transactions on Evolutionary
	 * Computation, Special Issue on Artificial Immune Systems, vol. 6, n. 3,
	 * pp. 239-251, 2002 'step x' in the comments refers a) the description
	 * (chapter V.A and V.B) and b) the pseudocode in Appendix II
	 * 
	 * @return
	 */
	public Antibody clonalgOptim() {
		// calculate affinities/score fore all initial antibodies
		calcAffinities(initialAntibodiesList);
		this.antibodies = new TreeSet<Antibody>(initialAntibodiesList);
		initialAntibodiesList = null;
		while (!stopCondition()) {
			// increase generation
			this.generation++;
			// select the nSelection best antibodies, step 3
			//System.out.println("Selecting antibodies");
			NavigableSet<Antibody> selectedAntibodies= select(this.antibodies, nSelection);
			// generate clones for every of those selected antibodies, step 4
			ArrayList<Antibody> clonesList = clonalgClone(selectedAntibodies, beta, false);
			// hypermutate the clones, step 5
			hypermutate(clonesList);
			// find clone affinities, step 6
			calcAffinities(clonesList);
			NavigableSet<Antibody> clones = new TreeSet<Antibody>(clonesList);
			// select nSelection best clones, step 7
			//System.out.println("Selecting clones");
			NavigableSet<Antibody> selectedClones = select(clones, nSelection);
			// save current best solution
			Antibody savedBestSolution = getBestSolution(this.antibodies);
			// replace set of antibodies with clones
			this.antibodies = selectedClones;
			// insert saved best solution into antibody set. this implements
			// the hint given in section V.B, page 10
			this.antibodies.add(savedBestSolution);
			// randomly generate d antibodies
			ArrayList<Antibody> randomAntibodiesList = generateRandomAntibodies(d);
			// compute random antibody affinities
			calcAffinities(randomAntibodiesList);
			NavigableSet<Antibody> randomAntibodies = new TreeSet<Antibody>(randomAntibodiesList);
			// replace d lowest affinity antibodies with random antibodies, step 8
			replace(this.antibodies, randomAntibodies);
			
			if(this.generation < 10)
				System.out.printf("Generation %d\tBest Solution %s\n", this.generation, getBestSolution(this.antibodies).toString());
		}
		return getBestSolution(this.antibodies);
	}

	

	/**
	 * Return best solution for the current set of antibodies
	 * @param antibodies2
	 * @return
	 */
	private Antibody getBestSolution(NavigableSet<Antibody> antibodies2) {
		return antibodies2.last();
	}



	/**
	 * Replace the worst (lowest affinity) antibodies with random antibodies
	 * @param antibodies2
	 * @param randomAntibodies
	 */
	private void replace(NavigableSet<Antibody> antibodies2,
			NavigableSet<Antibody> randomAntibodies) {
		//int d = randomAntibodies.size();
		//assert d <= antibodies2.size();
		// remove the d worst antibodies
		for(int i = 0; i < d; i++){
			try{
				antibodies2.pollFirst();
			}catch(NoSuchElementException e){
				System.err.printf("Requested removal of %d elements, but only %d are present.\n", d, antibodies2.size());
				//e.getMessage();
				break;
			}
			
			
			// TODO raise exception if there are no more elements
		}
		// add all random antibodies
		antibodies2.addAll(randomAntibodies);
	}



	/**
	 * Generate d2 random antibodies
	 * @param d2
	 * @return
	 */
	private ArrayList<Antibody> generateRandomAntibodies(int d2) {
		return this.antibodies.first().generateRandom(d2);
	}


	/** Clone selectedantibodies, how much clones are produced depends on beta2. If considerAffinity = true, less clones
	 * are produced for lower-affinity antibodies
	 * @param selectedAntibodies
	 * @param beta2
	 * @param considerAffinity
	 * @return
	 */
	private ArrayList<Antibody> clonalgClone(
			NavigableSet<Antibody> selectedAntibodies, float beta2, boolean considerAffinity) {
		int n = selectedAntibodies.size();
		Iterator<Antibody> sortedAntibodiesIterator = selectedAntibodies.descendingIterator();
		ArrayList<Antibody> out = new ArrayList<Antibody>(n*(int)Math.round(beta2));
		int nrOfClones;
		for(int i = 0; i < n; i++){
			if (considerAffinity){
				nrOfClones = (int) Math.round((float) n* beta2 / (i+1));
			}else{
				nrOfClones = (int) Math.round((float) n * beta2);
			}
			ArrayList<Antibody> cloneList = sortedAntibodiesIterator.next().createClones(nrOfClones);
			out.addAll(cloneList);
		}
		return out;
	}



	/**
	 * Hypermutate the given clones, depending on their affinities
	 * 
	 * @param clones
	 * @return
	 */
	private void hypermutate(ArrayList<Antibody> clones) {
		int n = clones.size();
		for (int i = 0; i < n; i++) {
			clones.get(i).hypermutate();
		}
		return;
	}

	/**
	 * Select the nSelection best antibodies
	 * @param antibodies2
	 * @param affinities
	 * @param nSelection2
	 * @return
	 */
	private NavigableSet<Antibody> select(NavigableSet<Antibody> antibodies2, int nSelection2) {
		//System.out.printf("nr antibodies: %d\tnr requested: %d\n" , antibodies2.size(), nSelection2);
//		assert nSelection2 <= antibodies2.size();
		NavigableSet<Antibody> out = new TreeSet<Antibody>();
		Iterator<Antibody> iter = antibodies2.descendingIterator();
		for(int i = 0; i < nSelection2; i++){
			try{
				out.add(iter.next());
			}catch(NoSuchElementException e){
				System.err.printf("Exception: requested %d elements for selection, but only %d are present. returned the lower number of elements\n", nSelection2, antibodies2.size());
				//System.err.println(e.getMessage());
				break;
			}
		}
		return out;
	}

	/**
	 * Test whether we should stop looping
	 * 
	 * @return true, if looping should stop
	 */
	private boolean stopCondition() {
		return this.generation > 512;
	}

}
