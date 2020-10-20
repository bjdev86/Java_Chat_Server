/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websockets;

import chatDB.RecptionRoom;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Ben
 */
public class WebSocketImp 
{
    // Byte buffer to hold header data exchanged during connection handshake
    private static ByteBuffer headerBuff = ByteBuffer.allocate(600); // DIRECT???
    
    /**
     * Staging method for finishing up a connection that has been accepted by
     * this server. The method parse the headers sent by the client, validate 
     * them and then complete the TCP handshake that will establish the 
     * connection with the server. 
     * 
     * @param clientKey The SelectionKey associated with the web socket trying
     *        to connect to this server. 
     */
    public static void connect(SelectionKey clientKey)
    {
        // Local Variable Declaration 
        Map<String, String> headers = new HashMap<>(); 
        String headerString = "";
        
        // Read headers from socket client
        headerString = receiveHeaders(clientKey);
        
        // Parse and validate the headers if the headerString could be read
        if(!headerString.equals(null))
        {
            headers = parseAndValidateHeaders(headerString);
        }
            
        // Make sure the header contained the GET method 
        if (headers.containsKey("GET") && headers.containsKey("Sec-WebSocket-Key"))
        {
            // Complete handshake, by sending response header confirming connection terms
            finishConnection(headers, clientKey);
        }
        else
        {
            // Send a Bad Request HTTP response 
            SocketChannel sc = (SocketChannel) clientKey.channel();
            
            try 
            {
                byte rsp[] = ("\"HTTP/1.1 400 Bad Request\\r\\n" 
                           +  "Description: Missing GET Method or Sec-WebSocket-Key"
                           +  "\\r\\n").getBytes("UTF-8");
            
                sc.write(ByteBuffer.wrap(rsp));
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(RecptionRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void enFrame(){}
    public static void deFrame(){}
    
    public static void unsupportedDirective(SelectionKey clientKey, String directive)
    {
        
    }
/*----------------------------------------------------------------------------*/
    /**
     * Method to read in the raw headers from the socket client. 
     * 
     * @param clientKey The key representing the client connection in this 
     *        selector thread's selector.
     * 
     * @return The raw serialized string representation of the client socket 
     *         headers
     */
    private static String receiveHeaders (SelectionKey clientKey)
    {
        // Local Variable Declaration 
        int bytesRead = 0; byte[] trimedHeader, rsp;
        String rawHeaders = null; SocketChannel clientSC; 
        
        try 
        {
            // Get the socket channel from the key passed 
            clientSC = (SocketChannel)clientKey.channel();

            // Clear the buffer before each read
            headerBuff.clear();

            // Read the header in from the socket channel
            bytesRead = clientSC.read(headerBuff);

            // Initialize the header byte array to the amount of bytes read
            trimedHeader = new byte[bytesRead];

            // Trim off any white space 
            System.arraycopy(headerBuff.array(), 0, trimedHeader, 0, bytesRead);

            // Convert bytes to String for parsing 
            rawHeaders = new String(trimedHeader, Charset.forName("UTF-8"));

            // Trim trailing whitespace 
            rawHeaders = rawHeaders.trim();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        finally
        {
            return rawHeaders;
        }
    }
    
    /**
     * Method to parse the serialized headers sent by the client. The method 
     * also validates them against the standards found in 
     * {@link #HeaderStandards HeaderStandards}. If the standard doesn't exist 
     * in the mapping then no entry is made. If the value is invalid then the 
     * empty String is mapped to the valid standard name. 
     * 
     * @param serialHeaders The raw serialized header String.
     * 
     * @return The mapping of header keys to header values as extracted from the
     *         serialHeaders string. 
     */
    private static Map<String, String> parseAndValidateHeaders(String serialHeaders)
    {
        // Local Variable Declaration 
        Stream<String> headerEntries; 
        Map<String, String> validatedHeaders = new HashMap<>();
        class Wrapper { String[] headerKV; }; Wrapper accessor = new Wrapper();     
        
        // Split the strings over the natural lines or the request header
        headerEntries = serialHeaders.lines();
        
        // Loop through each line
        headerEntries.forEach((entry) ->
        {
            // Split each line along thier natural delimiters
            accessor.headerKV = entry.split("( \\/ |: )");
            
            // Make sure a key value pair was just parsed
            if (accessor.headerKV.length == 2)
            {
                // Check in with the validation standard for each entry
                if (HeaderStandards.checkStandard(accessor.headerKV[0]))
                {
                    /* Determine the header value based on wether the value is 
                     * valid according to the standard. Wildcards (*) allowed */
                    String headerValue = 
                        HeaderStandards.check(accessor.headerKV[0], accessor.headerKV[1]) || 
                        HeaderStandards.check(accessor.headerKV[0], HeaderStandards.WLD_CRD)
                            ? accessor.headerKV[1] : "";
                    
                    // Put the value with it's header in the map
                    validatedHeaders.put(accessor.headerKV[0], headerValue);
                }
            }
        });
        
        return validatedHeaders;
    }
    
    /**
     * Method to complete the hand shake process between this server and a web
     * socket client. Sends back the response header
     * 
     * @param headers Mapping of header keys (names) and header values.
     * 
     * @param clientKey The SelectionKey that represents the client socket 
     *        trying to connect
     */
    private static void finishConnection(Map<String, String> headers, SelectionKey clientKey)
    {
        // Local Variable Declaration
        String SWSKey, magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        byte[] rsp; 
        SocketChannel sc; 
        
        try 
        {
            // Get the socketchannel from the key representing the connecting client 
            sc = (SocketChannel) clientKey.channel();
            
            // Get the Sec-WebSocket-Key
            SWSKey = headers.get("Sec-WebSocket-Key");

            // Build response header to send to client to complete handshake
            rsp = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(MessageDigest
                .getInstance("SHA-1").digest((SWSKey + magicString)
                .getBytes("UTF-8"))) + "\r\n\r\n").getBytes("UTF-8");

            // Write the response header back to the connection client. That's it!
            sc.write(ByteBuffer.wrap(rsp));
         
            /* That's it!!! The response has been sent and the connection is 
             * established change the interest op set of the connection to read
             * so that data frames can be recieved.*/
            clientKey.interestOps(SelectionKey.OP_READ);
            
            // Wake up the selector, so that the connection can be do I/O 
            clientKey.selector().wakeup();
        } 
        catch (IOException ex) 
        {
            // Send HTTP error status code 
                        
            ex.printStackTrace();
        } 
        catch (NoSuchAlgorithmException ex) 
        {
            Logger.getLogger(RecptionRoom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
