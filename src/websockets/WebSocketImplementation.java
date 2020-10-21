
package websockets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Ben
 * @param <T> The type of socket either a {@code Socket} or {@code SelectionKey}
 *        all other types of sockets or socket wrappers are not supported and 
 *        will cause a {@link #UnsupportedTypeException UnsupportedTypeException}
 *        to be thrown.
 * @TODO learn type checking so that we can implement the common private methods
 *       in each implementation of this abstract class, so that we don't have to 
 *       copy and paste private method to each implementation.
 */
public abstract class WebSocketImplementation<T> 
{
    // Data Members 
    protected  Map<T, WebSocket> sockets = new ConcurrentHashMap<>();
    
/*----------------------------------------------------------------------------*/    
    // Abstract Methods
    protected abstract void connect(T clientConnection);
    protected abstract void enFrame(byte[] payload, T clientConnection, WebsocketDataHandler onFramed );
    protected abstract void deFrame(byte[] frame, T clientConnection, WebsocketDataHandler wdh ) throws Exception;
    protected abstract void unsupportedDirective(T clientConnection, String directive);
/*----------------------------------------------------------------------------*/
   
}
