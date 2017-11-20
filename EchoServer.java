/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EchoServer implements Runnable {
	
	public static Socket clientSocket; //Socket of clientSocket
	private static HashMap<String, ArrayList<Object>> players = new HashMap<String, ArrayList<Object>>(); //List of players connected to server currently (name [port, listeningPort])
	
	public synchronized boolean join(Integer port,String name,Integer serverPort,Boolean status){ //Checks if client is already in list, and only adds if they are not
		if(players.containsKey(name) != true){ //Returns true if not in list, else return false.
			ArrayList<Object> temp = new ArrayList<Object>();
			temp.add(0,port); //Client port connect to server's listening port
			temp.add(1,serverPort); //Client's own listening port
			temp.add(2,status); //Status : if client is in chat (false = not in chat, true = in chat)
			players.put(name,temp);
			return true;
		}
		else{
			return false;
		}
	}
	
	
	public synchronized boolean leave(String name){ //Checks if client is in list and removes them from it.
		if(players.containsKey(name) == true){ //Returns true if is in list, else returns false.
			players.remove(name);
			return true;
		}
		else{
			return false;
		}
	}
	
	
	public synchronized HashMap<String, ArrayList<Object>> getPlayers(){ //Returns the list.
		return players;
	}
	
	public void run(){
		Socket CSocket = clientSocket; //Socket of client
		String hostName = CSocket.getInetAddress().getHostName(); //Host name of client's PC
		int portNum = CSocket.getPort(); //Unique port # of client
		int portNumOfServer; //Listening port of current client
		String username = null; //Holds UNIQUE username of current client.
		String user = null; //Holds username of client that this client is talking to.
		boolean inList = false; //Checks to see if client is already in the list with the above username
		boolean inChat = false; //Checks if user is free to chat.
		boolean cInChat = false; /*Safety net flag to see if client (chat initiator) is currently in 
									chat and if program suddenly terminates, it will send a message to the server to set other client to not in chat*/
		try(
            PrintWriter out =
                new PrintWriter(CSocket.getOutputStream(), true);                   
            BufferedReader in = new BufferedReader(
                new InputStreamReader(CSocket.getInputStream()));
		) {
			String inputLine; //Holds input string of client

			inputLine = in.readLine();
			portNumOfServer = Integer.parseInt(inputLine);
			System.out.println(hostName + " through port # " + portNum +" has connected and has a listening port of " + portNumOfServer + ".");
			//System.out.println("Client local port " + CSocket.getLocalPort() + " INetAddress " + CSocket.getInetAddress() + " Local Address " + CSocket.getLocalAddress());
			while((inputLine = in.readLine()) != null){ //Checks for whenever clients enters a command
				
				
				if(inputLine.equals("JOIN")){ //When client enters 'JOIN' command
					if(inList == true){ //Checks to see if client has already joined the list.
						out.println("Already in list as " + username + ". Please leave first then try again.");
					}
					else{
						while(inList == false){ //Asks for a UNIQUE name, and repeats if name is already being used and is in the list.
							out.println("Enter a UNIQUE name to add to list: ");
							username = in.readLine();
							if(this.join(portNum,username,portNumOfServer,inChat) == true){
								System.out.println(hostName + " through port # " + portNum + " has successfully joined the list as " + username);
								out.println("You have joined the list with the UNIQUE name of " + username);
								inList = true;
							}
						}
					}
				}

				else if(inputLine.equals("LEAVE")){ //When client enters 'LEAVE' command
					if(inList == false){ //Check to see if client is not in the list.
						out.println("You're are currently not in the list.");
					}
					else{ //Removes the specific name of the client from the list.
						this.leave(username);
						inList = false;
						System.out.println(hostName + " through port # " + portNum + " with username " + username + " was successfully removed from the list.");
						out.println("You with the username " + username + " was successfully removed from the list.");
					}
				}
				
				
				else if(inputLine.equals("LIST")){ //When client enters 'LIST' command
					HashMap<String, ArrayList<Object>> listOfPlayers = this.getPlayers();
					
					StringBuffer stringBf = new StringBuffer();
					stringBf.append("List of Players who joined:");
					if(listOfPlayers.isEmpty()){
						System.out.println(hostName + " through port # " + portNum + " requested to see the list of players.");
						stringBf.append(" No one has connected to the server yet.");
					}
					else{
						System.out.println(hostName + " through port # " + portNum + " requested to see the list of players.");
						for(Entry<String, ArrayList<Object>> entry : listOfPlayers.entrySet()){ //Key = username, arraylist[port, listenPort, status]
							stringBf.append(",," + "Host Name: " + hostName + " Username: " + entry.getKey() + " Listening Port:" + entry.getValue().get(1) + " In Chat?: " + entry.getValue().get(2));
						}
					}
					out.println(stringBf);
				}
				
				else if(inputLine.equals("CHAT")){ //When client enters 'CHAT' command

					System.out.println(username + " requested a chat");
					
					if (players.isEmpty()){ //If no one has joined the directory yet, notify the client and they will print an appropriate response.
						out.println("na");
					}
					else if(!inList){ //If current user is not in list then they cannot start a chat
						out.println("nil"); 
					}
					else{
						boolean found = false; //This is used to check if the server is able to find the client online to chat with
						out.println("Server : Enter the name of the user you would like to chat with or enter 'CANCEL' to cancel the search.");
						while((user = in.readLine()) != null){ //Search map for user to see if they are online.
							if(players.containsKey(user)){ //Makes sure that the client cannot chat with themselves
								if(user.equals(username)){
									out.println("Cannot chat with yourself! Try again.");
								}
								else{ //
									out.println("Found user"); //Change to better message (same with client)
									found = true;
									break;
								}
							}
							else if(user.equals("CANCEL")){ //Option to cancel search for a chat
								out.println("cancel");
								break;
							}
							else{ //If user is not in the directory/does not exist, the client can try again
								out.println("Try Again");
							}
						}
						if(found){ //If the client is online and in the directory, it goes to this loop
							HashMap<String, ArrayList<Object>> listOfPlayers = this.getPlayers();
							ArrayList<Object> temp = listOfPlayers.get(user); //Holds the person you want to chat with info
							ArrayList<Object> temp2 = listOfPlayers.get(username); //Copy of current client's information
							if(!((boolean) temp.get(2))){ //This is used to check if the client is already in a chat or not
								String tmp1; //Temporary string
								
								System.out.println("Setting " + username + "'s and " + user + "'s status to inChat.");
								temp.add(2, true);
								temp2.add(2, true);
								players.put(user, temp);
								players.put(username, temp2);
                        		cInChat = true;
								out.println("Ready to chat");	
								System.out.println("Sending listening port");
								out.println(temp.get(1)); //Listening port
								
								System.out.println("Waiting for chat to end"); 
								tmp1 = in.readLine(); //Waits for the 'done' message when client is done chatting so that it can set back both to not in chat
								if(tmp1.equals("done")){
									if(players.containsKey(user)){ //This is to set both
										System.out.println("Setting " + username + "'s and " + user + "'s status to not inChat.");
										temp.add(2, false);
										temp2.add(2, false);
										players.put(user, temp);
										players.put(username, temp2);
		            					cInChat = false;
										System.out.println("ok");
									}
									else{ //This is just to set the current's user status incase the other client's program closed suddenly
										System.out.println("Setting " + username + "'s status to not inChat.");;
										temp2.add(2, false);
										players.put(username, temp2);
		            					cInChat = false;
										System.out.println("ok");
									}
								}
							}
							else{ //If user is already in a chat, it tells the client to try again later
								out.println("User is already in chat, try again later.");
							}
						}
						else{ //If no user is found and the client gives up then they can cancel.
							System.out.println(hostName + " stopped searching.");
							out.println("Cancelled search for a chat.");
						}
					}
				}
				else{ //When client enters an invalid command
					out.println("Invalid command");
				}
			}
			
			CSocket.close(); //Closes the client socket when it is done.
			System.out.println(hostName + " through port # " + portNum + " has disconnected."); //When a client disconnects/terminates (Ctrl + C) they will throw this exception, take them off the list.
			if(players.containsKey(username) == true){
				players.remove(username);
				System.out.println("Username " + username + " removed from list.");
			}
		}
		catch (IOException e){ 
			System.err.println("Couldn't get I/O for the connection to " +
                hostName + " through port # " + portNum);
			System.out.println(hostName + " through port # " + portNum + " has disconnected."); //When a client disconnects/terminates (Ctrl + C) they will throw this exception, take them off the list.
			if(cInChat){
				System.out.println("Client was in chat, setting other client back to not in chat");
				HashMap<String, ArrayList<Object>> listOfPlayers = this.getPlayers();
				ArrayList<Object> temp = listOfPlayers.get(user); //Holds the person you want to chat with info
				temp.add(2,false);
				players.put(user, temp);
			}
			if(players.containsKey(username) == true){
				players.remove(username);
				System.out.println("Username " + username + " removed from list.");
			}
		}
	}
	
    public static void main(String[] args) throws IOException {
        
        if (args.length != 1) { //Exits the program if no port number is provided.
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        
        int portNumber = Integer.parseInt(args[0]); //Holds port number for server
        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        try
        	{
            Boolean isListening = true; //Is true whenever server is listening for Clients.
            System.out.println("Server opened.");
            while(isListening){ //Continues to loop, while listening for new clients and creates new thread for each client
            	clientSocket = serverSocket.accept();
            	Thread SThread = new Thread(new EchoServer());
            	SThread.start();
            }
            System.out.println("Closing server socket.."); //Closing server socket
            serverSocket.close();
            System.out.println("Socket closed.");
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
            System.out.println("Closing server socket.."); //Closing server socket
            serverSocket.close();
            System.out.println("Socket closed.");
        }
    }
}
