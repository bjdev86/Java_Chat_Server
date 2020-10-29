
package websockets;

/**
 *
 * @author Ben
 */
public interface WebsocketClosingHandler <T> 
{
    public void onClose (T closingConnector);
}
