/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ben
 */
public class WaitingRoom implements Runnable 
{
//-------------------------- PRIVATE DATA MEMBERS ------------------------------
   // Selector used to hold all the member socketchannels for this chat thread/room
   private Selector waitSelector = null; 

   // The byte into which message data will be written 
   private ByteBuffer readBuffer = ByteBuffer.allocate(100); // DIRECT???
    
   // Map of a socket channel to a list of all  ByteBuffers to be written 
   private Map <SocketChannel, List <ByteBuffer>> pendingData = new HashMap <>();
   
   /* List of change requests used to flip the interest ops set of theselectable 
    * keys in the selector. */
    private List< ChangeRequest > changeRequests = new LinkedList <> ();
   
    // Set to hold the selection keys, clients, marked for deletion (logging off)
    private Set <SelectionKey> clientsToRemove = new HashSet<>();
    
   // Authentication worker thread used to authenticate credentials concurrently
   private WaitingRoomWorker cmdExecutor = null;
   
   // ChatThread used as the chat room for this chat server
   private ChatThread chatThread = null;
   
   // Map to hold a list of all the ChatThreads by name 
   private Map <String, ChatThread> chatThreads = new HashMap<>();
   
   private Map <String, String> users = new HashMap<>();
/*----------------------------------------------------------------------------*/
   
   public WaitingRoom () throws IOException 
   {
       // Setup temporary user credentials
       users.put("admin", "password");
       
       // Open up the waiting room selector 
       this.waitSelector = Selector.open();
   }
   
   /**
    * Method to add a socket channel to the selector managed by this ChatThread 
    *
    * @param sc The socket channel to register on this thread's selector
    * 
    * @throws java.io.IOException
    */
   public void addContact (SocketChannel sc) throws IOException
   {
       // Make sure the socekt channel is non-blocking
       sc.configureBlocking(false);

       /* Register the new socket channel with the chatSelector. Set the 
        * op-code for this key to read. This is done to facilitate the 
        * resting position of this server, which is to wait on the client*/
       sc.register(this.waitSelector, SelectionKey.OP_READ);
       
       // Wakeup the chatSelector so that this new connection can be included
       this.waitSelector.wakeup();
   }

    /**
     * Take the given socket channel's key out of the selector for this chat 
     * thread.
     * 
     * @param sc The socket channel to remove from this thread's selector
     */
    public void removeContact (SocketChannel sc) 
    {
        // Deregister a socket channel from
        sc.keyFor(this.waitSelector).cancel();       
    }
    
