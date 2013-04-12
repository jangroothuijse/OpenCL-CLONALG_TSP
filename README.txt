A parrallel version of CLONALG implemented as a TSP Optimizer using OpenCL.

authors Jan Groothijse, Niklas Weber, Rob Tiemens

Compilation requirements:
	Java 7

	Jars:
		JOCL
		For OpenCL bindings
		http://jocl.org/

		JGraphT
		Graph drawing library
		http://jgrapht.org/

Runtime requirements:
	To use our GPU accelerated versions, the system must have a GPU driver
	installed capable of running OpenCL 1.1, Intel, AMD and nVidia drivers 
	for videochips not older than 4 years generaly meet this demand.
	
	To use OpenCL on the CPU we recommend using SDK's from the manufacturer of your CPU:
	Intel:	http://software.intel.com/en-us/vcsource/tools/opencl-sdk
	AMD:	http://developer.amd.com/tools/heterogeneous-computing/amd-accelerated-parallel-processing-app-sdk/downloads/
	
	
Key folders:
	/src 
		contains java source code
	/kernels 
		contains a part of the kernels source code, some constants are
		added by the host code, so on their own the kernels do not compile

Key files:
	/src/nl/ru/bio/gui/gui.java										
		The gui that bind everything together

	/src/nl/ru/bio/model/Graph.java									
		Our model for a graph

	/src/nl/ru/bio/algorithm/java/ClonalgOptimController.java		
		Clonalg TSP Optimizer using pure java, used as reference point

	/kernels/CLONALGRadix.cl										
		CLonalg TSP Optimizer using OpenCL

	/src/nl/ru/bio/algorithm/opencl/CLONALGWithRadixSort.java		
		Host code for /kernels/CLONALRadix.cl

	
