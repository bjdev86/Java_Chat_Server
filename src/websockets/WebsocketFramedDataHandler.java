
package websockets;

/**
 *
 * @author Ben Miller
 */
public interface WebsocketFramedDataHandler 
{
    public void onFramed(byte frames[][]);
}
