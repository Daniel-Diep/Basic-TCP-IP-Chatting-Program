/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.io.*;
import java.net.*;

public class EchoClient implements Runnable{
	public static ServerSocket selfServerSocket; //Server of this client
	public static Socket clientSocket; //Connection to the main server

	public static Socket chatterSocket = new Socket(); //Connection from the server to send back messages
	public static boolean inChat;
	
    public static void main(String[] args) throws IOException {
        
        if (args.length != 3) { //Exits the program if host name or port or both are not given.
            System.err.println(
                "Usage: java EchoClient <host name> <port number> <listening port number>");
            System.exit(1);
        }

        String hostName = args[0]; //Holds host name of server PC
        int portNumber = Integer.parseInt(args[1]); //Holds server's port
        clientSocket = new Socket(hostName, portNumber); //Creates a socket connecting to the server on host name and port
        int listeningPort = Integer.parseInt(args[2]); //Get listening port from argument
        selfServerSocket = new ServerSocket(listeningPort); //Creating Listening port
        Thread SThread = new Thread(new EchoClient()); //Creates new thread to have server listen.
        SThread.start();
        
        try (
            
            PrintWriter out = 
                new PrintWriter(clientSocket.getOutputStream(), true); //Writes to the main server
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())); //Reads input from the server
            BufferedReader stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in)); //Reads from standard input
            
        ) {
        	out.println(listeningPort); //Gives the server the listening port of the current client
            String userInput; //Holds input string of server
            System.out.println("Client Created on " + clientSocket.getLocalPort() + " connected to " + clientSocket.getPort());
            System.out.println("Start entering commands.");
            while (((userInput = stdIn.readLine()) != null)) { //Keeps getting input from client from standard input
            	//System.out.println("In Main loop:");
            	
            	if(chatterSocket.isConnected() && !chatterSocket.isClosed()){ //Checks if the listening port accepted something and if chatterSocket was initialized.
            		System.out.println("Listening port accepted a connection.");
            		//System.out.println("Entering get loop");
            		get(stdIn);
            		//System.out.println("Leaving get loop");
            		continue;
            	}
            	
            	if(userInput.equals("CHAT")){ //If client wants to initiates a chat

            		String response;
            		out.println(userInput); //Sends 'CHAT' to server
            		response = in.readLine();
            		if(response.equals("na")){  //If no one has joined the directory yet.
            			System.out.println("Server : No one in list at the moment");
            		}
            		else if(response.equals("nil")){ //If current user is not in list then they cannot start a chat
            			System.out.println("Server : You have not joined the server yet, you must join the server first before you can chat.");
            		}
            		else{ 
                		String name;
                		boolean found = true; 
                		System.out.println(response);
                		do{
                			name = stdIn.readLine();
                    		out.println(name);
                			response = in.readLine();
                			if(response.equals("cancel")){
                				found = false;
                				break;
                			}
                			System.out.println("Server: " + response);
                			
                		}while(!(response.equals("Found user")));
                		
                		if(found){ //Found the user
                			response = in.readLine();
                			if(response.equals("Ready to chat")){ //If the user is not in chat already
                        		System.out.println("Server: " + response);
                        		int chatPort = Integer.parseInt(in.readLine());
                            	//System.out.println("Entering send loop");
                        		//System.out.println("setting both client's inChat to true");
            					send(hostName,chatPort, stdIn);
            					out.println("done");
            					//System.out.println("setting both client's inChat back to false after chat is done");
                            	//System.out.println("Leaving send loop");
                            	continue;
                			}
                			else{ //If the user is already in a chat
                				System.out.println("Server: " + response);
                			}

                		}
                		else{
                			System.out.println(in.readLine());
                		}
            		}
            	} //End of CHAT
            	
            	else{
            		out.println(userInput); //Sends whatever the client inputs to the server to process.
            		if(userInput.equals("LIST")){
            			System.out.println("Server: " + in.readLine().replaceAll(",,", "\n"));
            		}
            		else{
            			System.out.println("Server: " + in.readLine()); //Response from the server.
            		}
            	}         
            } //end of while loop
            	
            System.out.println("Exited Main loop");
        } catch (UnknownHostException e) { //Exception is thrown if the server's host name does not exist / is invalid.
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) { //Exception is throw if the connection is interrupted / socket is closed.
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
                System.out.println("Closing client socket..");
                clientSocket.close();
                System.out.println("Socket closed, returned to main menu.");
            System.exit(1);
        }
        
    } //End of main

	@Override
	public void run() { //Used to listen for connections
		// TODO Auto-generated method stub
			try {
				System.out.println("Client is now listening on port " + selfServerSocket.getLocalPort());
				while(true){
					chatterSocket = selfServerSocket.accept(); //Client socket
					System.out.println("Chat connection started, press Enter to continue.");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("IOException in run()");
			}
		}
	
	public static void send(String host, int port, BufferedReader in){ //This method is invoked when this client initiates a connection
		String hostName = host;
		int listeningPort = port; //Passed the listening port of other client's server
		BufferedReader stdIn = in;
		try (	//If this client socket leaves, it will throw an IOException
				Socket chatSocket = new Socket(hostName,listeningPort); // This client connecting to the other client's server
				PrintWriter chatOut =
			        new PrintWriter(chatSocket.getOutputStream(), true);
				BufferedReader chatIn =
	                    new BufferedReader(
	                        new InputStreamReader(chatSocket.getInputStream()));
		) 
		{
			//System.out.println("Client created on " + chatSocket.getLocalPort() + " connected to " + chatSocket.getPort());
    		System.out.println("Start chatting, enter 'CLOSE' to end chat.");
    		MessageReader mr = new MessageReader(chatIn); //MessageReader takes input coming from chatSocket's input stream and prints them out.
    		Thread MThread = new Thread(mr);
    		MThread.start();
    		String chatInput;
    		while(((chatInput = stdIn.readLine()) != null) && (mr.getPrinting())){ //How client sends to other client's server
    			//System.out.println("In send loop");
    			chatOut.println(chatInput);
    			if(chatInput.equals("CLOSE")){
    				mr.setPrinting(false);
    				break;
    			}
    		}
    		System.out.println("Closing chat socket...");
    		chatSocket.close();
    		System.out.println("Chat socket closed, returned to main menu.");
    		
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " +
	                listeningPort);
		} catch (NullPointerException e){
			System.out.println("NullPointerException in send");
		}
	}
	
	public static void get(BufferedReader in){
		BufferedReader stdIn = in;
		try ( //If this client socket leaves, these will throw an IOException
			PrintWriter chatterOut =
			        new PrintWriter(chatterSocket.getOutputStream(), true);
			BufferedReader chatterIn =
                    new BufferedReader(
                        new InputStreamReader(chatterSocket.getInputStream()));
			)
			{
    		System.out.println("Start Chatting, enter 'CLOSE' to end chat.");
    		MessageReader mr = new MessageReader(chatterIn);
    		Thread MThread = new Thread(mr);
    		MThread.start();
    		String chatInput;
    		while(((chatInput = stdIn.readLine()) != null) && (mr.getPrinting())){ //How client sends to other client's server
    			//System.out.println("In get loop");
    			chatterOut.println(chatInput);
    			if(chatInput.equals("CLOSE")){
    				mr.setPrinting(false);
    				break;
    			}
    		}
    		System.out.println("Closing Chatter socket...");
    		chatterSocket.close();
    		System.out.println("Chatter socket closed, returned to main menu.");
    		
		} catch (IOException e) {
			System.out.println("IOException in get");			
		} catch (NullPointerException e){
			System.out.println("NullPointerException in get");
		}
			
	}
}