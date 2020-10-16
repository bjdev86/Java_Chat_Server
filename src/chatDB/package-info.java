/**
 * Package to define a websocket server that facilitate chat threads between 
 * multiple clients. Chats between 2 or more clients are supported. Currently 
 * the chat threads are ephemeral, and can't be accessed at a later date. This 
 * state will be altered in the future when the goal of adding database support 
 * through MongoDB is realized. 
 * <br><br>
 * This server is efficiently threaded to help break important tasks and ensure
 * the smooth transmission of messages between chat clients. Those threads are
 * broken up into selector threads and worker threads. Selector threads are
 * responsible for the direct interaction with the client. They handle 
 * new connection accept events, read events and write events to and from the
 * client. The selector thread achieves this by maintaining a <code>Slector</code>
 * which contains all the <code> SocketChannel </code> connections. Working 
 * together with native APIs the selector 'listens' for events (accept, read,
 * write), the selector handles those events directly, handing off data 
 * processing to worker threads. 
 * <br><br>
 * The data processing protocol expects data to be serialized as a byte string
 * (<code>byte[]</code>). The string is expected to contain commands and the 
 * data necessary to carry out those commands. Each selector thread's worker
 * threads are responsible for deserializing the data and transforming it into 
 * an associative mapping. The command is then accessed and process using the 
 * data passed through the socket channel. To achieve deserialization the worker
 * thread will use the <code>DataSerializer</code> class. This class also defines
 * the commands and the error codes used by this server. 
 * <br><br>
 * The overall structure of the server thus is a series of selector threads 
 * through which a <code>SocketChannel</code> will move. At each selector thread
 * stage the selector thread will allow the client to send a command along with
 * the data to be processed by that command, and will then take action based on
 * that command's execution. The socket channel may be handed off to another 
 * selector thread stage, it may just wait in the current selector thread stage
 * or it may be disconnected from server altogether. The connection can be moved
 * bi-directionally through all the selector thread stages. This allows for 
 * multiple stages with a predictable outcome, or route for a connection as it 
 * works its way through the server. In this particular server's case the 
 * connection is routed through the "log-in" selector to the "waiting room" 
 * selector and finally to the "chat thread" selector. The connection is ushered 
 * back to the waiting room selector thread when the client issues the "log-off"
 * command. 
 * <br><br>
 * @author Ben Miller 
 * @version 1.0
 */
package chatDB;
