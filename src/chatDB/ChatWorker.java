
package chatDB;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class to define a worker thread responsible for echoing messages received from
 * one client out to all the clients involved in <code>ChatRoom</code> thread.
 * This worker thread receive raw data along with the socket connection over 
 * which the raw data came and created a <code>ChatDataEvent</code>. This thread
 * is then interrupted and the <code>ChatDataEvent</code> is handled. 
 * <br><br>
 * The handling of that events begins with the deserialization of the raw data
 * byte[] to an associative map. The command is then dereferenced. If the command
 * is not a "MSG" command then the socket connection and raw data is sent to the
 * local <code>WaitingRoomWorker</code> thread for processing and handling. 
 * Otherwise the message is sent to be multiplexed and ultimately echoed back,
 * by the selector thread, to all the clients connected to the 
 * <code>ChatRoom</code> to which this worker thread is attached. This worker 
 * thread will continue processing and handling <code>ChatDataEvents</code> until
 * this server is shutdown. 
 * 
 * @author Ben Miller
 * @version 1.0
 */
public class ChatWorker implements Runnable 
{
/*------------------------------ PRIATE MEMBER DECLARATION -------------------*/
    // Private Data Constants 
    private final String CMD = DataSerializer.CMD;
    private final String MSG = DataSerializer.MSG;
    
    // Create a queue that will hold writing (echo) jobs
    private List<ChatDataEvent> queue = new LinkedList<>();
/*----------------------------------------------------------------------------*/
    
    // Constructor 
    public ChatWorker(){}

    /* Method to process the message data and enqueue it into the list, so 
     * that they can be handled by the selector thread (the main thread). */
    public void processData (ChatRoom ct, SocketChannel sc, byte[] data, int count)
    {
        /* Create a byte array to hold message data.*/
        byte[] filteredReadData = new byte[count];

        /* Copy the original data payload into another array, only copying the 
         * bytes that were just read into the buffer */
        System.arraycopy(data, 0, filteredReadData, 0, count);
        
        // Deserialize the data array passed converting into a map, 'associative array'
        Map <String, String> postData = DataSerializer.deserializeData(filteredReadData);
        
        // Create sychronized lock
        synchronized (queue)
        {
            // Add a new ChatDataEvent to the queue 
            queue.add(new ChatDataEvent 
            (
                ct, sc, postData.get(MSG), filteredReadData
            ));

            // Notify the queue that there are new events 
            queue.notify();
        }
    }

    /* Method overriden that defines what this thread will do when its 
     * started. This thread will monitor the queue of ServerEvents. When a  
     * new server event is enqueued (via the process data method) then 
     * this thread will take that new serverEvent and call it's send 
     * method, to send the data back to the client. */
    @Override
    public void run() 
    {
        // Local Variable Declaration 
        ChatDataEvent dataEvent; 

        /* Start event loop taht will continually try to process worker 
         * events */
        while (true)
        {
            // Block until data is available 
            synchronized (queue)
            {
                // Force the execution to pause until the queue has an item
                while (queue.isEmpty())
                {
                    try
                    {
                        // force the queue to wait until it's notified
                        queue.wait();
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                // Pop off the last ChatDataEvent from the event queue
                dataEvent = (ChatDataEvent) queue.remove(0);
            }

            // Check to see if the client issued a 'MSG' command
            if (dataEvent.getMessage() != null)
            {
                /* Send the data back to the client by calling the multiplex method 
                 * in the ChatRoom class */
                dataEvent.getChatThread().multiplex
                ( 
                    dataEvent.getSocket(), dataEvent.getMessage().getBytes() 
                );
            }
            else 
            {
                // Issue the command to the waiting room worker for handling 
                dataEvent.getChatThread().doWaitingRoomTask
                (
                        dataEvent.getSocket(), dataEvent.getCommandString()
                );
            }
        }
    }
}
