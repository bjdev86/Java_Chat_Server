import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

public class ChatServerDBAsync 
{
    // Define the port that this server will be listening on.
    public static final int PORT = 90;
    public static final int CLIENT_PORT = 91;
    
    public static void main(String[] args) 
    {
        // Local Variable Declaration 
        String usrLine = "";
        AsynchronousServerSocketChannel ssc; 
        AsynchronousSocketChannel sc; 
        Selector usrSelector;
        int numKeys = 0;
        SelectionKey key; 

       // Tell the user the server is starting 
       System.out.println("Starting server...");

       try 
       {
            // Create a ServerSocket Channel 
            ssc = AsynchronousServerSocketChannel.open();

            // Bind the interal socket of the server to a given port
            ssc.bind(new InetSocketAddress(PORT));
            
            // Configure the server socket channel to be non-blocking 
            //ssc.configureBlocking(false); 

            /* Create the buffer to send a message back to the client upon 
             * connection */
            
            /* Open a selector which will allow all in the connecting  
             * socket channels to be multiplexed. The selector works  
             * withe operating system to findout which channels are 
             * ready for input. Key handles to those channels are 
             * returned, so that I/O Operations can be preformed */
            usrSelector = Selector.open(); 

            // Start an event loop that will check in for incoming connections
            while(true)
            {
                /* Let the user know the server is still checking for incoming 
                 * connections */
                System.out.print(".");
                /* Check for a new connection. Since this server channel has 
                 * been configured as non-blocking, execution flow will not 
                 * block and wait for a connection, but will move on processing 
                 * current connections. */
                sc = ssc.accept();

                /* If a connection was recieved on this iteration throught he   
                 * event loop then the socket channel will have been created by 
                 * 'accpet'. The channel will then be processed. */
                if (sc != null)
                {
                    /* Let the user know that a connection was recieved is that 
                     * a new client is connected */
                    System.out.println("\nRecieved connection from " + 
                                       sc.socket().getRemoteSocketAddress());

                    /* Jump to another method to handle the chat thread 
                     * creationg and management */

                    // Make sure the sc is non-blocking
                    sc.configureBlocking(false);

                    /* Register the recently created socket channel with the 
                     * selector used to multiplex usr connections. */
                     sc.register(usrSelector, SelectionKey.OP_READ );

                    // Create an inner event loop 
                    //while (true)
                    {
                    /* Get the number of ready keys, corresponding to the 
                     * number of ready channels. */
                    numKeys = usrSelector.select();
                    System.out.println("numKeys: " + numKeys);
                    // Make sure there are ready channels to process 
                    if (numKeys != 0)
                    {
                        // Get an iterator to all of the user channels
                        Iterator<SelectionKey> keyIt = 
                            usrSelector.selectedKeys().iterator();

                        // Loop through all the ready keys
                        while (keyIt.hasNext())
                        {
                            // Get the next key
                            key = keyIt.next();

                            /* Test whether the underlying channel registered 
                             * with this key is ready to be read from */
                            if (key.isReadable() )
                            {
                                System.out.println("The channel for the  " + "socket is readable: " + key.channel() );

                                /* Get a handle to the socket channel 
                                 * referenced by this key */
                                SocketChannel socChan = (SocketChannel) key.
                                                                  channel();
                                
                                System.out.println("scoChan address: " + 
                                                 socChan.getRemoteAddress());

                                /* Allocate a buffer to store the message from 
                                 * the socket channel */
                                ByteBuffer aLine = ByteBuffer.allocate(100);
                                
                                /* Declare a String to hold the characters of 
                                 * the incoming line buffer */
                                String lineString = "";
                                
                                // Read from the socket channel into a buffer
                                while (socChan.read(aLine) != -1)
                                    System.out.println("Reading from client");
                                          
                                aLine.flip();

                                // Build a string from the buffer 
                                System.out.println("aline capacity: " + aLine.capacity());
                                System.out.println("aline limit: " + aLine.limit());
                                System.out.println("aline capacity: " + aLine.capacity());
                                System.out.println("aline position: " + aLine.position());
                                System.out.println("hasRemaining: " + aLine.hasRemaining());
                                
                                String msg = new String ( aLine.array(), StandardCharsets.UTF_8);

                                System.out.println("Client said: " + msg);
                                
                                // Try to send the message the sender sent
                                ChatThread.multiplexMsg("sender", 
                                    new ArrayList<SelectionKey> (usrSelector.keys()), msg);
                            }
                            if (key.isWritable())
                            {
                                System.out.println("The channel for the " +
                                "socket is writable: " + key.channel());

                                // Get the socket channel the key is registered for
                                SocketChannel socChan = (SocketChannel) key.channel();
                            }

                            // Remove the key to the channel just processed
                            keyIt.remove();
                        }
                    }
                    }   
                }

                else 
                {
                    try 
                    {
                        /* If a connection was not found then hang the main 
                         * execution thread for a fraction of a second in order 
                         * to wait for a new connection to come in */
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ie)
                    {
                        assert false; // There never should be an error thrown.
                    }
                }
            } 
       }
       catch(IOException ex)
       {
           System.err.println("Server startup error: " + ex);
       }
    } 
}