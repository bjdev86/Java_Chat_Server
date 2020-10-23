
package websockets;

import java.net.Socket;

/**
 *
 * @author Ben
 */
public class WebSocketSocketAPI extends AbstractWebSocketAPI<Socket>
{

    @Override
    protected void connect(Socket clientConnection) 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void enFrame(byte[] payload, Socket clientConnection, WebsocketStringDataHandler onFramed)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void deFrame(byte[] frame, Socket clientConnection, WebsocketStringDataHandler stringHandler, WebsocketByteDataHandler byteHandler) throws Exception 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void unsupportedDirective(Socket clientConnection, String directive) 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
