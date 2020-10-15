/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Worker thread to echo the message that was sent from a user back to the other 
 * users connected to this chat server. 
 * 
 * @author Ben
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
     * that they can be handled by the selector thread (the main thread), 
     * where it will be written back to user. */
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
