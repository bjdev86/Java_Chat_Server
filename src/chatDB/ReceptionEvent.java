package chatDB;

import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * Class to define an event object that will contain all the data that is used 
 * to define a <code>ReceptionEvent</code>. A <code>ReceptionEvent</code> is an 
 * event that occurs when the reception selector thread sends data to the 
 * <code>ReceptionWorker</code> for processing. This event will encapsulate a 
 * new log-in or registration job request. This event's data members include,
 * The <code>ReceptionRoom</code> selector thread, the socket over which the 
 * raw data was received by the <code>ReceptionRoom</code>, and the byte[] data
 * string containing the command and command dependent data. 
 * 
 * @author Ben Miller
 * @version 1.0
 */
class ReceptionEvent
{
    // Local Variable Declaration 
    private RecptionRoom selectorThread;
    private SocketChannel socket;
    private String command; 
    private Map <String, String> variables;
    
    public ReceptionEvent(RecptionRoom chatServer, SocketChannel sc, String command, 
                         Map<String, String> vars) 
    {
        this.selectorThread = chatServer;
        this.socket = sc; 
        this.command = command;
        this.variables = vars; 
    }
    
    // Getters 
    public RecptionRoom getSelectorThread()
    {
        return this.selectorThread;
    }
    
    public SocketChannel getSocket()
    {
        return this.socket;
    }
    
    public String getCommand()
    {
        return this.command;
    }
    
    public Map<String, String> getVariables()
    {
        return this.variables;
    }
}
