
package websockets;

import chatDB.RecptionRoom;
import java.io.IOException;
import java.math.BigInteger;
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
public class WebSocketChannelAPI extends WebSocketImplementation <SelectionKey>
{
    // Private Constants
    private static final String RQS_METHOD  = "RQS_METHOD";
    private static final String RQS_VERSION = "RQS_VERSION";
    private static final String RQS_STANDRD = "RQS_STANDRD";
    
    // Exceptoin strings
    public static final String NO_MASK    = "NoMaskException";
    public static final String PY_LD_LGTH = "PayloadLengthOutOfBounds";
    
    // Byte buffer to hold header data exchanged during connection handshake
    private static ByteBuffer headerBuff = ByteBuffer.allocate(600); // DIRECT???
    
    // Map to track sockets that connect to this server 
    /*private Map<SelectionKey, WebSocket> sockets = new ConcurrentHashMap<>();*/
/*----------------------------------------------------------------------------*/
    
    // Class Constructor 
    public WebSocketChannelAPI (){}

    /**
     * Staging method for finishing up a connection that has been accepted by
     * this server. The method parse the headers sent by the client, validate 
     * them and then complete the TCP handshake that will establish the 
     * connection with the server. 
     * 
     * @param clientKey The SelectionKey associated with the web socket trying
     *        to connect to this server. 
     * @param clientName The name of the client using this socket connection.
     *        This is the name by which this socket will be tracked.
     * 
     * @TODO Change the socket Map to accept Integer as keys then use a random
     *       number generator to generate keys.
     */
    @Override
    public void connect(SelectionKey clientKey)
    { 
        // Local Variable Declaration 
        Map<String, String> headers = new HashMap<>(); 
        String headerString = "", rqsMethod = "", socKey = "";
        float rqsVersion = 0;
        
        // Read headers from socket client
        headerString = receiveHeaders(clientKey);
        
        // Parse and validate the headers if the headerString could be read
        if(!headerString.equals(null))
        {
            headers = parseAndValidateHeaders(headerString);
        }
        
        // Extract the HTTP method and version used in this connection request
        rqsMethod = headers.get(RQS_METHOD);
        rqsVersion = Float.parseFloat(headers.get(RQS_VERSION));
        socKey = headers.get("Sec-WebSocket-Key");
        
        /* Make sure the header contained the GET method has a version higher 
         * 1.1 and that the socket key exists */ 
        if (rqsMethod.equals(new String("GET")) && rqsVersion >= 1.1 && socKey != null)
        {
            // Complete handshake, by sending response header confirming connection terms
            finishConnection(headers, clientKey);
            
            // Add the socket to the map of sockets by the name passed 
            this.sockets.put(clientKey, new WebSocket()); 
        }
        else
        {
            // Send a Bad Request HTTP response 
            SocketChannel sc = (SocketChannel) clientKey.channel();
            
            try 
            {
                // Build a response header for the error 
                byte rsp[] = ("\"HTTP/1.1 400 Bad Request\\r\\n" 
                           +  "Description: Missing GET Method or Sec-WebSocket-Key"
                           +  "\\r\\n").getBytes("UTF-8");
            
                // Send the response error header to the client
                sc.write(ByteBuffer.wrap(rsp));
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(RecptionRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
   @Override
    protected void enFrame(byte[] payload, SelectionKey clientConnection, WebsocketDataHandler onFramed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Method to parse a frame sent by a client. The method takes in a Lambda
     * expression ({@link #WebsocketDataHandler WebsocketDataHandler}) that will
     * handle the payload data send in the frame. The payload could be either 
     * a string of characters or bytes. 
     * 
     * @param frame The byte array containing the bytes that make up the frame
     * @param wdh The {@link #WebsocketDataHandler WebsocketDataHandler} lambda
     *        expression to handle the parsed payload.
     * @param clientKey The {@code SelectionKey} of socket over which this frame 
     *        is coming. This key is also how this connection is being tracked
     *        by this API.
     * 
     * @throws Exception If the frame's mask bit is unset or if the payload 
     *         length is out of bounds. These are fatal errors and the socket
     *         connection should be closed.
     */
    @Override
    public void deFrame(byte[] frame, SelectionKey clientKey, WebsocketDataHandler wdh ) throws Exception
    {
        // Local Variable Declaration 
        String payload = ""; 
        int dataDex = 0, unmaskedPyldByte; 
        long total = 0, payloadLength = 0;
        boolean fin = false, rsv1 = false, rsv2 = false, rsv3 = false, 
                mask = false;
        byte aByte = 0x0, opCode = 0x0, maskKeys[] = new byte[4];
        final byte FIN = (byte)128, RSV_1 = (byte)64, RSV_2 = (byte)32, 
                   RSV_3 = (byte)16, OPCODE = (byte)15, MASK = (byte)128, 
                   PYLD_LENGTH = (byte)127;
        final BigInteger MAX_PYLD_LENGTH = new BigInteger("9223372036854775807", 10);
        final BigInteger PYLD_HGH_BIT = new BigInteger("-9223372036854775808", 10);
        
        // Get the first byte
        aByte = frame[dataDex];
        
        // Use a flag to determine wheather the fin bit was set
        fin = (aByte & FIN) == FIN ? true : false;
        
        // Use flags to determine which reserve bits were set 
        rsv1 = (aByte & RSV_1) == RSV_1 ? true : false; 
        rsv2 = (aByte & RSV_2) == RSV_2 ? true : false; 
        rsv3 = (aByte & RSV_3) == RSV_3 ? true : false;
        
        // Use a flag to determine the opcode
        opCode = (byte) (aByte & OPCODE); 
        
        // Get the second byte from the buffer 
        aByte = frame[++dataDex];
        
        // Determine whether the mask bit was set using a flag
        mask = (aByte & MASK) == MASK ? true : false;
        
        // Make sure this frame was masked by the client 
        if (!mask)
        {
            // Throw and error, all inbound frames must be maasked
            throw new Exception("All incoming frames from a client must be masked", 
                                new Throwable( NO_MASK ));
        }
        
        // Determine the payload length using the payload flag.
        payloadLength = aByte & PYLD_LENGTH;
        
        // Determine how many bytes the payload length figure takes up in the frame
        if (payloadLength == 126)
        {
            // Initialize total to 16 bit two byte combo
            total = 255; 
            
            /* If the payload length is 126 then the payload length is contained
             * in the next 16 bits. Get the first byte's worth (high byte) of
             * the number, and store it in total*/
            total &= frame[++dataDex];
            
            /* Shift the total up by eight bits. Zeros will be shifted in from 
             * the rigth */
            total <<= 8;
            
            /* Concatenate the second byte (low byte) with the total useing a 
             * bitwise XOR */
            total ^= frame[++dataDex];
            
            // Store the payload length for furture use 
            payloadLength = (int) total;
        }
        else if (payloadLength == 127)
        {
            // Intialiaze the total 
            total = 0;
            
            /* If the payload length was 127 the payload length is contained in
             * the next 64 bits (8 bytes). Loop through those bytes, 
             * concatenating them together to get the actual payload length.*/
            for (int i = 0; i < 8; i++)
            {
                // Concatenate the next byte from the frame 
                total ^= frame[++dataDex];
                
                // Shift the last byte over to make room for the next byte 
                total <<= 8; 
            }
            
            // Check to see if the total contained 0 for the most significant bit
            payloadLength = ((total & PYLD_HGH_BIT.longValueExact()) == 1  ? -1 : total);
        }
        if (payloadLength < 0 || (payloadLength > MAX_PYLD_LENGTH.longValueExact()))
        {
            /* If the payload length was negative or was greater than the 
             * maximum allowed payload length then throw an exception */
            throw new Exception("Payload length is out of bounds: " 
                                + payloadLength, 
                                new Throwable( PY_LD_LGTH ));
        }
        
        /* Next parse the the demasking key form the next 32 bit (4 bytes).*/
        for (int i = 0; i < maskKeys.length; i++)
        {
            // Capture the bytes 
            maskKeys[i] = frame[++dataDex];
        }
        
        /* Set the local payload string with the current payload string 
         * associated with this socket, so that the incoming payload will be 
         * concatenated on. */
        payload = sockets.get(clientKey).properties.get(WebSocket.PY_LOAD);
        
        /* Decrypt and store the payload data. This data will be used by the the
         * server how it sees fit. The decryption is done byte by byte from what
         * is left of the frame. Data messages (datagrams) that span multiple 
         * frames will be added to the current payload here. */
        for (int i = 0; i < payloadLength; i++)
        {
            /* Capture the next unmasked byte, cbcause bitwise operators produce 
             * SIGNED integers take the absoulute value of the unmasked integer. 
             * FIGURE OUT HOW TO BITWISE REMOVE THE HIGH BIT*/
            unmaskedPyldByte = Math.abs
            (
                /* To unmask the each byte from the payload each byte from the 
                 * frame is XORed with a byte from the mask key array. That byte is
                 * at the loop index modulos with the amount of bytes in the mask 
                 * key array*/
                frame[++dataDex] ^ maskKeys[i % maskKeys.length]
            );
                       
            // Make sure the byte unmaksed represents a supported code point              
            if (Character.isValidCodePoint( unmaskedPyldByte ))
            {
                // Add each unmasked byte from the payload to a local copy
                payload += Character.toString( unmaskedPyldByte );
            }
            else 
            {System.out.println("Invalid byte: " + unmaskedPyldByte);
                // SHOULD AN EXCEPTION BE THROWN IF WE GET AN INVALID CHARATER???
            }
        }
        
        // Add the concatenated payload back into the mapping for this socket
        sockets.get(clientKey).properties.put(WebSocket.PY_LOAD, payload);
        
        // Process the payload data according to the frame's fin bit and opcode
        if (fin)
        {
            /* If the fin bit is set to 1 then this was the last frame in the 
             * data gram, proceed by checking the opcode to see what to do with 
             * the delivered data gram. */
            if (opCode == 0)
            {
                /* If the opcode is 0, but the fin bit is 1 then the message is
                 * complete, get the opcode stored on the socket object for this
                 * connection. Continue processing the gathered message based on
                 * opcode.*/
            }
            
            // If the opcode is 1 then the payload is expected to be text based
            if (opCode == 1)
            {
                // Invoke the payload handler (if passed) passing the message to it
                if (wdh != null)
                {
                    wdh.payloadHandler
                    (
                        /* Clear the socket's payload so it's ready for the next 
                         * message */
                        sockets.get(clientKey).properties.put(WebSocket.PY_LOAD, "").getBytes("UTF-8")
                    );
                }
                
                // Clear the payload data stored for the socket 
                this.sockets.get(clientKey).properties.put(WebSocket.PY_LOAD, "");
            }
            // If the opcode was 2 then the payload is a byte string
            else if (opCode == 2)
            {
                /* Invoke the payload handler passed (if passed) passing the 
                 * bytes along */
                if (wdh != null)
                {
                    wdh.payloadHandler(payload.getBytes());
                }
                
                // Clear the payload data stored for the socket 
                this.sockets.get(clientKey).properties.put(WebSocket.PY_LOAD, "");
            }
            // If the opcode is 8 then the client wants to close the connection 
            else if (opCode == 8)
            {
                // Echo the frame back to the client 
                
                // Close the socket connection 
            }
            else if (opCode == 9)
            {
                // Send the ping data back to the client as a pong 
                
            }
            else if (opCode == 10)
            {
                // Handle pong frame
                System.out.println("We got a pong frame");
            }
        }
        else
        {
            /* A zero fin bit means that this was not the last frame in the 
             * data gram. Save the opcode from this the first frame, so that the
             * other frames are delt with in the same way. */
            this.sockets.get(clientKey).properties.put(WebSocket.OPCODE, 
                    Byte.toString(opCode));
        }
    }   

    @Override
    public void unsupportedDirective(SelectionKey clientKey, String directive)
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
    private String receiveHeaders (SelectionKey clientKey)
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
     * @TODO FIND OUT WHY "(\\/ )" REGEX ISN'T PARSING THE ' ' OUT OF GET METHOD HEADER
     *       TRY "( \\/ )"
     */
    private Map<String, String> parseAndValidateHeaders(String serialHeaders)
    {
        // Local Variable Declaration 
        Stream<String> headerEntries; String firstHeaderEntry, firstHeaderEntryKV[];
        Map<String, String> validatedHeaders = new HashMap<>();
        class Wrapper { String[] headerKV; }; Wrapper accessor = new Wrapper();  
        int firstCRLF = 0;
        
        // Find the first CRLF
        firstCRLF = serialHeaders.indexOf("\r\n"); 
        
        // Extract the first header entry
        firstHeaderEntry = serialHeaders.substring(0, firstCRLF);
        
        // Capture the rest of the headers 
        serialHeaders = serialHeaders.substring((firstCRLF + 2));

        // Split the method from the standard and version 
        firstHeaderEntryKV = firstHeaderEntry.split("(\\/)");
        
        // Make sure there was three element parsed from the first line 
        if (firstHeaderEntryKV.length == 3)
        {
            // Put the method in the map, trim off white space (should be filtered from regEx)
            validatedHeaders.put(RQS_METHOD, firstHeaderEntryKV[0].trim());

            // Put the standard into the map
            validatedHeaders.put(RQS_STANDRD, firstHeaderEntryKV[1]);
            
            // Put the version into the map
            validatedHeaders.put(RQS_VERSION, firstHeaderEntryKV[2]);
        }
        
        // Get a stream over the rest of the headers in the serialheaders passed
        headerEntries = serialHeaders.lines();
        
        // Loop through the rest of line header entries
        headerEntries.forEach((entry) ->
        {
            // Split each line along thier natural delimiters
            accessor.headerKV = entry.split("(: )");
            
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
                    validatedHeaders.put(accessor.headerKV[0], headerValue.trim());
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
    private void finishConnection(Map<String, String> headers, SelectionKey clientKey)
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
