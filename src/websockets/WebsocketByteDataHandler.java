
package websockets;

/**
 * Functional interface to define the lambda signature for passing handler 
 * methods to the {@link #deFrame deFrame} and {@link #enFrame enFrame} methods
 * implemented by {@link #websockets.WebSocketImp WebSocketImp}. 
 * @author Ben
 */
public interface WebsocketByteDataHandler
{
    public void handleByteData(byte[] data);   
}
