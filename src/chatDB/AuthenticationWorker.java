/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ben
 */
public class AuthenticationWorker extends Thread 
{
/*----------------------------- PRIVATE CONSTANTS ----------------------------*/
    // Byte flag to indicate the username is incorrect 
    private final byte USR_NM_WRG = (byte) 1; 
    private final byte PSSWRD_WRG = (byte) 2; 
    private final byte BOTH_WRG   = (byte) 3; 
/*----------------------------------------------------------------------------*/
/*---------------------------- PRIVATE DATA MEMBERS --------------------------*/
    // Queue of WaitRoomEvents to process 
    private List<AuthenticationEvent> queue = new LinkedList<>();
    
    private Map <String, String> users = new HashMap<>();
/*----------------------------------------------------------------------------*/
    
    public AuthenticationWorker ()
    {
        this.users.put("admin", "password");
    }
    
    /* Method to process the connection and enqueue it into the list, so 
     * that they can be handled by the selector thread (the waiting room thread). 
     */
    public void processConnection (ChatServerDB serverThread, SocketChannel soc)
    {        
        // Make sure the connection is non-blocking
        
        /* Create sychronized lock over the queue so that it can't be changed 
         * while a new waitRoomEvent is being added */
        synchronized (queue)
        {
            // Add a new AuthenticationEvent to the queue 
            queue.add(new AuthenticationEvent(serverThread, soc));

            // Notify the queue that there are new events 
            queue.notify();
        }
    }

    /**
     * Method to authenticate the credential String passed against the credentials 
     * on file. 
     * 
     * @param credentialString 
     */
    private byte authenticate(String credentialString) 
    {
        // Local Variable Declaration 
        String clUsrName = ""; String clPssWrd = ""; 
        String dbUsrName = ""; String dbPssWrd = "";
        byte resultFlag = 0b00000000;
        
        //Parse the credentials over the comma delemiter 
        String[] credentials = credentialString.split(",");
        
        // Get the user name and password parsed from the string passed
        clUsrName = credentials[0]; clPssWrd = credentials[1];
        
        // Get the user name and password from the database (USE BUILT-IN Object for now)
        dbPssWrd = this.users.get(clUsrName);
        
        // Check to see if the password is null 
        if (dbPssWrd == null)
        {
            // Client name was wrong, set the flag
            resultFlag |= 0b00000001;
        }
        
        // Check to make sure the password is correct
        if (dbPssWrd != clPssWrd)
        {
            // Client password passed was incorrect set the byte flag
            resultFlag |= 0b00000010;
        }
    
        return resultFlag;
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
        AuthenticationEvent authEvent; 
        ByteBuffer buff = ByteBuffer.allocate(100);
        byte authenticationFlag = (byte) 0;
        
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
                authEvent = (AuthenticationEvent) queue.remove(0);
            }

            try
            {
                /* Attempt to read data from socket channel associated with the 
                 * latest AuthenticationEvent */
                if (authEvent.getSocketChannel().read(buff) > 0)
                {
                    /* If there was data to read in from the channel try to 
                     * authenticate it.*/
                    authenticationFlag = this.authenticate(new String(buff.array()));
                    
                    // Test the authentication flag
                    if (authenticationFlag != 0b00000000)
                    {
                        // If both the user name and password are valid let the client know
                        //authEvent.getServer().isAuthentic(authEvent.getSocketChannel());
                    }
                    else 
                    {
                        /* If both credentials are invalid pass the byte flag 
                         * along to identify which one is wrong */
                        //authEvent.getServer().isInAuthentic(authEvent
                        //         .getSocketChannel(), authenticationFlag);
                    }
                }
                else 
                {
                    /* Put the AuthenticationEvent back in the queue to be proccessed
                     * later as there was not data to read from the client on this 
                     * pass */
                    this.queue.add(authEvent);
                }
                
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }    

    

}
