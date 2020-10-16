
package chatDB;

import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * Class to define an event object containing the data that represents a new 
 * <code>WaitingRoom</code> job request. This event object contains a handle to 
 * the WaitingRoom selector thread that spawned this event, the socket over 
 * which the data for this event was sent and the deserialized associative 
 * mapping of the data that is connected to this <code>WaitingRoomEvent</code>. 
 * These events are created by <code>WaitingRoom</code> selector thread and
 * handled by the <code>WaitingRoomWorker</code> thread.
 * 
 * @author Ben Miller
 * @version 1.0
 */
class WaitRoomEvent
{
    // Local Variable Declaration 
    private WaitingRoom wtrThread;
    private SocketChannel socket;
    private Map<String, String> vars;
    
    public WaitRoomEvent(WaitingRoom wrtrd, SocketChannel sc, Map vars) 
    {
        this.wtrThread = wrtrd;
        this.socket = sc; 
        this.vars = vars;
    }
    
    // Getters 
    public WaitingRoom getWaitingRoomThread()
    {
        return this.wtrThread;
    }
    
    public SocketChannel getSocket()
    {
        return this.socket;
    }
    
    public Map<String, String> getVariables()
    {
        return vars;
    }
}
