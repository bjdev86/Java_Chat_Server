
package chatDB;

import com.sun.source.tree.Scope;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class to define a ChatThread. A chat thread is thread that facilitates   
 * the communication amongst users (socket channels).
 * 
 * @author Ben
 */
public class ChatThread implements Runnable
{
//------------------------ PRIVATE DATA MEMBERS ----------------------------
   // Selector used to hold all the member socketchannels for this chat thread/room
   private Selector chatSelector = null; 

   // The byte into which message data will be written 
   private ByteBuffer readBuffer = ByteBuffer.allocate(100); // DIRECT???

   // Multiplexor worker to multiplex socket channels together for chatting
   private EchoWorker echoW = null;

   // Local handle to the waiting room thread from whence this chat was spawned
   private WaitingRoom waitingRoom = null; 
   
   // Local handle to the WaitingRoomWorker used to execute WaitingRoom commands
   private WaitingRoomWorker waitRoomWorker = null;
   
   // The name of this ChatThread 
   private String name = "";
   
   /* List of change requests used to flip the selector to select for 
    * write events */
    private List< ChangeRequest > changeRequests = 
                                       new LinkedList <ChangeRequest> ();

   // Map of a socket channel to a list of all  ByteBuffers to be written 
   private Map< SocketChannel, List< ByteBuffer >> pendingData = 
                       new HashMap< SocketChannel, List< ByteBuffer >>();
//--------------------------------------------------------------------------

    /**
     * Constructor for a new chat thread. A new selector is created and opened
     * for use by this chat thread. 
     * 
     * @throws IOException
     */
    public ChatThread (WaitingRoom wr, String name) throws IOException
   {
       // Open up the selector 
       this.chatSelector = Selector.open();
       
       // Set the waiting room selector thread, the creator of this ChatThread
       this.waitingRoom = wr; 
       
       // Set the name 
       this.name = name;
   }

   /* Method to add a socket channel to the selector managed by this ChatThread */
   public void addContact (SocketChannel sc) throws IOException
   {
       // Make sure the socekt channel is non-blocking
       sc.configureBlocking(false);

       /* Register the new socket channel with the chatSelector. Set the 
        * op-code for this key to read. This is done to facilitate the 
        * resting position of this server, which is to wait on the client*/
       sc.register(this.chatSelector, SelectionKey.OP_READ);

       // Let the user know a socket channel has been connected. Move this to a log with a time stamp
       System.out.println("Client Entered Chat: " + sc.toString());
       
       // Let everybody else in the chat thread know the client has joined. 
       this.multiplex(sc, (sc.getRemoteAddress() + "has joined the chat").getBytes());
       
       // Wakeup the chatSelector so that this new connection can be included
       this.chatSelector.wakeup();       
   }

   /**
    * Take the given socket channel's key out of the selector for this chat 
    * thread.
    * 
    * @param sc 
    */
   public void removeContact (SocketChannel sc) 
   {
       // Save the user's chat data?? It should be saved on the fly
       
       // Deregister a socket channel from
       sc.keyFor(this.chatSelector).cancel();
   }

   /**
    * Allows for the passing of WaitingRoomWorker jobs to be handed off to the
    * waiting room selector thread by threads outside this one. This method 
    * wraps the executor method in the WaitingRoom class.
    * 
    * @param sc The channel involved in the command to executed.
    * @param cmdString The command in byte form, to be executed. It assumed that
    *                  any variables needed to execute the command are included
    *                  in this string.
    */
   public void doWaitingRoomTask (SocketChannel sc, byte[] cmdString)
   {
       this.waitRoomWorker.processData
       (
            this.waitingRoom, sc, cmdString, cmdString.length
       );
   }
   
   // Getter/Setter for the chat thread name 
   public String getName()
   {
       return this.name;
   }
//   public void setName(String name) {this.name = name;}
   
/******************************************************************************/

