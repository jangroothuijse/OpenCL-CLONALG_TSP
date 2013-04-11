package nl.ru.bio.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.Console;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import nl.ru.bio.algorithm.java.Antibody;
import nl.ru.bio.algorithm.java.ClonalgOptimController;
import nl.ru.bio.algorithm.java.TSPAntibody;
import nl.ru.bio.algorithm.java.TSPGraphScoreFunction;
import nl.ru.bio.algorithm.java_non_clonalg.BestOfGreedyNCubed;
import nl.ru.bio.algorithm.java_non_clonalg.GreedyNSquared;
import nl.ru.bio.algorithm.java_non_clonalg.HeuristicNCubed;
import nl.ru.bio.algorithm.java_non_clonalg.NaiveN;
import nl.ru.bio.algorithm.opencl.CLONALGWithMergeSort;
import nl.ru.bio.algorithm.opencl.CLONALGWithMergeSortCPU;
import nl.ru.bio.algorithm.opencl.CLONALGWithRadixSort;
import nl.ru.bio.algorithm.opencl.CLONALGWithRadixSortCPU;
import nl.ru.bio.helper.Coordinate;
import nl.ru.bio.model.Graph;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;
import org.jgrapht.alg.HamiltonianCycle;



public class gui extends JPanel implements ActionListener {
    JButton startButton;
    JButton randomButton;
    JComboBox<String> algoList;
    JComboBox<String> threadList;
    
    static JLabel distance;
    static JLabel elapsed;
    
    
    private static String[] algorithms = {"Naive N", "Heuristic N Cubed", "Greedy N Squared", "Best Of Greedy N Cubed", "ClonAlg CPU", "ClonAlg GPU (Merge sort)", "ClonAlg GPU (Radix sort)", "ClonAlg OpenCL CPU (Merge sort)", "ClonAlg OpenCL CPU (Radix sort)" };
    private static String[] nrThreads = {"1", "16", "32", "64", "128", "256", "512"};
    
    
    private static final long serialVersionUID = 4958701194316765330L;
	private static JGraph jgraph;
	private static ListenableUndirectedWeightedGraph<Long, DefaultWeightedEdge> g;
	private static JGraphModelAdapter<Long, DefaultWeightedEdge> adapter;
	private static int nrOfCities = 51;
	private static int nrRandomCities = 20; 
	private static int spread = 13;
	private static float[] matrix;
	private static Coordinate[] cities;
	
	private static final int POP_MAX = 2048;
	private static final int SELECTION_SIZE = 512;
	private static final int GENERATIONS = 256;
	private static final int MUTATIONS = 8;
	
	
	public gui() {
	        super(new BorderLayout());
	        
			//	Creating the panel at bottom and adding components
			JPanel panel 	= new JPanel(); // the panel is not visible in output
			JLabel label 	= new JLabel("Algorithm");
			JLabel dist 	= new JLabel("Distance:");
			JLabel threads 	= new JLabel("Nr of threads");
			JLabel time 	= new JLabel("Time:");
			
			algoList	= new JComboBox<String>(algorithms);
			threadList 	= new JComboBox<String>(nrThreads);
			
			startButton 	= new 	JButton("start");
			randomButton 	= new  JButton("random");
			
			distance = new JLabel("-");
			elapsed = new JLabel("-");
			
			panel.add(label);
			panel.add(algoList);
			panel.add(threads);
			panel.add(threadList);
		
			
			panel.add(startButton);
			
			panel.add(dist);
			panel.add(distance);
			panel.add(time);
			panel.add(elapsed);
			
			//panel.add(randomButton);
		
			//	Adding Components to the frame.
			add(BorderLayout.SOUTH,panel);
			add(BorderLayout.CENTER,jgraph);
			
			//add event handler
			startButton.addActionListener(this);
			randomButton.addActionListener(this);
	    }
	 
