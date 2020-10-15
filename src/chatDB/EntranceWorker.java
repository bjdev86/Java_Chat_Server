/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Ben
 */
public class EntranceWorker extends Thread 
{
/*---------------------------- PRIVATE DATA MEMBERS --------------------------*/
    // Private delimitor constants for parsing data strings
    private final String LOG_OFF = DataSerializer.LOG_OFF;     
    private final String LOG_IN  = DataSerializer.LOG_IN; 
    private final String SGN_UP  = DataSerializer.SGN_UP; 
    
    private final String ENTRY_DELM = DataSerializer.ENTRY_DELM;
    private final String KV_DELM    = DataSerializer.KV_DELM;
    
    private final String CMD    = DataSerializer.CMD;
    private final String UNAME  = DataSerializer.UNAME;
    private final String PSSWRD = DataSerializer.PSSWRD;
    private final String FNAME  = DataSerializer.FNAME;
    private final String LNAME  = DataSerializer.LNAME;
    
    // Queue of WaitRoomEvents to process 
    private List<EntranceEvent> queue = new LinkedList<>();
/*----------------------------------------------------------------------------*/
    
    /* Method to process the command data and enqueue it into the list, so 
     * that they can be handled by the selector thread (the waiting room thread). 
     */
    public void processData (ChatServerDB serverThrd, SocketChannel soc, byte data[], int count)
    {
        // Local Variable Declaration 
        byte[] filteredReadBuff = null;
        
        // Instansiate the sub buffer array with the amount of bytes read
        filteredReadBuff = new byte[count];

        /* Get the subset of the read buffer that represents the data just read, 
         * filtering out data in the buffer from prior reads */
         System.arraycopy(data, 0, filteredReadBuff, 0, count);
         
        // Deserialize the data array passed converting into a map, 'associative array'
        Map <String, String> postData = DataSerializer.deserializeData(filteredReadBuff);

        /* Create sychronized lock over the queue so that it can't be changed 
         * while a new waitRoomEvent is being added */
        synchronized (queue)
        {
            // Add a new ChatDataEvent to the queue 
            queue.add(new EntranceEvent
            (
                serverThrd, soc, 
                ((postData.get(CMD)).trim()), 
                postData
            ));

            // Notify the queue that there are new events 
            queue.notify();
        }
    }

    /* Method overriden that defines what this thread will do when its 
     * started. This thread will monitor the queue of WaitRoomEvents. When a  
     * new waiting room event is enqueued (via the process data method) this 
     * thread will be awakened and will  then call the WaitingRoomEvent's 
     * parseCommand method. This method will handle the WaitingRoomEvent in the
     * selector thread. */
    @Override
    public void run() 
    {
        // Local Variable Declaration 
        EntranceEvent entEvent; 

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
                        // Force the queue to wait until it's notified
                        queue.wait();
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                // Pop off the last WaitRoomEvent from the event queue
                entEvent = (EntranceEvent) queue.remove(0);
            }

            /* Send the command back to the command parser to be executed in the 
             * waiting room thread. */
            this.executeCommand(entEvent.getChatServer(), entEvent.getSocket(),  
                                entEvent.getCommand(), entEvent.getVariables());
        }
    }    
    
    private void executeCommand(ChatServerDB server, SocketChannel socket, 
                                String command, Map<String, String> params) 
    {
        // Switch the command byte flag
        switch( command )
        {
            case LOG_IN:
            {
                server.login(socket, params.get(UNAME), params.get(PSSWRD));
                break;
            }
            case SGN_UP:
            {
                // Jump to the server class to preform the new user registration
                server.register(socket, params.get(UNAME), params.get(PSSWRD), 
                                        params.get(FNAME), params.get(LNAME));
                break;
            }
//            case LOG_OFF: 
//            {
//                break;
//            }
            default:
            {
                // Unsported command exception 
                
                break;
            }
        }
    }
}
