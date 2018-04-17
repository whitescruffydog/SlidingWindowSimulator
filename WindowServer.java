import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;


public class WindowServer implements Runnable {
	
	static ACKHandler acki = new ACKHandler();
	
	boolean end = false;
	static String clientAddress;
	static int numPackets;
	
	static final long MIN_WAIT_MS = 105;
	static final long MAX_WAIT_MS = 1100;
	
	static final long MIN_WAIT_ACK = 105;
	static final long MAX_WAIT_ACK = 1100;
	
	static final int SIZE_OF_PACKETS = 20;
	static final int MAX_WIN_SIZE = 3;
	static final String FILE_NAME = "ModernMajor.txt";
	
	static final int dataPort = 9000;
	static final int ACKPort = 9100;
	
	
	
	
	public void run() {  
		try{	//runnable cannot throw an IOException
		        ServerSocket nackack = new ServerSocket(ACKPort);
		        BufferedReader input;    	
		        Random rand = new Random();

		        try {
	                Socket s = nackack.accept();  //receiving a ack or nack
	                input = new BufferedReader(new InputStreamReader(s.getInputStream()));
	                String response;
	                int cycle = 0;
		            while (acki.numACK < numPackets) {
		            	response = input.readLine();
		            	
		            	if(cycle == 0)
		            	{
			                acki.ACKorNACK = response;
			                cycle = 1;
		            	}
		            	else if (cycle == 1)
		            	{
			                acki.acknum = Integer.parseInt(response);
			                Thread.sleep( (long) rand.nextInt((int)(MAX_WAIT_ACK - MIN_WAIT_ACK) + 1) + MIN_WAIT_ACK);
			                synchronized(acki)
			                {							
		            			if(acki.ACKorNACK.equals("ACK")) //does not handle the case of receiving ACK out of order
		            			{
		            				acki.numACK ++;
		            				acki.windowSize ++;
		            			}
		            			else
		            			{
		            				acki.windowSize = MAX_WIN_SIZE;  //this works because the packet being sent is determined by the max size, current size, and number of ack
		            			}
	                			
			                	System.out.println("Received " + acki.ACKorNACK + " for Packet " + acki.acknum);

	                			acki.ACKInterrupt = false;
	                			acki.acknum = -1;
	                			acki.ACKorNACK = null;
	                			
	                    	}	
			                cycle = 0;
		            	}
		            	
		            }
	                
	                s.close();
		        }
		        finally {
		        	System.exit(0);
		            nackack.close();
		        }
		}
		catch(Exception e)
		{
			
		}
	}
	
    /**
     * Runs the server.
     */
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(dataPort);
        long fileSizeInBytes;
        PrintWriter out;
        int PacketToSend;
    	Random rand = new Random();

        
        try {  //main body, must have try catch
            Socket socket = listener.accept();
            clientAddress = socket.getRemoteSocketAddress().toString();
            clientAddress = clientAddress.substring(1, clientAddress.indexOf(':'));
            System.out.println(clientAddress);
            File file = new File(FILE_NAME);
            if(!file.exists()) { 
                System.out.println(FILE_NAME + " was not found.  Please move the file into directory or change the filename.");
                System.exit(0);
            }
            fileSizeInBytes = file.length();  
            numPackets = (int) Math.ceil((double) fileSizeInBytes / (double) SIZE_OF_PACKETS);
            
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(FILE_NAME + "\n" + numPackets);  //send preliminary data--file name and number of packets
        	System.out.println("Sent preliminary data");
        	
            out.close();
            socket.close();
            acki.windowSize = MAX_WIN_SIZE;
            
            (new Thread(new WindowServer())).start(); //start the ack handler 
            int theWindow;
        	
            while (acki.numACK < numPackets) {
            	Thread.sleep( (long) rand.nextInt((int)(MAX_WAIT_MS - MIN_WAIT_MS) + 1) + MIN_WAIT_MS);  //simulate transmission time
            	 //check for ack interrupt
            	synchronized(acki){
            		theWindow = acki.windowSize;
            	}
                while(theWindow == 0) //we have no window, wait until we do.  basically busy waiting
                {
                	System.out.println("Waiting for window ...");
                	Thread.sleep( (long) rand.nextInt((int)(MAX_WAIT_MS - MIN_WAIT_MS) + 1) + MIN_WAIT_MS);
                	synchronized(acki){
                		theWindow = acki.windowSize;
                	}
                }
                
            	if(acki.numACK == numPackets)
            	{
            		break;
            	}
                
                //get and send data
                synchronized(acki)
                {
                    PacketToSend = (acki.numACK + (MAX_WIN_SIZE - acki.windowSize));
                    if(PacketToSend >= numPackets)
                    {
                    	
                    }
                    else
                    {
		                byte[] data = toByteArray(file, PacketToSend * SIZE_OF_PACKETS, SIZE_OF_PACKETS);
		                
		                boolean scanning=true;
		                while(scanning)
		                {
		                    try
		                    {
		                
				                socket = new Socket(clientAddress, dataPort);  //connect to client
				                scanning=false;
				                
				                DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
				                
				                dOut.writeInt(PacketToSend);  //send packet number and data
				                dOut.writeInt(data.length); // write length of the message
				                dOut.write(data);           // write the message
				                
				                System.out.println("Sent packet " + PacketToSend);
				
				                socket.close();
		                    }
		                    catch(IOException e)
		                    {
		                        try
		                        {
		                            Thread.sleep((long) rand.nextInt((int)(MAX_WAIT_MS - MIN_WAIT_MS) + 1) + MIN_WAIT_MS);
		                        }
		                        catch(InterruptedException ie){
		                            ie.printStackTrace();
		                        }
		                    } 
		                }
	                }
	                acki.windowSize --;
                }
                
            }
            
            
            
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally {
            listener.close();
            System.out.println("Goodbye");
        }
    }
    
    public static byte[] toByteArray(File file, int start, int count) {
        int length = (int) file.length();
        if (start >= length) return new byte[0];
        count = Math.min(count, length - start);
        byte[] array = new byte[count];
        InputStream in;
		try {
			in = new FileInputStream(file);
	        in.skip(start);
	        int offset = 0;
	        while (offset < count) {
	            int tmp = in.read(array, offset, (count - offset));
	            offset += tmp;
	        }
	        in.close();
		} catch (Exception e) {
			System.out.println("Error reading file");
			e.printStackTrace();
			System.exit(1);
		}
        return array;
  }
    
	public static class ACKHandler
	{
		boolean ACKInterrupt = false;
		String ACKorNACK = null;
		int acknum = -1;
		int numACK = 0;
		int windowSize;
	}  
}

