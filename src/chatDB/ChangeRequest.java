
package chatDB;

import java.nio.channels.SocketChannel;

/**
 *
 * @author Ben
 */
public class ChangeRequest
{
    // Public flags
        public static final int REGISTER = 1; 
        public static final int CHANGEOPS = 2;
        
        private SocketChannel socket; 
        private int type; 
        private int ops; 

        public ChangeRequest (SocketChannel socket, int type, int ops)
        {
            this.socket = socket; 
            this.type = type; 
            this.ops = ops;
        }

        // Private Data Member Getters
        public SocketChannel getSocketChannel() { return this.socket; }
        public int getType() { return this.type; } 
        public int getOps() { return this.ops; }
}
