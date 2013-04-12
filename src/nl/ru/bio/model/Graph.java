package nl.ru.bio.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Utility class for graphs.
 * 
 * Graphs are represented by an array of floating points,
 * graphs are accompanied by their size in vertices, which is 
 * the square root of the length of the array. 
 * 
 * Graphs are 1D while representing 2D data:
 * 
 * 2D:
 * 1,2
 * 3,4
 * 
 * 1D:
 * 1,2,3,4
 * 
 * 2D[i][j] = 1D[i * sqrt(1D.length) + j]
 * 
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public final class Graph {
	
	private static Random rand = new Random(23);
	
	private Graph() {}
	/**
	 * Distance of a path (cycle really)
	 * @param graphSize number of vertices in graph
	 * @param graph must be float[graphSize^2]
	 * @param path must be int[graphSize]
	 * @return distance of the path
	 */
	public static float distance(int graphSize, float[] graph, int[] path) {
		// path maps an sequential index to a vertex number
		// Start of with the length from the last to the first
		float acc = graph[path[graphSize - 1] * graphSize + path[0]];
		// Iterate through all end points (i is the end point)
		for (int i = 1; i < graphSize; i++) {
			// i - 1 is the begin point
			acc += graph[path[i - 1] * graphSize + path[i]];
		}		
		return acc;
	}
	
	/**
	 * Distance between points
	 * @param graphSize number of vertices in graph
	 * @param graph must be float[graphSize^2]
	 * @param from must be 0 < from < graphSize
	 * @param to must be 0 < to < graphSize
	 * @return distance from from to to in graph graph
	 */
	public static float distance(int graphSize, float[] graph, int from, int to) {
		// 2D[i][j] = 1D[i * sqrt(1D.length) + j]
		return graph[from * graphSize + to];
	}
	
	/**
	 * Pretty prints a graph.
	 * @param graphSize
	 * @param graph must be float[size^2]
	 */
	public static void printGraph(int graphSize, float[] graph) {
		// Top row (Header)
		System.out.print("\t");
		for (int i = 0; i < graphSize; i++) {
			// Loop to print column headers
			System.out.print(i + "\t");
		}		
		// Rest of the rows
		System.out.println();
		for (int i = 0; i < graphSize; i++) {
			System.out.print(i + "\t"); // <- row header
			for (int j = 0; j < graphSize; j++) {
				// Actual cell with information
				System.out.print(String.format("%.2f\t", graph[i * graphSize + j]));
			}
			System.out.println(); // Cannot be omitted because we want to end with \n
		}		
	}
	
	/**
	 * Validates a path of a graph: path length must match
	 * no elements must be double in it, all elements must be >= 0
	 * @param graphSize size of the graph (in vertices)
	 * @param path (vertices)
	 * @return validness
	 */
	public static boolean pathIsValid(int graphSize, int[] path) {
		if (path.length != graphSize) return false;
		java.util.BitSet visited = new java.util.BitSet();
		for (int vertex : path) {
			if (vertex < 0) {
				return false;
			}
			if (!visited.get(vertex)) {
				visited.set(vertex);
			} else return false;
		}
		return true;
	}
	
	/**
	 * Validates a path of a graph: path length must match
	 * no elements must be double in it, all elements must be >= 0
	 * @param graphSize size of the graph (in vertices)
	 * @param path (vertices)
	 * @return validness
	 */
	public static void printError(int graphSize, int[] path) {
		if (path.length != graphSize) 
			System.out.println("size does not match, graphSize:" + graphSize +
			", path.lenght: " + path.length);
		java.util.BitSet visited = new java.util.BitSet();
		for (int vertex : path) {
			if (vertex < 0) System.out.println("vertex has value < 0:" + vertex);
			if (!visited.get(vertex)) {
				visited.set(vertex);
			} else System.out.println("Vertex found more than once:" + vertex);
		}
	}
	
	public static void printError(int graphSize, char[] path) {
		int[] path2 = new int[graphSize];
		for (int i = 0; i < graphSize; i++) path2[i] = path[i];
		printError(graphSize, path2);
	}
	
	/**
	 * Creates a random path
	 * @param graphsize how long a path should be
	 * @return
	 */
	public static int[] createRandomPath(int graphsize){
		int[] rPath = new int[graphsize];
		int[] tmp = new int[graphsize];
		for(int i = 0; i < graphsize; i++){
			tmp[i] = i;
		}
		int pickIndex = rand.nextInt(graphsize);;
		for(int i = 0; i < graphsize; i++){
			while(tmp[pickIndex] == -1){
				pickIndex = rand.nextInt(graphsize);				
			}
			rPath[i] = tmp[pickIndex];
			tmp[pickIndex] = -1;
		}
		return rPath;
	}
	
	public static void main(String[] args) {
		
		System.out.println("Testing symmetrical properties");
		int size = 12;
		float[] testGraph = GraphGenerator.fullyConnectedSymmetric(size);
		System.out.println("Next 2 lines should be equal (and this should be an asertion for unit testing..)");
		System.out.println(distance(size, testGraph, 4, 8));
		System.out.println(distance(size, testGraph, 8, 4));
		
		int[] path1 = new int[size];
		int[] path2 = new int[size];
		for (int i = 0; i < size; i++) {
			path1[i] = i;
			path2[i] = (size - 1) - i;
		}
		
		System.out.println("path1 validness: " + (pathIsValid(size, path1)));
		System.out.println("path2 validness: " + (pathIsValid(size, path2)));
		
		System.out.println("Next 2 lines should be almost equal too (and this should also be an asertion for unit testing..)");
		System.out.println(distance(size, testGraph, path1));
		System.out.println(distance(size, testGraph, path2));
		System.out.println("Used graph:");
		printGraph(size, testGraph);
	}
}