	public void actionPerformed(ActionEvent e) {
		long start;
		long end;
		
		int nrThreads = Integer.parseInt((String) threadList.getSelectedItem());
	    	if(e.getActionCommand() == "start")
	    	{
	    		String value = (String) algoList.getSelectedItem();
	    		
	    		switch(value) 
	    		{
	    			case "Naive N":
	    				start = java.util.Calendar.getInstance().getTimeInMillis();
	    				update(new NaiveN().findSolution(nrOfCities, matrix));
	    				end = java.util.Calendar.getInstance().getTimeInMillis();
	    				
	    				elapsed.setText(Long.toString(end - start));
	    				break;
	    			case "Heuristic N Cubed":
	    				start = java.util.Calendar.getInstance().getTimeInMillis();
	    				update(new HeuristicNCubed().findSolution(nrOfCities, matrix));
    					end = java.util.Calendar.getInstance().getTimeInMillis();
	    				
	    				elapsed.setText(Long.toString(end - start));
	    				break;
					case "Greedy N Squared":
						start = java.util.Calendar.getInstance().getTimeInMillis();
						update(new GreedyNSquared().findSolution(nrOfCities, matrix));
						end = java.util.Calendar.getInstance().getTimeInMillis();
	    				
	    				elapsed.setText(Long.toString(end - start));
	    				break;
					case "Best Of Greedy N Cubed":
						start = java.util.Calendar.getInstance().getTimeInMillis();
						update(new BestOfGreedyNCubed().findSolution(nrOfCities, matrix));
						end = java.util.Calendar.getInstance().getTimeInMillis();
						
	    				elapsed.setText(Long.toString(end - start));
						break;
	    			case "ClonAlg CPU":
	    				//Niklas code
	    				TSPGraphScoreFunction scorefunc = new TSPGraphScoreFunction(matrix);
	    				int nSelection = nrOfCities;
	    				int beta = 3;
	    				int d = nrOfCities;
	    				
	    				int initialSize = nrOfCities;
	    				ArrayList<Antibody> initialAntibodiesList = new ArrayList<Antibody>(initialSize+9);
	    				
	    				initialAntibodiesList.add(new TSPAntibody(new HeuristicNCubed().findSolution(nrOfCities, matrix)));
	    				initialAntibodiesList.add(new TSPAntibody(new GreedyNSquared().findSolution(nrOfCities, matrix)));
	    				initialAntibodiesList.add(new TSPAntibody(new BestOfGreedyNCubed().findSolution(nrOfCities, matrix)));
	    				
	    				
	    				//initialAntibodiesList.add(new TSPAntibody(new BestOfGreedyNCubed().findSolution(nrOfCities, matrix)));
	    				//initialAntibodiesList.add(new TSPAntibody(new BestOfGreedyNCubed().findSolution(nrOfCities, matrix)));
	    				
	    				for(int i = 0; i < initialSize; i++){
	    					initialAntibodiesList.add(new TSPAntibody(Graph.createRandomPath(nrOfCities)));
	    				}
	    				
	    				
	    				
	    				
	    				
	    				
	    				
	    				ClonalgOptimController optimController = new ClonalgOptimController(scorefunc, nSelection, beta, d, initialAntibodiesList);
	    				//optimController.run();
	    				start = java.util.Calendar.getInstance().getTimeInMillis();
	    				
	    				Antibody result = optimController.clonalgOptim();
	    				int[] route = result.getData();
	    				
    					end = java.util.Calendar.getInstance().getTimeInMillis();
						
	    				elapsed.setText(Long.toString(end - start));
	    				
	    				update(route);
	    				
	    				break;
	    			case "ClonAlg GPU (Merge sort)":
	    				
	    				
	    				
	    				CLONALGWithMergeSort alg = new CLONALGWithMergeSort(nrOfCities, nrThreads, matrix, POP_MAX, SELECTION_SIZE, GENERATIONS, MUTATIONS);
	    				elapsed.setText(Integer.toString(alg.time));
	    				update(alg.output);
	    		 		//System.out.println(algoList.getSelectedItem());
	    				break;
	    			
	    			case "ClonAlg GPU (Radix sort)":
	    				
	    				
	    				
	    				CLONALGWithRadixSort algR = new CLONALGWithRadixSort(nrOfCities, nrThreads, matrix, POP_MAX, SELECTION_SIZE, GENERATIONS, MUTATIONS);
	    				elapsed.setText(Integer.toString(algR.time));
	    				update(algR.output);
	    		 		//System.out.println(algoList.getSelectedItem());
	    				break;
	    				
	    			case "ClonAlg CPU OpenCL (Merge sort)":
	    				
	    				
	    				
	    				CLONALGWithMergeSort algMC = new CLONALGWithMergeSortCPU(nrOfCities, nrThreads, matrix, POP_MAX, SELECTION_SIZE, GENERATIONS, MUTATIONS);
	    				elapsed.setText(Integer.toString(algMC.time));
	    				update(algMC.output);
	    		 		//System.out.println(algoList.getSelectedItem());
	    				break;
	    			
	    			case "ClonAlg CPU OpenCL (Radix sort)":
	    				
	    				
	    				
	    				CLONALGWithRadixSort algRC = new CLONALGWithRadixSortCPU(nrOfCities, nrThreads, matrix, POP_MAX, SELECTION_SIZE, GENERATIONS, MUTATIONS);
	    				elapsed.setText(Integer.toString(algRC.time));
	    				update(algRC.output);
	    		 		//System.out.println(algoList.getSelectedItem());
	    				break;
	    				
	    				
	    		 	default:
	    		 		
	    		 		break;
	    		}
	    		
	    	}
	    	else 
	    	{
	
	    		generateRandomGraph();
	    		
	    	}
	    	
	    }
	    
