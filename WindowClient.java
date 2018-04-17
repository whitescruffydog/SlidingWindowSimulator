import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.Random;

import javax.swing.JOptionPane;

public class WindowClient {
	
    public static void main(String[] args) throws IOException, InterruptedException {
    	int dataPort = 9000;
    	int ACKPort = 9100;
    	int ProbabilityOfSuccess = 75;
    	
    	String fileName;
    	int numPackets;
    	int packetsRecieved = 0;
    	Random rand = new Random();
    	
    	Socket socket;
    	PrintWriter ackSend;
    	
    	
        String serverAddress = JOptionPane.showInputDialog(
            "Enter IP Address of a machine that is\n" +
            "running the Sliding Window Server port " + dataPort);
        Socket s = new Socket(serverAddress, dataPort);
        BufferedReader input =
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        System.out.println("Receiving preliminary file data...");
        fileName = input.readLine().replace("\n",  "").replace("\n", "");
        numPackets = Integer.parseInt(input.readLine().replace("\n",  "").replace("\n", ""));
        s.close();  //get preliminary data about file
        
        System.out.println("The filename is " + fileName);
        System.out.println("The file will be received in " + numPackets + " packets.");
        
        OutputStream out;
        
        ServerSocket listener = new ServerSocket(dataPort);
		out = new FileOutputStream(fileName, false);
		
		socket = new Socket(serverAddress, ACKPort);
		ackSend = new PrintWriter(socket.getOutputStream(), true);
        while(packetsRecieved < numPackets)
        {
        	s = listener.accept();
            System.out.println("Receiving a packet ...");
            DataInputStream dIn = new DataInputStream(s.getInputStream());
			
			int ackNum = dIn.readInt();
			int length = dIn.readInt();                    // read length of incoming message
			byte[] message = new byte[1];
			if(length>0) {
			    message = new byte[length];
			    dIn.readFully(message, 0, message.length); // read the message
			}
			
			s.close();
			
			if(ackNum == packetsRecieved)
			{
    			if(rand.nextInt(100) < ProbabilityOfSuccess)
    			{
    	        	System.out.println("Received packet " + ackNum);
            		String toSend = "ACK\n" + ackNum;
            		
	        		out.write(message);
            		
            		ackSend.println(toSend);
            		
            		packetsRecieved ++;
    			}
    			else
    			{
    	        	String toSend = "NACK\n" + ackNum;
            		
            		ackSend.println(toSend);
    	        	
    	        	System.out.println("Failed to receive packet " + ackNum);
    			}	
			}
			else
			{
				System.out.println("Ignoring received packet " + ackNum);
			}
        	
        }
        Thread.sleep(3000);
        socket.close();
        out.close();
        listener.close();

        
        System.exit(0);
    }
    
    public static class dataQueue
    {
    	Queue<String> data;	
    }
}