
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
public abstract class AbstractWebSocketAPI<T> 
{
/*-------------------------- Protected Data Members --------------------------*/
    // Map to track sockets that connect to this server 
    protected  Map<T, WebSocketData> sockets = new ConcurrentHashMap<>();
    
    // Handshake header string 
    protected final String HND_SHK_HDR =
                        "HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: ";
    
    // Bad Request Header 
    protected final String BAD_RQST_HDR =
            "HTTP/1.1 400 Bad Request\r\n"
            + "Descrition: ";
    
    // Magic string used to decode the Web-Socket key sent by the connecting client
    protected String magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
/*----------------------------------------------------------------------------*/    
    // Abstract Methods
    protected abstract void connect(T clientConnection);
    protected abstract void enFrame(byte[] payload, T clientConnection, 
                                    WebsocketStringDataHandler onFramed );
    protected abstract void deFrame(byte[] frame, T clientConnection,
                                    WebsocketStringDataHandler strHandlr, 
                                    WebsocketByteDataHandler byteHandlr ) 
        throws Exception;
    protected abstract void unsupportedDirective(T clientConnection, String directive);
/*----------------------------------------------------------------------------*/
   
}
