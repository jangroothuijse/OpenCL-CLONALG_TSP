package nl.ru.bio.algorithm.opencl;

import static org.jocl.CL.*;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.*;
import java.util.*;

import nl.ru.bio.model.Graph;

import org.jocl.*;
/**
 * OpenCL Host code for massively parallel CLONALG TSP Optimizer using radix sort
 * @authors Jan Groothijse, Niklas Weber, Rob Tiemens
 */
public class CLONALGWithRadixSort {
	private cl_context context;
    private cl_command_queue commandQueue;
    private cl_kernel kernel;
    private cl_program program;
    private cl_mem memIn;
    private cl_mem memOut;
    private cl_mem seeds;
    public int[] output;
    public int time;


    protected long deviceType() {
    	return CL.CL_DEVICE_TYPE_GPU;
    }
    
    public int[] randomData(int size) {
    	int[] data = new int[size];
    	for (int i = 0; i < size; i++)
    		data[i] = (int) (Math.random() * Integer.MAX_VALUE);
    	return data;
    }
    
    public CLONALGWithRadixSort(int size, int workItems, float[] buffer, 
			int popSize, int selectionSize, 
			int generations, int mutations) {

        final int deviceIndex = 0;
        
        if (popSize % selectionSize != 0) {
        	System.out.println("WARNING: popSize has to be a multiple of selectionSize");
        }
        
        cl_device_id device = null;
        cl_context_properties contextProperties = null;
        
    	 // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);

        for (int i = 0; i < numPlatforms; i++) {
        	 // Obtain a platform ID
            cl_platform_id platform = platforms[i];
            
         	// Initialize the context properties
            contextProperties = new cl_context_properties();
            contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
            
            // Obtain the number of devices for the platform
            int numDevicesArray[] = new int[1];
            clGetDeviceIDs(platform, deviceType(), 0, null, numDevicesArray);
            int numDevices = numDevicesArray[0];
            
            System.out.println("Plaform " + i + " has " + numDevices + " compliant devices");
            
            if (numDevices > 0) {
	            // Obtain a device ID 
	            cl_device_id devices[] = new cl_device_id[numDevices];
	            clGetDeviceIDs(platform, deviceType(), numDevices, devices, null);
	            device = devices[deviceIndex];
	            
	            String deviceName = getString(devices[0], CL_DEVICE_NAME);
	            System.out.printf("CL_DEVICE_NAME: %s\n", deviceName);
	            
	            break;
            }
        }
        
        CL.setExceptionsEnabled(true);
    	 // Create a context for the selected device
        context = clCreateContext(
            contextProperties, 1, new cl_device_id[]{device}, 
            null, null, null);
        
        // Create a command-queue for the selected device
        commandQueue = 
            clCreateCommandQueue(context, device, 0, null);

        // Program Setup
        String source = readFile("kernels/CLONALGRadix.cl", size, popSize, selectionSize, generations, mutations);
        
        System.out.println("Building program");

        // Create the program
        program = clCreateProgramWithSource(context, 1, 
            new String[]{ source }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        System.out.println("Creating kernel");
        
        // Create the kernel
        kernel = clCreateKernel(program, "CLONALG", null);
        
        System.out.println("Setting kernel arguments");
        int populationSize = 512;
        // Buffer for output
        memOut = clCreateBuffer(context, CL_MEM_WRITE_ONLY,        		
        		size * Sizeof.cl_int, null, null);
        
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memOut));

        System.out.println("Made output");
                             
        cl_event bufferingInput = new cl_event();
        
        //float[] buffer = cities//nl.ru.bio.model.GraphGenerator.fullyConnectedSymmetric(size);
        System.out.println("Buffering input matrix");
        // Buffer for input
        memIn = clCreateBuffer(context, CL_MEM_READ_ONLY, 
        		size * size * Sizeof.cl_float, null, null);
        clEnqueueWriteBuffer(commandQueue, memIn, true, 0, 
        		size * size * Sizeof.cl_float, Pointer.to(buffer), 0, null, bufferingInput);
        
        CL.clWaitForEvents(1, new cl_event[]{ bufferingInput });
        System.out.println("Buffered input matrix");
        
        CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memIn));
        
        java.util.Random r = new java.util.Random();
        
        long[] seedArray = new long[workItems];
        for (int i = 0; i < workItems; i++) {
        	seedArray[i] = r.nextLong();
        }
        
        cl_event bufferingSeeds = new cl_event();
        
        seeds = clCreateBuffer(context, CL_MEM_READ_ONLY, 
        	workItems * Sizeof.cl_ulong, null, null);
        clEnqueueWriteBuffer(commandQueue, seeds, true, 0, 
        	workItems * Sizeof.cl_ulong, Pointer.to(seedArray), 0, null, bufferingSeeds);
        
        CL.clWaitForEvents(1, new cl_event[]{ bufferingSeeds });
        System.out.println("Buffered seeds");

        CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(seeds));
        
        cl_mem paths = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 
        		popSize * (Sizeof.cl_float + (64 * Sizeof.cl_uchar)), null, null);
        CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(paths));
        cl_mem translation = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 
        		2 * popSize * Sizeof.cl_uint, null, null);
        CL.clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(translation));
        
        cl_mem translation2 = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 
        		2 * popSize * Sizeof.cl_uint, null, null);
        CL.clSetKernelArg(kernel, 5, Sizeof.cl_mem, Pointer.to(translation2));
        
        cl_mem popSizeData = clCreateBuffer(context, CL_MEM_READ_ONLY, 
        		Sizeof.cl_ushort, null, null);
        cl_event bufferedPopSize = new cl_event();
        clEnqueueWriteBuffer(commandQueue, seeds, true, 0, 
        		Sizeof.cl_ushort, Pointer.to(new int[]{popSize}), 0, null, bufferedPopSize);
        CL.clWaitForEvents(1, new cl_event[]{ bufferedPopSize });
        System.out.println("bufferedPopSize");
        CL.clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(popSizeData));
        
        cl_mem counts = clCreateBuffer(context, CL.CL_MEM_READ_WRITE,        		
                4 * workItems * Sizeof.cl_uint, null, null);
        CL.clSetKernelArg(kernel, 7, Sizeof.cl_mem, Pointer.to(counts));  
        
        cl_event kernelDone = new cl_event();
        
        long[] workSize = { workItems }; 
        
        long start = java.util.Calendar.getInstance().getTimeInMillis();
        
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, 
        		workSize, null, 0, null, kernelDone);
        
        CL.clWaitForEvents(1, new cl_event[]{ kernelDone });
        long end = java.util.Calendar.getInstance().getTimeInMillis();
        
        
        System.out.println("Kernel done in " + (time = (int)(end - start)) + "ms");
        
        int[] output = new int[size];
        
        cl_event readDone = new cl_event();
        start = java.util.Calendar.getInstance().getTimeInMillis();
        CL.clEnqueueReadBuffer(commandQueue, memOut, CL.CL_TRUE, 0, size * Sizeof.cl_int, Pointer.to(output), 0, null, readDone);        
        CL.clWaitForEvents(1, new cl_event[]{ readDone });
        end = java.util.Calendar.getInstance().getTimeInMillis();
        System.out.println("Reading done in " + (end - start) + "ms");
        
        for (int i = 0; i < size; i++) System.out.println(output[i]);
        nl.ru.bio.model.Graph.printError(size, output);
       // System.out.println(nl.ru.bio.model.Graph.distance(size, buffer, output));
        System.out.println("total distance: " + Graph.distance(size, buffer, output));
        /*
        for (int j = 0; j < populationSize; j++) {
        	int[] path = new int[size];
        	for (int i = 0; i < size; i++) {
        		path[i] = output[(j * size) + i];
        	}
        	nl.ru.bio.model.Graph.printError(size, path);
        }*/
        this.output = output;
        
    }
    
    
    /**
     * Helper function which reads the file with the given name and returns 
     * the contents of this file as a String. Will exit the application
     * if the file can not be read.
     * 
     * @param fileName The name of the file to read.
     * .. parameters for the kernel...
     * @return The contents of the file
     */
    private String readFile(String fileName, int pathSize, int popSize, 
    						int selectionSize, int generations, int mutations)
    {
        try
        {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileName)));
            StringBuffer sb = new StringBuffer();
            sb.append("#define PATH_LENGTH\t\t" + pathSize + "\n"
            		+ "#define POP_MAX\t\t" + popSize + "\n"
            		+ "#define SELECTION_SIZE\t\t" + selectionSize + "\n"
            		+ "#define GENERATIONS\t\t" + generations + "\n"
            		+ "#define MUTATIONS\t\t" + mutations + "\n"	
            
            );
            String line = null;
            while (true)
            {
                line = br.readLine();
                if (line == null)
                {
                    break;
                }
                sb.append(line).append("\n");
            }
            
            return sb.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
  
    public static void main(String[] args) {
		int size = 51;
		try {
			new CLONALGWithRadixSort(size, 128, nl.ru.bio.model.GraphGenerator.fullyConnectedSymmetric(size),
					2048, 512, 512, 8);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
    }
    
    private static String getString(cl_device_id device, int paramName)
    {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int)size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length-1);
    }
}
