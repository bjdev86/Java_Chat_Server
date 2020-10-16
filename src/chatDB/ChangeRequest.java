
package chatDB;

import java.nio.channels.SocketChannel;

/**
 * Class to define a Data Access Object to encapsulate data related to a 
 * <code>ChangeRequest</code>. A <code>ChangeRequest</code> is issued when a 
 * worker thread or a selector thread wants to change the current interest 
 * operation for a certain <code>SelectionrKey</code>. This change would occur
 * for example when a certain key's interest op is set for read readiness
 * and the interested thread wants to check for write readiness on this same key.
 * The interested thread would issue a new <code>ChangeRequest</code>, and that 
 * request will be honored by the selector thread's main event loop, where the 
 * key's interest op will be changed.
 * <br><br>
 * <code>ChangeRequest</code>s are added to a list of <code>ChangeRequest</code>s
 * So that multiple requests can be handled for multiple keys.
 * <br><br>
 * This DAO's public members consist of a handle to the  
 * <code>SocketChannel</code> for which the <code>SelectionKey</code> who's 
 * interest op is being changed. The type of change this request is asking to   
 * make wether that be to REGISTER for a new socket connection, or the CHANGEOPS 
 * the interest op for the key held by the <code>SocketChannel</code> that is   
 * apart of this <code>ChangeRequest</code>. The new interest op that will be   
 * set on the socket's key when this <code>ChangeRequest</code> is handled in  
 * the event loop of a selector thread. 
 * 
 * @author Ben Miller
 * @version 1.0
 */
public class ChangeRequest
{
    // Public flags
    public static final int REGISTER = 1; 
    public static final int CHANGEOPS = 2;

    // Private Data Members
    private SocketChannel socket; 
    private int type; 
    private int ops; 

    /**
     * 
     * @param socket
     * @param type
     * @param ops 
     */
    public ChangeRequest (SocketChannel socket, int type, int ops)
    {
        this.socket = socket; 
        this.type = type; 
        this.ops = ops;
    }

    // Private Data Member Getters
    public SocketChannel getSocketChannel()
    {
        return this.socket; 
    }

    public int getType() 
    { 
        return this.type; 
    } 

    public int getOps() 
    { 
        return this.ops; 
    }
}