	public void update(int[] route) {
		int length = route.length;
		float dist = 0;
		
		//remove all edge (very naive);
		for (long i = 0; i< length; i++)
		{
			for (long j = 0; j< length; j++)
			{
				g.removeAllEdges(i,j);	
				
			}
			
		}
			
		//Draw cycle
		for (int i = 0; i< length-1; i++){
			g.addEdge( (long)route[i], (long)route[i+1] );
			dist += matrix[route[i]*nrOfCities + route[i+1]];	
		}
			
		//Complete cycle
		g.addEdge( (long)route[length-1], (long)route[0] );
		dist += matrix[route[length-1]*nrOfCities + route[0]];
		
		//update label.
		distance.setText(Float.toString(dist));
		
	}
	
	public static void generateRandomGraph() {
		// remove all old cities

		
		cities = new Coordinate[nrRandomCities];
		Random randomGenerator = new Random();
		 
		for(int i =0;i< nrRandomCities; i++)
			cities[i] = new Coordinate(randomGenerator.nextInt(50), randomGenerator.nextInt(50));
		
		matrix = new float[nrRandomCities * nrRandomCities];
    	
    	for(int i = 0; i <  nrRandomCities; i++)
    	{
    		for(int j = i; j <  nrRandomCities; j++)
	    	{
    			
    			if( i == j)
    				matrix[(i * nrRandomCities)+j] = 0;
    			else
    			{
    				float distance = (float) Math.sqrt(Math.pow(cities[i].x * spread - cities[j].x * spread,2) +  Math.pow(cities[i].y * spread - cities[j].y * spread,2));
    				matrix[(i * nrRandomCities)+j] = matrix[(j * nrRandomCities)+i] = distance;
    				
    				if (distance <= 0)
    					System.out.printf("Matrix entry %d %d has distance 0 \n", i,j);
    				
				
    			}
	    	}
    	}
    	nrOfCities = nrRandomCities;
    	
    	
    	createGraph();
	}
	
