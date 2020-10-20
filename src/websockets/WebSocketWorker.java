/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websockets;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Ben
 */
public class WebSocketWorker implements Runnable
{
 /*---------------------------- PRIVATE DATA MEMBERS --------------------------*/
    // Private String constants
    private final String CONCT   = "CONCT";
    private final String ENFRAME = "ENFRAME";
    private final String DEFRAME = "DEFRAME";
    
    // Queue of WaitRoomEvents to process 
    private List<WebSocketEvent> queue = new LinkedList<>();
/*----------------------------------------------------------------------------*/
    
    // Class Constructor
    public WebSocketWorker (){}
    
    /**
     * Method to handle connection jobs. This method will begin the work of 
     * transforming a TCP socket to a Websocket. 
     * 
     * @param clientKey The key representing the client TCP socket that will be
     *        upgraded to a Websocket.
     */
    public void connect (SelectionKey clientKey)
    {
        this.createJob(clientKey, null, null, CONCT);
    }
    
    /**
     * Method to create a WebsocketWorker job, that will be executed by this 
     * thread.
     * 
     * @param clientKey key that represents the client being connected
     * @param frame The frame to be parsed and have it's payload returned
     * @param payload Payload to be enframed to be sent down a websocket 
     * @param dir  The directive that this job should fulfill. 
     */
    private void createJob(SelectionKey clientKey, byte[] frame, byte[] payload, String dir)
    {
        /* Create a connection job by creating and adding a WebSocketEvent to 
         * the queue. Create sychronized lock over the queue so that inter-
         * leaving synchronicity is maintain. */
        synchronized (queue)
        {
            // Add a new ChatDataEvent to the queue 
            queue.add(new WebSocketEvent (clientKey, frame, payload, dir));

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
        WebSocketEvent wbsEvent; 

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
                wbsEvent = (WebSocketEvent) queue.remove(0);
            }

            /* Parse the command by directive and then execute it */
            this.executeCommand(wbsEvent.getClientKey(), wbsEvent.getFrame(),  
                                wbsEvent.getPayload(), wbsEvent.getDirective());
        }
    }    
    
    private void executeCommand(SelectionKey clientKey, byte[] frame, byte[]payload, String directive) 
    {
        // Switch the directive string
        switch( directive )
        {
            case CONCT:
            {
                // Finish the connection 
                WebSocketImp.connect(clientKey);                
                break;
            }
            case DEFRAME:
            {
                // Finish the connection 
                WebSocketImp.deFrame(clientKey);                
                break;
            }
            case ENFRAME:
            {
                // Finish the connection 
                WebSocketImp.enFrame(clientKey);                
                break;
            }
            default:
            {
                // Unsported command exception 
                WebSocketImp.unsupportedDirective(clientKey, directive);
                
                break;
            }
        }
    }   
}
