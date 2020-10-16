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
 * Class to define a worker thread used to create and handle 
 * <code>WaitingRoomEvents</code>. First the raw <code>byte[]</code> is and 
 * socket connection are added to a queue of <code>WaitingRoomEvents</code> to 
 * be processed then this thread is interrupted so that the events can be handled.
 * <br><br>
 * The handling of <code>WaitingRoomEvents</code> starts off with the 
 * deserialization of the byte string sent by the client. Next the command is 
 * dereferenced, matched and then executed. The thread continues to wait for 
 * <code>WaitingRoomEvent</code>s and processes them until this server is 
 * shutdown.
 * 
 * @author Ben Miller
 * @version 1.0
 */
public class WaitingRoomWorker extends Thread 
{
/*---------------------------- PRIVATE DATA MEMBERS --------------------------*/
    // Private delimitor constants for parsing data strings
    private final String CMD        = DataSerializer.CMD;
    private final String CHAT_NAME  = DataSerializer.CHAT_NAME;

    private final String CRT_CHT    = DataSerializer.CRT_CHT; 
    private final String DLT_CHT    = DataSerializer.DLT_CHT; 
    private final String JOIN_CHT   = DataSerializer.JOIN_CHT; 
    private final String LEAVE_CHT  = DataSerializer.LEAVE_CHT; 
    private final String SIGN_OFF   = DataSerializer.SIGN_OFF; 
    
    // Queue of WaitRoomEvents to process 
    private List<WaitRoomEvent> queue = new LinkedList<>();
/*----------------------------------------------------------------------------*/
    
    /* Method to process the command data and enqueue it into the list, so 
     * that they can be handled by the selector thread (the waiting room thread). 
     */
    public void processData (WaitingRoom wtrmThrd, SocketChannel soc, byte data[], int count)
    {
        // Local Variable Declaration 
        byte[] filteredReadBuff = null;
        
        // Instansiate the sub buffer array with the amount of bytes read
        filteredReadBuff = new byte[count];

        /* Get the subset set of the read buffer that represents the data 
         * just read */
         System.arraycopy(data, 0, filteredReadBuff, 0, count);
         
        // Deserialize the data array passed converting into a map, 'associative array'
        Map <String, String> postData = DataSerializer.deserializeData(filteredReadBuff);
        
        /* Create sychronized lock over the queue so that it can't be changed 
         * while a new waitRoomEvent is being added */
        synchronized (queue)
        {
            // Add a new ChatDataEvent to the queue 
            queue.add(new WaitRoomEvent(wtrmThrd, soc, postData));

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
        WaitRoomEvent wrEvent; 

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
                wrEvent = (WaitRoomEvent) queue.remove(0);
            }

            /* Send the command back to the command parser to be executed in the 
             * waiting room thread. */
            this.executeCommand
            (
                wrEvent.getSocket(),wrEvent.getWaitingRoomThread(),
                wrEvent.getVariables().get(CMD),
                wrEvent.getVariables()
            );                                
        }
    }    

        /**
     * Method to parse a command issued by the client to this server.
     * 
     * @param socket The SocketChannel over which the command to parse was sent.
     * 
     * @param parameters The command data
     */
    private void executeCommand(SocketChannel socket, WaitingRoom selThread, 
                                String command, Map<String, String> vars) 
    {
        // Switch the command byte flag
        switch( command )
        {
            case CRT_CHT: 
            {
                selThread.createChatThread(socket, vars.get(CHAT_NAME));
                
                break; 
            }
            case DLT_CHT:
            {
                break;
            }
            case JOIN_CHT:
            {
                // Attempt to join a chat thread in progress
                selThread.joinChatThread(vars.get(CHAT_NAME), socket);
                
                break; 
            }
            case LEAVE_CHT:
            {
                selThread.leaveChatThread(vars.get(CHAT_NAME), socket);
                
                break;
            }
            case SIGN_OFF:
            {
                selThread.logOff(socket);
                
                break;
            }
            default:
            {
                selThread.unSupportedCmd(socket, command);
                
                break; 
            }
        }
    }
}
