/**
 * 
 */
package nl.ru.bio.algorithm.java;

/**
 * @authors Jan Groothijse, Niklas Weber, Rob Tiemens
 * An instance of this class is used to compute the affinity (higher -> better) of the given antibody
 */
public abstract class ScoreFunction {

	public abstract float score(Antibody antibody);

}
