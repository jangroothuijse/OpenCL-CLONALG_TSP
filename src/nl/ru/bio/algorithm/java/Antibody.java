package nl.ru.bio.algorithm.java;

import java.util.ArrayList;
/**
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public abstract class Antibody implements Comparable<Antibody>{
	
	public float affinity = Float.NEGATIVE_INFINITY;
	public int[] data;
	
	static final int minNrMutations = 1;
	
	public abstract void hypermutate();
	public abstract int[] getData();
	public abstract ArrayList<Antibody> createClones(int i);
	// this one should be static, but we cannot override static methods by inheritance
	// so this is my ugly workaround
	public abstract ArrayList<Antibody> generateRandom(int i);
	
	/**
	 * Constructor
	 * @param data
	 */
	public Antibody(int[] data) {
		this.data = data;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Antibody o) {
		return sgn(this.affinity - o.affinity);
	}

	/** the signum function
	 * @param f
	 * @return
	 */
	private int sgn(float f) {
		if(f < 0){
			return -1;
		}else if(f > 0){
			return 1;
		}else{
			return 0;
		}
	}
}
