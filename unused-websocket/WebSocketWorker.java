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
    public static final AbstractWebSocketAPI websocAPI = new WebSocketSelectionKeyAPI();
/*----------------------------------------------------------------------------*/
    
    // Class Constructor
    public WebSocketWorker (AbstractWebSocketAPI api)
    {
        // Instansiate the web socket implementation for use in this class
        //websocAPI = api;
    }
    
    /**
     * Method to handle connection jobs.This method will begin the work of 
     * transforming a TCP socket to a Websocket. 
     * 
     * @param clientKey The key representing the client TCP socket that will be
     *        upgraded to a Websocket.
     * @param onConnection Hanlder invoked when a successful connection is made
     */
    public void connect (SelectionKey clientKey /*WebsocketConnectionHandler onConnection*/)
    {
        this.createEvent(clientKey, null, null, CONCT, null, null);
    }
    
    /**
     * Method to parse an incoming frame from the Websocket. The method takes 
 in a lambda expression that handles the parsed payload data from the the
 frame 
     * 
     * @param frame The frame (byte array) to be parsed (DEFRAMED)
     * @param clientKey The {@code SelectionKey} associated with the socket over 
     *        which this frame was sent.
     * @param frameSize The amount of bytes read in for this frame. 
     *        Frame size in bytes.
     * @param strHndlr The handler responsible for processing the data 
     *        payload contained in this frame.
     * @param byteHndlr Hanlder used to deal with bytes parsed from the websocket
     *        frame.
     */
    public void parseFrame(byte[] frame, SelectionKey clientKey, int frameSize, 
            WebsocketStringDataHandler strHndlr, WebsocketByteDataHandler byteHndlr)
    {
        // Local Variable Declaratin 
        byte trimedFrame[] = new byte[frameSize];
        
        // Trim the frame down to what was actually read in on the socket
        System.arraycopy(frame, 0, trimedFrame, 0, frameSize);
        
        // Create a websocket event job to handle this frame parse
        this.createEvent(clientKey, trimedFrame, null, DEFRAME, strHndlr, byteHndlr);
    }
    
    /**
     * Method to frame up a data string (byte array) into one or more valid web
     * socket frames. A handler is passed to direct what happens to those frames
     * after they are made. Presumably the frames will be sent down the TCP 
     * socket to the client. 
     * 
     * @param data The byte array to be framed in one or more frames
     * @param frameSize The desired size of the payload per frame. This will be 
     *        the divisor over which data will be split into frames. 
     * @param framesHndlr The handler that will be executed after the frames 
     *        have been created. 
     */
    public void frameData (byte[] data, int frameSize, WebsocketFramedDataHandler framesHndlr)
    {
        this.createDataFramingEvent(data, frameSize, ENFRAME, framesHndlr);
    }
/*----------------------------------------------------------------------------*/ 
    
    /**
     * Method to create a WebsocketWorker job, that will be executed by this 
     * thread.
     * 
     * @param clientKey key that represents the client being connected.
     * @param frame The frame to be parsed and have it's payload returned.
     * @param payload Payload to be enframed to be sent down a web socket. 
     * @param dir  The directive that this job should fulfill.
     * @param clientName The name by which this socket will be tracked
     */
    private void createEvent(SelectionKey clientKey, byte[] frame, byte[] payload, 
                 String dir, WebsocketStringDataHandler strHndlr,
                 WebsocketByteDataHandler byteHndlr)
    {
        /* Create a connection job by creating and adding a WebSocketEvent to 
         * the queue. Create sychronized lock over the queue so that inter-
         * leaving synchronicity is maintain. */
        synchronized (queue)
        {
            // Add a new ChatDataEvent to the queue 
            queue.add(new WebSocketEvent (clientKey, frame, payload, dir, strHndlr, byteHndlr));

            // Notify the queue that there are new events 
            queue.notify();
        }
    }
    
    /**
     * Method to create a {@code WebsocketEvent} that will hold the data 
     * necessary to complete the framing of the data passed into a valid 
     * web socket frame. This method will create one or more frames based on the
     * desired frame payload size.
     * 
     * @param data Array of bytes to use as the payload of the frame(s)
     * @param frameSize The desired size of the payload data per frame.
     * @param directive The name of the directive this {@code WebSocketEvent} represents.
     * @param framesHndlr Handles the frames after they are created.
     */
    private void createDataFramingEvent(byte[] data, int frameSize, String directive,
                                        WebsocketFramedDataHandler framesHndlr) 
    {
        /* Create a concurency lock over the queue, so that the adding of a new 
         * WebsocketEvent will interleave. The WebsocketEvent will provide 
         * access to the data required to frame up the data passed. */
        synchronized (queue)
        {
            // Add a new WebsocketEvent to the queue to be handled
            queue.add(new WebSocketEvent(data, (byte) 1, frameSize, directive, framesHndlr));
            
            // Notify the worker thread that the queue has been updated
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
            this.executeCommand(wbsEvent.getDirective(), wbsEvent);
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
     * @param strHndlr The handler to either process parsed frame payload data or 
     *        enframed payload data.
     */
    private void executeCommand(String directive, WebSocketEvent event) 
    {
        // Switch the directive string
        switch( directive )
        {
            case CONCT:
            {
                // Finish the connection 
                this.websocAPI.connect(event.getClientKey());                
                break;
            }
            case DEFRAME:
            {
                try 
                {
                    // Deframe or parse the frame that came in.
                    this.websocAPI.deFrame(event.getFrame(), event.getClientKey(), 
                                           event.getStringHandler(), 
                                           event.getByteHandler());
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
                // Frame the data array (and then send it down the wire) 
                this.websocAPI.enFrame(event.getPayload(), event.getOpcode(), 
                                       event.getFrameSize(), 
                                       event.getFramedHandler());                
                break;
            }
            case DSCONCT:
            {
                break; 
            }
            default:
            {
                // Unsported command exception 
                this.websocAPI.unsupportedDirective(event.getClientKey(), event.getDirective());
                
                break;
            }
        }
    }   
}
