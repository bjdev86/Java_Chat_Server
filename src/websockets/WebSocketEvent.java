
package websockets;

import java.nio.channels.SelectionKey;

/**
 *
 * @author Ben
 */
public class WebSocketEvent 
{
 // Local Variable Declaration 
    private SelectionKey clientKey;
    private byte[] frame, payload;
    private String directive = "";
    
    public WebSocketEvent(SelectionKey clientKey, byte[] frame, byte[] payload, String dir) 
    {
        this.clientKey = clientKey; 
        this.frame = frame;
        this.payload = payload; 
        this.directive = dir; 
    }
    
    // Getters
    public SelectionKey getClientKey()
    {
        return this.clientKey;
    }
    
    public byte[] getFrame()
    {
        return this.frame;
    }
    
    public byte[] getPayload()
    {
        return this.payload;
    }    
    public String getDirective ()
    {
        return this.directive;
    }
}
