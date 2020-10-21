
package websockets;

import java.net.Socket;

/**
 *
 * @author Ben
 */
public class WebSocketAPI extends WebSocketImplementation<Socket>
{

    @Override
    protected void connect(Socket clientConnection) 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void enFrame(byte[] payload, Socket clientConnection, WebsocketDataHandler onFramed)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void deFrame(byte[] frame, Socket clientConnection, WebsocketDataHandler wdh) throws Exception 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void unsupportedDirective(Socket clientConnection, String directive) 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