    /**
     * Method to prepare a message (string of bytes) to be sent down the socket
     * channel back to the client. The method creates a new ChangeRequest that 
     * will flip the socketchannel's key's interest operation from read to write
     * and then adds the message to be sent to a queue, so that it can be written
     * via the selector in this thread. 
     * 
     * @param sc The socket channel down which the data should be sent.
     * 
     * @param data The data, String in byte form to be sent down the 
     *             SocketChannel sc.
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
           this.chatSelector.wakeup();
    }
    
   /* Method to enqueue data, so that it can be written back down the socket
    * channel. The */
   public void multiplex( SocketChannel sc, byte[] data )
   {       
       /* Get a lock from the changeRequests List object, so that any 
        * thread that accesses the list will have to wait until a current 
        * thread is done accessing the list */
       synchronized (this.changeRequests) 
       {           
           /* Get a lock over the pendingData map so that other threads don't 
            * interfer with any actions in this block */
           synchronized (this.pendingData) 
           {
               /* Loop through the keys and enqueue the data to write back to the
                * socket channels on the other keys. */
               this.chatSelector.keys().forEach((key) -> 
               {
                    /* Indicate we want the interest ops set changed for each key (socket
                     * connetion) in the chat selector. Do this so that each key's socket
                     * channel will be written to.*/ 
                     this.changeRequests.add(new ChangeRequest(
                        (SocketChannel) key.channel(), ChangeRequest.CHANGEOPS, 
                        SelectionKey.OP_WRITE));
                     
                    /* Get the socket channel associated with the key being iterated.
                     * This socket channel will be used to store and access 
                     * data queues associated with the key. */
                    SocketChannel keyChan = (SocketChannel) key.channel(); 
                    
                   /* Get the queue of byte buffers that are associated 
                    * with the socket channel passed, the socket channel to 
                    * be used to send the byte buffer(s) to the client */   
                    List<ByteBuffer> queue = (List<ByteBuffer>)this.pendingData.get(keyChan);

                    // Make sure the queue returned from the Map exsisted
                    if (queue == null) 
                    {
                        // If the queue is null then create an empty ArrayList queue
                        queue = new ArrayList<ByteBuffer>();

                       /* Map the key's socket to the newly created queue of byte 
                        * buffers */
                        this.pendingData.put(keyChan, queue);
                    }

                    /* Skip the socket channel that the data array originated 
                     * from, so that the message will not be echoed back to 
                     * sender, but to all others connected. */
                    if (keyChan != sc)
                    {
                          /* Add the data that is to be sent back to the client 
                           * to the queue. ArrayList passed by reference. */
                          queue.add(ByteBuffer.wrap(data));
                    }
               });
            }
        }
       
       // Finally, wake up our selecting thread so it can make the required changes
       this.chatSelector.wakeup();
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

       // Try reading from the channel 
       try 
       {
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
                this.echoW.processData(this, sc, this.readBuffer.array(), bytesRead);
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
       // Create a EchoWorker thread for the chat thread
       this.echoW = new EchoWorker();
       
       // Start the thread
       new Thread(this.echoW, "EchoWorker").start();
       
       // Instansiate a WaitingRoomWorker used by this chat thread 
       this.waitRoomWorker = new WaitingRoomWorker();
       this.waitRoomWorker.setName("Chat WaitingRoom Worker");
       
       // Start the waitingRoom worker 
       this.waitRoomWorker.start();
       
       /* Start an event loop that will poll the selector for events 
        * on the socket channels registered for this chat */
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
                               SelectionKey key = change.getSocketChannel().keyFor(this.chatSelector);

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
               this.chatSelector.select();

               /* Get an iterator over the selected keys from the socSelector, 
                * this are keys for which a connection, read or write event 
                * was found. */
               Iterator<SelectionKey> selectedKeys = this.chatSelector
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
           catch (Exception e)
           {
               e.printStackTrace();
           }          
       }
   }
}
