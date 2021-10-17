import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class Chat {

	//if the user enters a valid port number( a number) then start the chat loop.
	 public static void main(String[] arg){
	        if(arg != null && arg.length > 0){
	            try{
	                int listenPort = Integer.parseInt(arg[0]);
	                Chat chatApp = new Chat(listenPort);
	                chatApp.startChat();
	            }catch(NumberFormatException nfe){
	                System.out.println("Invalid Argument for the port");
	            }
	        }else{
	            System.out.println("Invalid Args : run with 'java chat.Chat <PORT#>'");
	        }
	    }

    private int myPort;
    private InetAddress myIP;
    private Map<Integer, Destination> destinationsHosts = new TreeMap<>();
    private int clientCounter = 1;
    private Server messageReciever ;

//required meathods
    private Chat(int myPort) {
        this.myPort = myPort;
    }

    private String getmyIP(){
        return myIP.getHostAddress();
    }

    private int getmyPort(){
        return myPort;
    }

    private void help(){
        System.out.println("Available options: ");
        System.out.println("myip - Displays the IP address of this process.");
        System.out.println("myport - Displays the port listening for incoming connections.");
        System.out.println("connect <Destination> <dest-port> - Connect with destination IP and Port");
        System.out.println("list - Displays a numbered list of all the connections this process is part of.");
        System.out.println("terminate <connection id.> - close the connection for the selected id");
        System.out.println("send  <connection id.>  <message> - Sends a message using the connection id");
        System.out.println("exit - closes all connections and terminate the process");
        System.out.println("\n");

    }

    private void sendMessage(String[] commandArg){
        if(commandArg.length > 2){
            try{
                int id = Integer.parseInt(commandArg[1]);
                Destination destinationHost = destinationsHosts.get(id);
                if(destinationHost != null){
                    StringBuilder message = new StringBuilder();
                    for(int i = 2 ; i < commandArg.length ; i++){
                        message.append(commandArg[i]);
                        message.append(" ");
                    }
                    destinationHost.sendMessage(message.toString());
          }else
          System.out.println(
					"No Connection available with provided connection ID check the list command");

            }catch(NumberFormatException ne){
                System.out.println("Invalid Connection ID check the list command");
            }
        }else{
        System.out.println("Invalid command format use: send <connection id.> <message>");
        }
    }

    private void listDestinations(){
        System.out.println("Id:\tIP Address\tPort");
        if(destinationsHosts.isEmpty()){
            System.out.println("No Destinations available");
        }else{
            for(Integer id : destinationsHosts.keySet()){
                Destination destinationHost = destinationsHosts.get(id);
                System.out.println(id+"\t"+destinationHost.toString());
            }
        }
        System.out.println();
    }

    private void connect(String[] commandArg){

        if(commandArg != null && commandArg.length == 3){
            try {
                InetAddress remoteAddress = InetAddress.getByName(commandArg[1]);
                int remotePort = Integer.parseInt(commandArg[2]);
								System.out.println("Connecting to " + remoteAddress + " on port: " +remotePort);

                Destination destinationHost = new Destination(remoteAddress,remotePort);
                if(destinationHost.initConnections()){
                	destinationsHosts.put(clientCounter, destinationHost);
                    System.out.println("Connected successfully, client id: " + clientCounter++);

                }else{

                    System.out.println("Unable to establish connection try again");
                }
            }catch(NumberFormatException ne){
                System.out.println("Invalid Remote Host Port unable to connect");
            }catch (UnknownHostException e) {
                System.out.println("Invalid Remote Host Address unable to connect");
            }
        }else{
					//trying to connect  with no/wrong port
            System.out.println("Invalid command format use: connect <destination> <port no>");
        }

    }

    private void terminate(String[] commandArg){
			if(commandArg != null){
				System.out.println("Attempting to terminate Cid: " + commandArg[1]);
					try {
						int id = Integer.parseInt(commandArg[1]);
						if(destinationsHosts.containsKey(id) == false) {
							System.out.println("Invalid connection ID unable to terminate try list");
							return;
						}	//continue if theres a valid id

	    			Destination destinationHost = destinationsHosts.get(id);
						boolean closed = !destinationHost.closeConnection();
						if(closed){
							System.out.println("ConnectionID: "+ id + " was terminated");
							destinationsHosts.remove(id);
						}

					}catch(NumberFormatException e){
                System.out.println("Invalid connection ID unable to terminate");
									}
				}else {
						System.out.println("Invalid command format use: terminate <connectionID>");
					}

    }

    private void startChat(){


        Scanner scanner = new Scanner(System.in);
        try{

        	 myIP = InetAddress.getLocalHost();
             messageReciever = new Server();
             new Thread(messageReciever).start();


            while(true){
                System.out.print("Enter the command :");
                String command = scanner.nextLine();
                if(command != null && command.trim().length() > 0){
                    command = command.trim();
                    if(command.equalsIgnoreCase("help") || command.equalsIgnoreCase("/h") || command.equalsIgnoreCase("-h")){
                    	help();
                    }else if(command.equalsIgnoreCase("myip")){
                        System.out.println(getmyIP());
                    }else if(command.equalsIgnoreCase("myport")){
                        System.out.println(getmyPort());
                    }else if(command.startsWith("connect")){
                        String[] commandArg = command.split("\\s+");
                        connect(commandArg);
                    }
                    else if(command.equalsIgnoreCase("list")){
                    	 listDestinations();
                    }
                    else if(command.startsWith("terminate")){
                    	String[] args = command.split("\\s+");
                        terminate(args);
                    }
                    else if(command.startsWith("send")){
                    	String[] commandArg = command.split("\\s+");
                        sendMessage(commandArg);
                    }
                    else if(command.startsWith("exit")){

											  System.out.println("Closing connections");
                        System.out.println("Chat closed");
                        closeAll();
                        System.exit(0);
                    }else{
                        System.out.println("Invalid command");
                        System.out.println();
                    }
                }else{
                    System.out.println("Invalid command");
                    System.out.println();
                }

            }
        }catch (UnknownHostException e) {
            e.printStackTrace();
        }finally{
            if(scanner != null)
                scanner.close();
            closeAll();
        }
    }
    private void closeAll(){
        for(Integer id : destinationsHosts.keySet()){
            Destination destinationHost = destinationsHosts.get(id);
            destinationHost.closeConnection();
        }
        destinationsHosts.clear();
        messageReciever.stopChat();
    }

	//Client class 
    private class Clients implements Runnable{

        private BufferedReader in = null;
        private Socket clientSocket = null;
        private boolean isStopped = false;
        private Clients(BufferedReader in,Socket ipAddress) {
            this.in = in;
            this.clientSocket = ipAddress;
        }

        @Override
        public void run() {

            while(!clientSocket.isClosed() && !this.isStopped)
            {
                String st;
                try {
                    st = in.readLine();
										if(st == null){
											 stop();	//the connection was closed.
											 System.out.println("Connection was terminated by: "
											+clientSocket.getInetAddress().getHostAddress()
											+":"+clientSocket.getPort()+". ");

											 return;
										 }
					System.out.println("Message recived from " + clientSocket.getInetAddress().getHostAddress());
                    System.out.println("Sender's Port:"+clientSocket.getPort());
                    System.out.println("Message:"+ st);

                } catch (IOException e) {
                	e.printStackTrace();
                }
            }
        }

        public void stop(){

            if(in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }

            if(clientSocket != null)
                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
            isStopped = true;
            Thread.currentThread().interrupt();
        }

    } //end of client class

//Server class 
    private class Server implements Runnable{

        BufferedReader in = null;
        Socket socket = null;
        boolean isStopped ;
        List<Clients> clientList = new ArrayList<Clients>();


        @Override
				public void run() {

            ServerSocket s;
            try {
                s = new ServerSocket(getmyPort());
                System.out.println("Server Waiting For The Client");
                while(!isStopped)
                {
                    try {
                        socket = s.accept();
                        in = new BufferedReader(new
                                InputStreamReader(socket.getInputStream()));
                        System.out.println(socket.getInetAddress().getHostAddress()
												+":"+socket.getPort()+" : client successfully connected.");

                        Clients clients = new Clients(in, socket);
                        new Thread(clients).start();
                        clientList.add(clients);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e1) {

            }

        }

        public void stopChat(){
            isStopped = true;
            for(Clients clients : clientList){
                clients.stop();
            }
            Thread.currentThread().interrupt();
        }

    }



}//end of server class


//Destination class
class Destination{

    private InetAddress remoteHost;
    private int remotePort;
    private Socket connection;
    private PrintWriter out;
    private boolean isConnected;

    public Destination(InetAddress remoteHost, int remotePort) {

        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public boolean initConnections(){
        try {
            this.connection = new Socket(remoteHost, remotePort);
            this.out = new PrintWriter(connection.getOutputStream(), true);
            isConnected = true;
        } catch (IOException e) {

        }
        return isConnected;
    }
    public InetAddress getRemoteHost() {
        return remoteHost;
    }
    public void setRemoteHost(InetAddress remoteHost) {
        this.remoteHost = remoteHost;
    }
    public int getRemotePort() {
        return remotePort;
    }
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public void sendMessage(String message){
        if(isConnected){
            out.println(message);
        }
    }
    public boolean closeConnection(){

        if(out != null)
            out.close();
        if(connection != null){
            try {
                connection.close();
            } catch (IOException e) {
            }
        }
        isConnected = false;
				return isConnected;
    }
    @Override
    public String toString() {
        return  remoteHost + "\t" + remotePort;
    }
}	//end of destination class