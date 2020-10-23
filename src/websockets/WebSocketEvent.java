
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
    private WebsocketStringDataHandler stringHandler;
    private WebsocketByteDataHandler byteHandler;
    
    /**
     * Class constructor to set all of the members of this class all at once
     * 
     * @param clientKey The key representing the socket involved in this 
     *                  {@code WebSocketEvent}.
     * @param frame The byte array representing a message frame sent by the client
     * @param payload The raw data to be framed and then sent back to the client
     * @param dir The directive to be executed by the {@linkplain WebSocketWorker} 
     * @param handler A lambda function used to handle frame complete and 
     *        payload ready events.
     * @param clientName The name of the client, used to track the socket.
     */
    public WebSocketEvent(SelectionKey clientKey, byte[] frame, byte[] payload,
        String dir, WebsocketStringDataHandler strHndlr, WebsocketByteDataHandler byteHndlr)
    {
        this.clientKey = clientKey; 
        this.frame = frame;
        this.payload = payload; 
        this.directive = dir; 
        this.stringHandler = strHndlr;
        this.byteHandler = byteHndlr;
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
    public WebsocketStringDataHandler getStringHandler ()
    {
        return this.stringHandler;
    }

    WebsocketByteDataHandler getByteHandler() 
    {
        return this.byteHandler;
    }
}
