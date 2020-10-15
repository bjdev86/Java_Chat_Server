package chatDB;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServerDB implements Runnable
{
//------------------------- PRIVATE DATA MEMBERS -------------------------------
    // Private Data Constansts 
    private final String UNAME = DataSerializer.UNAME;
    private final String PSSWRD = DataSerializer.PSSWRD;
    
    /* The address and port for this server "localhost:90" */
    private InetSocketAddress hostAddress = 
        new InetSocketAddress("localhost", 90); 

    /* The connections acceptance channel */
    private ServerSocketChannel serverChannel; 

    // The selector that will be queried regarding socket channel connections
    private Selector socSelector; 

    // The byte into which message data will be written 
    private ByteBuffer readBuffer = ByteBuffer.allocate(100); // DIRECT???
   
    /* List of change requests used to flip the interest ops set of theselectable 
     * keys in the selector. */
    private List< ChangeRequest > changeRequests = new LinkedList <> ();
    
   // Map of a socket channel to a list of all  ByteBuffers to be written 
   private Map <SocketChannel, List <ByteBuffer>> pendingData = new HashMap <>();
    /* The chat thread that will multiplex multiple socket channels for a text 
     * conversation */
    private WaitingRoom waitingRoom = null;
    
    /* Authentication worker thread to handle client credential authentication 
     * (ie login events) */
    private EntranceWorker doorman = null;
    
     private Map <String, String> users = new HashMap<>();
    //--------------------------------------------------------------------------

    /* Public constructor to setup this server's connection componets and 
     * initialize the selector that will be used to detect socket channel 
     * readiness. */
    public ChatServerDB (String hostAddress, int port) throws IOException
    {
        this.users.put("admin", "password"); //TEST USERS
        // Set the host and port from the parameters passed 
        this.hostAddress = new InetSocketAddress(hostAddress, port);

        // Proceed to initialize the selector 
        this.socSelector = this.initSelector();
    }
    
    /* Method to log a user in. 
     * @TODO Add start session when a login successfully occurs.*/
    public void login (SocketChannel sc, String usrName, String pssWrd)
    {
        // Local Variable Declaration 
        String dataString = "", dbPssWrd = ""; 
        
        // Get the user name and password from the database (USE BUILT-IN Object for now)
        dbPssWrd = this.users.get(usrName);
        
        // Check to see if the password is null, set the return string accordingly
        dataString = UNAME + "=" + (dbPssWrd == null ? "false;" : "true;");
        
        // Check to see if the password passed is the same as the one on file
        dataString += PSSWRD + "=" + (!pssWrd.equals(dbPssWrd) ? "false" : "true");
        
        // See if the user name and password passed the test
        if (!dataString.contains("false"))
        {            
            try 
            {
                // Start Session data?
                
                // Move the socketChannel into the waiting room 
                this.waitingRoom.addContact(sc);

                // Remove the socket channel associated with client logging in
                this.removeClient(sc);
                
                // Let the user know a socket channel has been logged-in
                System.out.println("Client Loged-In: " + sc.toString());
       
                /* Have the waitingRoom selector thread let the client know they
                 * were logged in */
                this.waitingRoom.send(sc, dataString.getBytes());                
            } 
            catch (IOException ex) 
            {
                // Build the error message into the dataString
                dataString += DataSerializer.ERR_MSG + "=" + ex.getMessage();
                ex.printStackTrace();
            }
        }
        else
        {        
            // Send dataString back to client so they'll know what is wrong.
            this.send(sc, dataString.getBytes());
        }
    }
    
    /* Method to register a new user */
    public void register (SocketChannel sc, String usrName, String pssWrd,
                                                     String fName, String lName)
    {
        // Local Variable Declaration 
        String rsp = "";
        
        // Enter data in the database
        this.users.put(usrName, pssWrd);
        
        // Build response string 
        rsp = DataSerializer.ERRORED + "=" + "false";
        
        // Return success code to client
        this.send(sc, rsp.getBytes());
    }
    
    /* Method to remove a socket channel from the selector used by this 
     * SelectorThread */
    public void removeClient (SocketChannel sc)
    {
        sc.keyFor(this.socSelector).cancel();
    }
            
   /* Method to initialize the selector used to keep track of the socket 
    * server channel that will listen for new socket channel connections. 
    * Those new connection channels will be also registered with this 
    * selector, so that they can be monitored for readiness as it regards 
    * reading and writing. */
    private Selector initSelector() throws IOException
    {
        // Create a selector that will monitor server socket and socket channels
        Selector socketSelector = Selector.open(); 
        
        // Create a non-blocking ServerSocketChannel used by this server
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        
        // Bind the server socket to the priorly defined address and port
        this.serverChannel.socket().bind(this.hostAddress);
        
        /* Register the server socket channel, indicating an interest in 
         * accepting new connections, with the selector. */
        this.serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
    
        // Let user know the server has started and is listening 
        System.out.println("ChatServer started listening at: " + this.  
                                     serverChannel.getLocalAddress()); 

        return socketSelector;
    }

    /* Method to accept an incoming connection to this server. The method will 
     * setup the new connection channel to be non-blocking and will register it 
     * with the socSelector */
    private void accept (SelectionKey key) throws IOException
    {
        // Get a handle to the server socket channel the key represents
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel(); 
        //ssc.configureBlocking(false);
        
        // Accept the socket connection and make sure it's non-blocking
        SocketChannel sc = ssc.accept(); // Shouldn't block for long

        /* Make the socket channel non-blocking, so that it can participate in 
         * the selector */
        sc.configureBlocking(false);
        
        /* Put the socket channel in the selector, put the selector in the read 
         * position */
        sc.register(this.socSelector, SelectionKey.OP_READ);
        
        // Wake up the selector ????
        //this.socSelector.wakeup();
    }

    /**
     * 
     * @param sc
     * @param data 
     */
    public void send (SocketChannel sc, byte[] data)
    {
        /* Get a lock from the changeRequests List object, so that any thread  
          * that accesses the list will have to wait until a current thread is 
          * done accessing the list */
         synchronized( this.changeRequests ) 
         {
             // Indicate we want the interest ops set changed
             this.changeRequests.add(new ChangeRequest(sc, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
            
             // And queue the data we want written
             synchronized (this.pendingData) 
             {
                 /* Get the queue of byte buffers that are associated with the 
                  * socket channel passed, the socket channel to be used to send 
                  * the byte buffer to the client */   
                 List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(sc);

                 // Make sure the quere returned from the Map exsisted
                 if (queue == null) 
                 {
                     // If the queue is null then create an empty ArrayList queue
                     queue = new ArrayList<ByteBuffer>();

                     /* Map the passed socket to the newly created queue of byte 
                      * buffers */
                     this.pendingData.put(sc, queue);
                 }

                 /* Add the data that is to be sent back to the client to the 
                  * queue */
                 queue.add(ByteBuffer.wrap(data));
             }
           }
        
           // Finally, wake up our selecting thread so it can make the required changes
           this.socSelector.wakeup();
    }
   
   /* Method to read data from a socket channel that is sent to this server. */
   private void read ( SelectionKey key ) throws IOException
   {
       // Local Variable Declaration 
       int bytesRead = 0; boolean clientClosed = false; 

       // Get a local handle on the channel so it can be read 
       SocketChannel sc = (SocketChannel) key.channel();

       /* Clear the read buffer so it's ready for new data */
       this.readBuffer.clear();

       try 
       {
           // Try reading from the channel 
           bytesRead = sc.read(this.readBuffer); 
       }
       catch (IOException ioe)
       {
           /* If an IOException is thrown that means the connectee closed the 
           * connection; therefore, the socketchannel should be closed, and 
           * the key should be canceled */
           // key.cancel(); 
           // sc.close();
           clientClosed = true; 
           ioe.printStackTrace();
       }
       finally
       {
          /* Test to see if the end of the channel had been reached by the last 
           * read operation, or if an IOException has been thrown and the socket 
           * channel needs to close*/
           if (bytesRead == -1 || clientClosed)
           {
               // Close the connection and cancel the key 
               key.cancel();
               sc.close();
           }
           else
           {
                // Hand off the data to a worker thread for processing
                this.doorman.processData(this, sc, this.readBuffer.array(), bytesRead);
           }
       }
   } 
   
   /**
    * 
    * @param key
    * @throws IOException 
    */
   private void write (SelectionKey key) throws IOException
   {
       // Get a handle to the SocketChannel associated with the key parameter
       SocketChannel sc = (SocketChannel) key.channel();

       /* Create a flag to indicate wether or not writing should contine on 
       * the given channel associated with the key passed */
       boolean stop = false; 

       /* Get a thread lock over the pending data map of sockets to byte 
       * buffers */
       synchronized( this.pendingData )
       {
           /* Get a list of all the buffers associated with a given socket 
            * channel, the channel associated with the key passed */
           List<ByteBuffer> queue =  
               (List< ByteBuffer >)this.pendingData.get(sc);
           
           // Loop through the queue of data buffers and write each one
           while (!queue.isEmpty() && !stop)
           {
               // Get a buffer handel to the byte buffer in the queue
               ByteBuffer data = (ByteBuffer) queue.get(0);
              
               // Write the data to the channel
               sc.write(data); 

               // See if the entire buffer was written
               if (!data.hasRemaining())
               {   
                   /* If the entire buffer was written then remove the buffer 
                   * from the queue of buffers. */
                   queue.remove(0);
               }
               else 
               {
                   /* If the data buffer still hasn't been completely drained 
                    * then processing of buffers needs to stop. The most likely 
                    * reason is that underlying system buffer for the socket 
                    * channel being written to is full and so further writing 
                    * should cease until the client has a chance to drain the 
                    * buffer on their end. When the channel is ready for writing 
                    * again the rest of this  buffer aswell as the other buffers 
                    * associated with this key's socket channel will get a chance 
                    * to be written.*/
                   stop = true;
               }
           } 

           // See if the queue of byte buffers is empty 
           if (queue.isEmpty())
           {
              /* Since all data was for this key's socket channel was written 
               * the selector can go back to checking to see if this key's 
               * channel is ready for reading */
               key.interestOps(SelectionKey.OP_READ); 
           }
       } 
   } 
    @Override
    public void run() 
    {
        try
        {
            // Create an authenticator worker thread 
            this.doorman = new EntranceWorker(); 
            
            // Start the authenticator worker 
            new Thread (this.doorman, "EntranceWorker").start();
            
            // Create a new ChatThread worker 
            this.waitingRoom = new WaitingRoom();
            
            // Start the WaitingRoom thread
            new Thread (this.waitingRoom, "Waiting Room Worker").start();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
        
        // Start event loop that will be the main event loop in this server 
        while (true)
        {
            try 
            {
                /* Process any pending change requests issued by a worker 
                 * thread */
                // Get a lock over changeRequests list 
                synchronized( this.changeRequests ) 
                {
                    // Get an iterator over the all the change requests 
                    Iterator <ChangeRequest> changes = 
                                            this.changeRequests.iterator();

                    // Loop through all the changes 
                    while ( changes.hasNext() )
                    {
                        // Get the changes request instance
                        ChangeRequest change = (ChangeRequest) changes.next();

                        // Examine the change type 
                        switch( change.getType() )
                        {
                            /* The case when the type of the ChangeRequest 
                             * is CHANGEOPS, the request to change the
                             * selector from looking for readiness to read 
                             * to looking for readiness to write on a given 
                             * socket channel. */ 
                            case ChangeRequest.CHANGEOPS:
                            {
                                /* Get the key from the socSelector for the 
                                 * channel represented in the change request */
                                SelectionKey key = change.getSocketChannel()
                                                         .keyFor(this.socSelector);

                                /* Change the intereste op for the key, which 
                                 * is still attached to the selector, to the op 
                                 * that is specified by the ChangeRequest */
                                key.interestOps( change.getOps() );
                            }// EndCase
                        }// EndSwitch
                    } // End While loop

                    /* Clear change requests list so that it is ready for 
                     * new requests */
                    this.changeRequests.clear(); 
                   }
                
                /* Halt execution while the socket selector polls serversocket 
                 * channel for connection events. Essetinally execution will 
                 * block here for connections, read readiness and write 
                 * readiness */
                this.socSelector.select();

                /* Get an iterator over the selected keys from the socSelector, 
                 * this are keys for which a connection, read or write event 
                 * was found. */
                Iterator<SelectionKey> selectedKeys = this.socSelector
                    .selectedKeys().iterator();
                
                // Loop through all the selected keys and process them
                while (selectedKeys.hasNext())
                {
                    // Get the next key from the iterator for processing 
                    SelectionKey key = (SelectionKey) selectedKeys.next();

                    /* Make sure the key is valid, ie its not cancelled, its 
                     * channel nor it's selector is closed */
                    if (key.isValid())
                    {
                        /* Check to see if the event was connection accept 
                         * event */
                        if (key.isAcceptable())
                        {
                            // Accept the connection 
                            this.accept(key);
                        }
                        else if (key.isReadable())
                        {
                            // Read from the channel 
                            this.read(key);
                        }
                        else if (key.isWritable())
                        {
                            // Write to the channel
                            this.write(key);
                        }
                    }

                    // Remove the key so that it doesn't get processed again 
                    selectedKeys.remove();
                }                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }            
    }
    
    public static void main(String[] args) 
    {
        try 
        {
            // Start the server as new thread listening at localhost:90
            new Thread (new ChatServerDB("localhost", 90), "ChatServer").start();
        }

        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}