	public static void generateGraph() {
	    	/* Eil51 */
	    	
	    	cities = new Coordinate[nrOfCities];
	    	cities[0] = new Coordinate(37, 52);
	    	cities[1] = new Coordinate(49, 49);
	    	cities[2] = new Coordinate(52, 64);
	    	cities[3] = new Coordinate(20, 26);
	    	cities[4] = new Coordinate(40, 30);
	    	
	    	cities[5] = new Coordinate(21, 47);
	    	cities[6] = new Coordinate(17, 63);
	    	cities[7] = new Coordinate(31, 62);
	    	cities[8] = new Coordinate(52, 33);
	    	cities[9] = new Coordinate(51, 21);
	    	
	    	cities[10] = new Coordinate(42, 41);
	    	cities[11] = new Coordinate(31, 32);
	    	cities[12] = new Coordinate(5, 25);
	    	cities[13] = new Coordinate(12, 42);
	    	cities[14] = new Coordinate(36, 16);
	    	cities[15] = new Coordinate(52, 41);
	    	cities[16] = new Coordinate(27, 23);
	    	cities[17] = new Coordinate(17, 33);
	    	cities[18] = new Coordinate(13, 13);
	    	cities[19] = new Coordinate(57, 58);
	    	
	    	cities[20] = new Coordinate(62, 42);
	    	cities[21] = new Coordinate(42, 57);
	    	cities[22] = new Coordinate(16, 57);
	    	cities[23] = new Coordinate(8, 52);
	    	cities[24] = new Coordinate(7, 38);
	    	cities[25] = new Coordinate(27, 68);
	    	cities[26] = new Coordinate(30, 48);
	    	cities[27] = new Coordinate(43, 67);
	    	cities[28] = new Coordinate(58, 48);
	    	cities[29] = new Coordinate(58, 27);
	    	
	    	cities[30] = new Coordinate(37, 69);
	    	cities[31] = new Coordinate(38, 49);
	    	cities[32] = new Coordinate(46, 64);
	    	cities[33] = new Coordinate(61, 33);
	    	cities[34] = new Coordinate(62, 63);
	    	cities[35] = new Coordinate(63, 69);
	    	cities[36] = new Coordinate(32, 22);
	    	cities[37] = new Coordinate(45, 35);
	    	cities[38] = new Coordinate(59, 15);
	    	cities[39] = new Coordinate(5, 6);
	    	
	    	cities[40] = new Coordinate(10, 17);
	    	cities[41] = new Coordinate(21, 10);
	    	cities[42] = new Coordinate(5, 64);
	    	cities[43] = new Coordinate(30, 15);
	    	cities[44] = new Coordinate(39, 10);
	    	cities[45] = new Coordinate(32, 39);
	    	cities[46] = new Coordinate(25, 32);
	    	cities[47] = new Coordinate(25, 55);
	    	cities[48] = new Coordinate(48, 28);
	    	cities[49] = new Coordinate(56, 37);
	    	
	    	cities[50] = new Coordinate(30, 40);
	    	   	

	    	matrix = new float[nrOfCities * nrOfCities];
	    	
	    	for(int i = 0; i <  nrOfCities; i++)
	    	{
	    		for(int j = i; j <  nrOfCities; j++)
		    	{
	    			
	    			if( i == j)
	    				matrix[(i * nrOfCities)+j] = 0;
	    			else
	    			{
	    				float distance = (float) Math.sqrt(Math.pow(cities[i].x * spread - cities[j].x * spread,2) +  Math.pow(cities[i].y * spread - cities[j].y * spread,2));
	    				matrix[(i * nrOfCities)+j] = matrix[(j * nrOfCities)+i] = distance;
	    				
	    				if (distance <= 0)
	    					System.out.printf("Matrix entry %d %d has distance 0 \n", i,j);
	    				
    				
	    			}
		    	}
	    	}
	 }
	    
	public static void main(String[] args) {
	        //Schedule a job for the event-dispatching thread:
	        //creating and showing this application's GUI.
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	generateGraph();
	            	createGraph();
	                createAndShowGUI();
	            }
	        });
	    }
		
	private static void createAndShowGUI() {
		JFrame frame = new JFrame("TSP - BIO GUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(60,80);

	
		JComponent newContentPane = new gui();
		newContentPane.setOpaque(true);
		frame.setContentPane(newContentPane);
		
		
		 frame.pack();
		 frame.setVisible(true);	
	}
		
	private static void createGraph() {

		g = new ListenableUndirectedWeightedGraph<Long, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		adapter = new JGraphModelAdapter<Long, DefaultWeightedEdge>( g );
		jgraph = new JGraph(  adapter );
		
		
		for(long i =0;i< nrOfCities; i++)
		{
			g.addVertex( i );
			
			positionVertexAt( i, cities[(int) i].x * spread, cities[(int) i].y * spread);
		}
		
	}
	
	private static void positionVertexAt( Object vertex, int x, int y ) {
        DefaultGraphCell cell = adapter.getVertexCell( vertex );
        Map              attr = cell.getAttributes(  );
        Rectangle2D        b    = GraphConstants.getBounds( attr );

        GraphConstants.setBounds( attr, new Rectangle( x, y, 20, 20 ) );

        Map cellAttr = new HashMap(  );
        cellAttr.put( cell, attr );
        adapter.edit( cellAttr, null, null, null );
    }





		
 
}

