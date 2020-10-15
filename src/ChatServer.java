import java.net.*;


import java.io.*;
import java.lang.reflect.Array; 

public class ChatServer 
{
    // Define the port that this server will be listening on.
    public static final int PORT = 90;
    
    public static void main(String[] args) throws Exception 
    {
       /* Setup a TCP connection hook, so that this server will be able to 
        * listen for incoming socket connection requests */
        try (ServerSocket server = new ServerSocket(PORT))
        {
            // Let the user know the server was started and is listening @ PORT
            System.out.println("Server Started, listening on localhost @ port: " + PORT);
            
            /* Handle each connection iterively, so that the server doesn't 
            * need to shut down after every connection */
            while (true)
            {                
               /* Wait for a new connection to come in. When a new connection 
                * comes in get the communication socket that results after the 
                * handshake between client and server is complete */
                try
                {
                    Socket socket = server.accept();
                    // Create a chat thread 
                    Thread chatTask = new ChatThread(socket);

                    // Start the chat thread
                    chatTask.start();
                }
                catch (IOException e)
                {
                    System.err.println(e);
                }
            } // While loop            
        }
        catch(IOException e)
        {
            System.err.println("Couldn't start server");
            System.err.println(e);
        }
    } 

    // Private class to define a thread to execute the protocol of this server 
    private static class ChatThread extends Thread
    {
        // Local Variable Declaration 
        private Socket socket; // Local handle to client socket

        /* Constructor to initialize this thread with a copy of the client 
         * socket connection */
        ChatThread (Socket connection)
        {
            this.socket = connection; 
        }

        /**
         * Define the run method for this thread to execute the chat protocol
         */
        @Override
        public void run()
        {
            // String to contain the line of txt sent by the client
            String line = "";  
            char[] lineBuff = new char[100];

            try
            {
               /* Let the client know that the connection is 
                * established */         
                System.out.println("Client connected");
                
                /* Setup the Input stream, so as to come from the client 
                 * socket connection */ 
                BufferedReader input = new BufferedReader(new InputStreamReader
                                           (socket.getInputStream() ));
                
               /* Start a loop that will allow this server to iterively 
                * handle each incoming connection */
                while (!line.equals("Over"))
                {
                    // Read the line sent over the websocket
                    line = input.readLine(); 
                    
                    // Print the line out to the user
                    System.out.println(line); 
                }
                
                /* When the user is done chatting tell the user the connection 
                 * will be closing */
                System.out.println( "Closing connection" );
                
                // Close the connection, server is still up though 
                input.close();
                socket.close();
            }
            catch(IOException e)
            {
                System.err.println(e);
            }
        }
    }
}
