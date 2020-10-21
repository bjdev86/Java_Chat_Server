package websockets;

/**
 * Convienence interface to define the lambda signature for passing handler 
 * methods to the {@link #deFrame deFrame} and {@link #enFrame enFrame} methods
 * implemented by {@link #websockets.WebSocketImp WebSocketImp}. 
 * 
 * @author Ben
 * @version 1.0
 */
public interface WebsocketDataHandler 
{
    public void payloadHandler (byte data[]);
    //public void payloadHandler (String data);
    //public void handleFrame (byte frame[]);
}

//public interface WebsocketStringDataHandler
//{
//    public void palyloadHandler (String data);
//}