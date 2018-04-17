# Sliding Window Server and Client Simulator

## Project Completed for Wireless Networking Class

### How to Run
1. Download WindowServer.java and ModernMajor.txt onto a computer.  

1. At the command line, compile with javac WindowServer.java and run with java WindowServer.

1. On a separate computer, download WindowClient.java.

1. Compile and run WindowClient.

1. When prompted, enter the IPv4 Address of the computer running WindowServer.java.

### Important Notes

This project is a simplified version of Sliding Window Protocol.  Some liberties were taken to get to the core of the project.

1. The project begins with the Server informing the Client how many packets it will be recieving, as opposed to sending a packet with EOF data.

1. The max size of the window does not change.  This is as opposed to a true "Sliding Window" protocol which changes its max size in response to what's happening around it.  Typically, the window would increase by one when recieving an ACK, and decrease by half when recieving an NACK.

1. This was a school project intended to show the intended behavior when properly used, and is not well defended against attempts to break it with improper use.  

1. Java reserves ports for some time after one is binded.  In repeated runs of the program, it may crash due to the socket being already in use.  Changing the dataPort and ACKPort in the files will circumvent this problem.  I found no other way to get around the issue.   

### Changeable Variables

The behavior of the project can be changed by modifying the global variables in the java files.  