/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websockets;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final String DSCONCT = "DSCONCT";
        
    // Queue of WaitRoomEvents to process 
    private List<WebSocketEvent> queue = new LinkedList<>();
    
    // Handle to WebSocketImpl object which implements the actions taken by this worker
    private WebSocketChannelAPI websocAPI;
/*----------------------------------------------------------------------------*/
    
    // Class Constructor
    public WebSocketWorker ()
    {
        // Instansiate the web socket implementation for use in this class
        websocAPI = new WebSocketChannelAPI();
    }
    
    /**
     * Method to handle connection jobs.This method will begin the work of 
     * transforming a TCP socket to a Websocket. 
     * 
     * @param clientKey The key representing the client TCP socket that will be
     *        upgraded to a Websocket.
     * @param rspHandler
     * @param clientName
     */
    public void connect (SelectionKey clientKey, WebsocketDataHandler rspHandler)
    {
        this.createJob(clientKey, null, null, CONCT, rspHandler);
    }
    
    /**
     * Method to parse an incoming frame from the Websocket. The method takes 
     * in a lambda expression that handles the parsed payload data from the the
     * frame 
     * 
     * @param frame The frame (byte array) to be parsed (DEFRAMED)
     * @param clientKey The {@code SelectionKey} associated with the socket over 
     *        which this frame was sent.
     * @param frameSize The amount of bytes read in for this frame. 
     *        Frame size in bytes.
     * @param pyldhandler The handler responsible for processing the data 
     *        payload contained in this frame.
     */
    public void parseFrame(byte[] frame, SelectionKey clientKey, int frameSize, WebsocketDataHandler pyldhandler)
    {
        // Local Variable Declaratin 
        byte trimedFrame[] = new byte[frameSize];
        
        // Trim the frame down to what was actually read in on the socket
        System.arraycopy(frame, 0, trimedFrame, 0, frameSize);
        
        // Create a websocket event job to handle this frame parse
        this.createJob(clientKey, trimedFrame, null, DEFRAME, pyldhandler);
    }
     
    /**
     * Method to create a WebsocketWorker job, that will be executed by this 
     * thread.
     * 
     * @param clientKey key that represents the client being connected
     * @param frame The frame to be parsed and have it's payload returned
     * @param payload Payload to be enframed to be sent down a websocket 
     * @param dir  The directive that this job should fulfill.
     * @param clientName The name by which this socket will be tracked
     */
    private void createJob(SelectionKey clientKey, byte[] frame, byte[] payload, 
                 String dir, WebsocketDataHandler handler)
    {
        /* Create a connection job by creating and adding a WebSocketEvent to 
         * the queue. Create sychronized lock over the queue so that inter-
         * leaving synchronicity is maintain. */
        synchronized (queue)
        {
            // Add a new ChatDataEvent to the queue 
            queue.add(new WebSocketEvent (clientKey, frame, payload, dir, handler));

            // Notify the queue that there are new events 
            queue.notify();
        }
    }
/*----------------------------------------------------------------------------*/
    
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
                                wbsEvent.getPayload(), wbsEvent.getDirective(),
                                wbsEvent.getHandler());
        }
    }    
    
    /**
     * Method to execute the directives that this Websocket server has been taught
     * how to execute. 
     * 
     * @param clientKey {@code SelectionKey} associated with this client socket. 
     * @param frame The frame being parsed
     * @param payload The payloand being enframed
     * @param directive The action to take regarding the frame
     * @param clientName The name of the client socket over which the frame is 
     *        transmitted
     * @param handler The handler to either process parsed frame payload data or 
     *        enframed payload data.
     */
    private void executeCommand(SelectionKey clientKey, byte[] frame, 
        byte[]payload, String directive, WebsocketDataHandler handler) 
    {
        // Switch the directive string
        switch( directive )
        {
            case CONCT:
            {
                // Finish the connection 
                this.websocAPI.connect(clientKey);                
                break;
            }
            case DEFRAME:
            {
                try 
                {
                    // Deframe or parse the frame that came in.
                    this.websocAPI.deFrame(frame, clientKey, handler);
                } 
                catch (Exception ex) 
                {
                    // THe connection should be closed if an exception is thrown
                    ex.printStackTrace();
                    Logger.getLogger(WebSocketWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                break;
            }
            case ENFRAME:
            {
                // Finish the connection 
                this.websocAPI.enFrame(payload, clientKey, handler);                
                break;
            }
            case DSCONCT:
            {
                break; 
            }
            default:
            {
                // Unsported command exception 
                this.websocAPI.unsupportedDirective(clientKey, directive);
                
                break;
            }
        }
    }   
}
