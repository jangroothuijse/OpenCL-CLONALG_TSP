package nl.ru.bio.algorithm.opencl;

import org.jocl.CL;
/**
 * A version that looks for a CPU type OpenCL device, 
 * so its still OpenCL, but compiled by the a compiler made by your CPU
 * manufacturer, using every last instruction set extension available on it.
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public class CLONALGWithMergeSortCPU extends CLONALGWithMergeSort {
    protected long deviceType() {
    	return CL.CL_DEVICE_TYPE_CPU;
    }
	public CLONALGWithMergeSortCPU(int size, int workItems, float[] buffer,
			int popSize, int selectionSize, int generations, int mutations) {
		super(size, workItems, buffer, popSize, selectionSize, generations, mutations);
		// TODO Auto-generated constructor stub
	}

    public static void main(String[] args) {
		int size = 64;
		try {
			new CLONALGWithMergeSortCPU(size, 1, nl.ru.bio.model.GraphGenerator.fullyConnectedSymmetric(size), 
									2048, 512, 256, 8);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
    }
}