    /**
     * Method to create a new ChatThread. The chat thread will be empty and 
     * ready to join. Confirmation of the new thread's creation will be sent 
     * back to the client. 
     * 
     * @param sc The socket channel over which the command to create the new 
     *           ChatThread came. 
     * 
     * @param chatName The name of the new ChatThread.
     */
    public void createChatThread (SocketChannel sc, String chatName)
    {
        // Local Variable Declaration 
        ChatThread newChat = null; 
        String rsp = ""; 
        
        try 
        {
            // See if this chat thread already exsists
            if (!this.chatThreads.containsKey(chatName))
            {
                // Spin up a new chat thread
                newChat = new ChatThread(this, chatName);

                // Start it running 
                new Thread(newChat, chatName + " Chat Thread").start();

                // Let the client know that the chat was successfully created 
                this.send
                (
                    sc, (DataSerializer.ERRORED + DataSerializer.KV_DELM + 
                         "false").getBytes()
                );

                // Add the new chat thread to the list of chat threads 
                this.chatThreads.put(chatName, newChat);
            }
        } 
        catch (IOException ex) 
        {
            // Build response string 
            rsp = DataSerializer.ERRORED + DataSerializer.KV_DELM + "true"
                + DataSerializer.ERR_MSG + DataSerializer.KV_DELM 
                + ex.getMessage();
            
            Logger.getLogger(WaitingRoom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
   /**
    * Method to allow a given socket channel and therefore a remote client to 
    * enter or join a given chat thread. 
    * 
    * @param chatName The name of the chat thread to joined
    * @param sc The socketChannel that will be joining a chat thread in progress
    */
    public void joinChatThread(String chatName, SocketChannel sc)
    {        
        // Local Varaible Declaration 
        String rsp = "";
        ChatThread chatThread; 
        
        try 
        {
            // Look up the chatThread to join by name 
            chatThread = this.chatThreads.get(chatName);
            
            // Add the socket channel to the chat thread passed to this method
            chatThread.addContact(sc);
           
            /* Remove the socket channel for this waiting room selector, so that
             * communication over this socket channel is handled by the chat 
             * thread. */
            this.removeContact(sc);
        
            // Build response string
            rsp = DataSerializer.ERRORED + "=" + "false";  
            
            // Have the ChatThread send the response message back to the clinet 
            chatThread.send(sc, rsp.getBytes());
        } 
        catch (IOException ioe) 
        {
            // Build response string 
            rsp = DataSerializer.ERRORED + DataSerializer.KV_DELM + "true"
                + DataSerializer.ERR_MSG + DataSerializer.KV_DELM 
                + ioe.getMessage();
            
            // Send the error message onto the client
            this.send(sc, rsp.getBytes());
            
            ioe.printStackTrace();
        }
    }
    
    /**
     * 
     * @param chatName
     * @param sc 
     */
    public void leaveChatThread(String chatName, SocketChannel sc)
    {
        // Local Variable Declaration 
        String rsp = "";
        ChatThread chatThread; 
        
        try 
        {
            // Look up the chatThread to leave by name 
            chatThread = this.chatThreads.get(chatName);
            
            // Register (move) the socketchannel to this selector thread's selector
            this.addContact(sc);
            
            // Remove the client from the chat thread 
            chatThread.removeContact(sc);
            
            /* Send a message to the other members of the chat thread that a user 
             * has left */
            chatThread.multiplex(null, ("Member left: " + sc.toString()).getBytes());
            
            // Build a response string 
            rsp = DataSerializer.ERRORED + "=false";
            
            // Send a success response to the client from this selector thread 
            this.send(sc, rsp.getBytes());
        }
        catch(IOException ioe)
        {
            // Build response string 
           rsp = DataSerializer.ERRORED + DataSerializer.KV_DELM + "true"
                + DataSerializer.ERR_MSG + DataSerializer.KV_DELM 
                + ioe.getMessage();
            
            // Send the error message onto the client
            this.send(sc, rsp.getBytes());
            
            ioe.printStackTrace();
        }
    }
    
    /**
     * 
     * 
     * @param sc 
     */
    public void logOff (SocketChannel sc)
    {
        // Local Variable Declaration 
        String rsp = ""; 
                
        // Log the time the client was signed off
        System.out.println("Client Logged-off: " + sc.toString());
        
        // Build response string 
        rsp = DataSerializer.ERRORED + DataSerializer.KV_DELM + "false";
        
        // Send the message back 
        this.send(sc, rsp.getBytes());
    
        // Mark the socket, client, for removal by adding it to the set
        this.clientsToRemove.add(sc.keyFor(this.waitSelector));
    }
/******************************************************************************/    
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
           this.waitSelector.wakeup();
    }
   
   /* Method to read data from a socket channel that is sent to this server. */
   public void read ( SelectionKey key ) throws IOException
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
                this.cmdExecutor.processData(
                    this, sc, this.readBuffer.array(), bytesRead
                );
           }
       }
   } 
   
   /**
    * 
    * @param key
    * @throws IOException 
    */
   public void write (SelectionKey key) throws IOException
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
           List<ByteBuffer> queues =  
               (List< ByteBuffer >)this.pendingData.get(sc);
           
           // Loop through the queue of data buffers and write each one
           while (!queues.isEmpty() && !stop)
           {
               // Get a buffer handel to the byte buffer in the queue
               ByteBuffer data = (ByteBuffer) queues.get(0);
              
               // Write the data to the channel
               sc.write(data); 

               // See if the entire buffer was written
               if (!data.hasRemaining())
               {   
                   /* If the entire buffer was written then remove the buffer 
                   * from the queue of buffers. */
                   queues.remove(0);
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
           if (queues.isEmpty())
           {
              /* Since all data was for this key's socket channel was written 
               * the selector can go back to checking to see if this key's 
               * channel is ready for reading */
               key.interestOps(SelectionKey.OP_READ); 
              
                /* Check to see if this key was marked for removal, this was it's
                 * last write */
                if (this.clientsToRemove.remove(key))
                {
                    // Cancel the key and remove it from the selector
                    key.cancel();
                }
           }
       } 
   } 
   
    @Override
    public void run() 
    {
        /* Instansiate an WaitingRoomWorker */
        this.cmdExecutor = new WaitingRoomWorker();
        /* Start up the authentication worker thread, so it's ready to
        * process authentication jobs */
        this.cmdExecutor.start();
        
        /* Start an event loop for this thread that will run for the life of the
         * thread */
        while( true )
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
                                                         .keyFor(this.waitSelector);

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

                /* Halt execution while the socket selector polls the
                 * serversocket channels for read/write events */
                this.waitSelector.select();

                /* Get an iterator over the selected keys from the socSelector, 
                 * this are keys for which a connection, read or write event 
                 * was found. */
                Iterator<SelectionKey> selectedKeys = this.waitSelector
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
                        // Check to see the channel is readable 
                        if (key.isReadable()) 
                        {
                            // Read the from the channel 
                            this.read(key);
                        }
                        else if (key.isWritable())
                        {
                            // Write to the channel 
                            this.write(key);
                        }
                    }

                   /* Remove the key so that it doesn't get processed 
                    * again */ 
                   selectedKeys.remove();
               }
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }    

}
