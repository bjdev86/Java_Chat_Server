
package websockets;

/**
 * Functional interface to define the lambda signature for passing handler 
 * methods to the {@link #deFrame deFrame} and {@link #enFrame enFrame} methods
 * implemented by {@link #websockets.WebSocketImp WebSocketImp}. 
 * 
 * @author Ben
 * @version 1.0
 */
public interface WebsocketStringDataHandler 
{
    public void handleStringData(String data); 
}