
package chatDB;

import java.nio.channels.SocketChannel;

/**
 * Class to define an event object that will contain all the data that is used
 * to represent a <code>ChatDataEvent</code>. This event includes the following 
 * data members, a handle to the ChatRoom selector thread that created this 
 * event; the socket connection over which the data for this event was sent, and
 * the raw byte[] data string containing the command and command dependent data.
 * This event is created by the <code>ChatRoom</code> selector thread. This 
 * event is handled by the <code>ChatWorker</code> worker thread.
 * 
 * @author Ben
 */
public class ChatDataEvent 
{
    // Local Variable Declaration 
    private ChatRoom chatThread;
    private SocketChannel socket;
    private String message;;
    private byte[] cmdString; 

    // Constructor
    public ChatDataEvent(ChatRoom ct, SocketChannel socket, String mssg, byte[] cmdString) 
    {
        this.chatThread = ct;
        this.socket = socket;
        this.message = mssg;
        this.cmdString = cmdString;
    }
    
    // Getters
    public ChatRoom getChatThread ()
    {
        return this.chatThread;
    }
    
    public SocketChannel getSocket()
    {
        return this.socket; 
    }
    
    public String getMessage()
    {
        return this.message;
    }
    
    public byte[] getCommandString()
    {
        return this.cmdString;
    }
    
}
