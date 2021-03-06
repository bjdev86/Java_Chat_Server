
package websockets;

import chatDB.RecptionRoom;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
public final class WebSocketSocketAPI extends AbstractWebSocketAPI<Socket>
{
/*----------------------------------- SINGLETON ------------------------------*/    
    // The singleton instance of this class 
    private static final WebSocketSocketAPI theAPI = new WebSocketSocketAPI();
/*---------------------------- PRIVATE DATA MEMBERS --------------------------*/    
    // Private Constants
    private static final String RQS_METHOD  = "RQS_METHOD";
    private static final String RQS_VERSION = "RQS_VERSION";
    private static final String RQS_STANDRD = "RQS_STANDRD";
    
    // Exceptoin strings
    public static final String NO_MASK    = "NoMaskException";
    public static final String PY_LD_LGTH = "PayloadLengthOutOfBounds";
    
    // Byte buffer to hold header data exchanged during connection handshake
    private static ByteBuffer headerBuff = ByteBuffer.allocate(600); // DIRECT???
    
/*----------------------------------------------------------------------------*/    
    
    // Private constructor, only this class will instansiate an instance of this class
    private WebSocketSocketAPI(){}

    /**
     * Method to return the singleton instance created when this class is loaded
     * into memory. This is the one and only instance of this class that will 
     * be available to use by any outside class. This means that the data 
     * encapsulated in this singleton reference will be uniform for the life of
     * the singleton to which the reference returned points.
     * 
     * @return The Singleton reference that points to the singleton held by this
     *         class, created when the class was loaded into memory
     */
    public static WebSocketSocketAPI getInstance ()
    {
        return theAPI;
    }

    /**
     * Staging method for finishing up a connection that has been accepted by
     * this server.The method parse the headers sent by the client, validate 
     * them and then complete the TCP handshake that will establish the 
     * connection with the server. 
     * 
     * @param clientSoc The Socket associated with the web socket trying
     *        to connect to this server.
     */
    @Override
    public synchronized void connect(Socket clientSoc) 
    {
       // Local Variable Declaration 
        Map<String, String> headers = new HashMap<>(); 
        String headerString = "", rqsMethod = "", socKey = "";
        float rqsVersion = 0;
        
        // Read headers from socket client
        headerString = receiveHeaders(clientSoc);
        
        // Parse and validate the headers if the headerString could be read
        if(headerString != null)
        {
            headers = parseAndValidateHeaders(headerString);
        
            // Extract the HTTP method and version used in this connection request
            rqsMethod = headers.get(RQS_METHOD);
            rqsVersion = Float.parseFloat(headers.get(RQS_VERSION));
            socKey = headers.get("Sec-WebSocket-Key");
        }
        
        /* Make sure the header contained the GET method has a version higher 
         * 1.1 and that the socket key exists */ 
        if (headerString != null && rqsMethod.equals(new String("GET")) && 
            rqsVersion >= 1.1 && socKey != null)
        {
            // Complete handshake, by sending response header confirming connection terms
            finishConnection(headers, clientSoc);
            
            // Add the socket to the map of sockets by the name passed 
            this.sockets.put(clientSoc, new WebSocketData()); 
        }
        else
        {            
            try 
            {
                // Build a response header for the error 
                byte rsp[] = (this.BAD_RQST_HDR 
                           + "Malformed request. The connection request must "
                           + "use a 'GET' method, must have a version greater "
                           + "than 1.1 and have 'Sec-WebSocket-Key'. Please"
                           + "check your headers"
                           +  "\\r\\n").getBytes("UTF-8");
            
                // Send the response error header to the client
                clientSoc.getOutputStream().write(rsp);
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(RecptionRoom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Method to encapsulate a byte array into a proper Websocket frame. The 
     * method allows the data byte array to be broken up into multiple web 
     * socket frames. Partial frames are saved and tacked on to the end of the 
     * sequence. The method collects all the frames created in a 2D array of 
     * bytes with the first frame stored at {@code frames[0]}. The passed handler
     * is called to process the frames once they've been created. 
     * 
     * @param data The byte array to enframe. This array may be broken up into 
     *        multiple frames
     * @param opcode Determines what type of data the frame(s) should be 
     *        interpreted as by the receiver. 1 for text (string) and 2 for 
     *        binary data. Defaults to 1 if > 2. 
     * @param frameDataSize The size of an individual frame. This size must be 
     *        between 0 and 2^31 - 1 (since Java uses 32 bit signed integers).
     *        This value will default to 2^31 - 1 (Integer.MAX_VALUE). 
     * @param framesHndlr Lambda called with frames created. This lambda is 
     *        expected to handle frames once they have been created. 
     */
    @Override
    public synchronized void frame(byte[] data, byte opcode, int frameDataSize, WebsocketFramedDataHandler framesHndlr) 
    {
        // Local Variable Declaration 
        byte byte1 = (byte) 0b11111111; byte byte2 = (byte) 0b11111111,
        frames[][], mask[] = new byte[4]; 
        int partFrameDataSize = 0, frameCount = 0, frameDex = 0, dataDex = 0, 
            pyldByteCount = 0, dataLength = data.length, thisFrameSize = 0; 
        final int BIT_16_PYLD = 65535, BIT_7_PYLD = 125;
        
        /* The frame size must be a valid number that is less than the maximum 
         * allowed and greater than -1 if it's not force it into maximum allowed */
        frameDataSize = frameDataSize <= Integer.MAX_VALUE && frameDataSize > -1 
                  ? frameDataSize : Integer.MAX_VALUE;
        
        /* Calculate the frame count from desired frame size payload byte array 
         * passed */
        frameCount = data.length / frameDataSize;
        
        // Capture the pay load length of the last partial frame (if any)
        partFrameDataSize = data.length % frameDataSize; 
        
        // Make sure to add in the last potentially partial frame
        frameCount += partFrameDataSize == 0 ? 0 : 1; 
        
        // Set the opcode on the last 4 bits 
        byte1 &= opcode > 2 ? 1 : opcode; 
        
        /* Set the fin bit for the first frame in the sequence of frames. Assume
         * reserved bit flags wont be used for now. */
        byte1 ^= frameCount > 1 ? 0b00000000 : 0b10000000;
        
        // Set reserved flag bits
                
        // Instansiate the array of frame arrays 
        frames = new byte[frameCount][0]; 
      
        /* If the frame count is only one and partialFrameSize is greater than 0 
         * then the partialFrameSize should be used to determine the length of 
         * the frame buffer since there is only enough data for a frame with a 
         * partial payload. */
        thisFrameSize = frameCount == 1 && partFrameDataSize > 0 
                                         ? partFrameDataSize : frameDataSize;
        // Loop through and build each frame one by one 
        for (int i = 0; i < frameCount; i++)
        {
            // Determine the length code
            if (thisFrameSize <= BIT_7_PYLD)
            {
                /* If the desired frame is less than or equal to the 7 bit 
                 * payload size then the payload length number will fit into the 
                 * final 7 bits of the second byte of the frame. Go ahead and 
                 * create that byte by anding the desired frame size with -128 
                 * (0b10000000), this will manipulate the lower 7 bits of the 
                 * byte, while leaving the top bit as 1. The top bit is the mask
                 * bit, which will tell the reciever we're sending this frame 
                 * with a masked payload. No additional byte space in the frame 
                 * will be needed. */
                byte2 &= (-128 + thisFrameSize);                
            }
            else if (thisFrameSize <= BIT_16_PYLD)
            {
                /* If the desired frame payload length is between 125 and 65535
                 * then the payload code of 126 will be put in the lower 7 bits
                 * of the second byte this will indicate to the receiver that 
                 * the length of the payload is stored in the next 16 bits 
                 * (2 bytes).*/
                byte2 &= 0b11111110;
                //byte2 &= 0b01111110;
                
                /* Indicate that the final frame will need space to pack the 
                 * bytes of this number */
                pyldByteCount = 2;
            }
            else 
            {
                /* If the desired payload length is between 2^16 and 2^64 - 1 
                 * (2^32 - 1 for Java arrays) then payload code 127 will be 
                 * stored in the lower 7 bits of the second byte to indicate 
                 * this. The reciever will know to look in the 64 bits (8 bytes)
                 * for the unsigned integer indicating the payload length. */
                byte2 &= 0b11111111;
                //byte2 &= 0b01111111;
                
                /* Indicate that the final frame will need to allocate space to
                 * pack all 8 bytes in. */
                pyldByteCount = 8;
            }
            
            // Set the size of the frame 
            frames[i] = new byte[2 + pyldByteCount + mask.length + thisFrameSize];
            
            /* Pack the first and second bytes, with the fin bit, reserved bits, 
             * opcode, mask bit and payload length/code */
            frames[i][frameDex = 0] = byte1; // Restart the frame index
            frames[i][++frameDex]   = byte2;
           
            // Generate Mask key 
            mask[0] = 50; 
            mask[1] = 123;
            mask[2] = 100;
            mask[3] = 36;
            
            /* The next set of bytes to add to the frame are the bytes that 
             * comprise payload length. The payload length number could be 1 
             * byte, 2 bytes, or 8 bytes wide and needs to put in the correct 
             * order with low byte of the payload length number put in the low 
             * byte position (last byte) in the frame payload length position. 
             * Therefore, set the position of the frame index to the position 
             * where the low byte of the payload length should go unless the 
             * payload length is 0 meaning the payload length was less than 125.*/
            frameDex += pyldByteCount; 
            
            /* Loop bacwards through current frame adding the bytes of 
             * the extedended payload length in lowest to highest order to the 
             * frame array. */
            for (int j = 0; j < pyldByteCount; j++)
            {
                // Set in the frame array (255 = 0b11111111)
                frames[i][frameDex - j] = (byte) (dataLength & 255); 
                
                // Shift the next byte off the length of bytes in the frame 
                dataLength >>>= 8; 
            }
            
            /* Pack each byte of the mask key into the frame */
            for (byte key : mask)
            {
                frames[i][++frameDex] = key;
            }
            
            /* Now the byte data in the array passed can be added to the frame.
             * Loop through the payload data and copy it over to the frame. This
             * concludes the forming of one frame. */
            for (int j = 0; j < thisFrameSize; j++)
            {
                frames[i][++frameDex] = (byte) ((data[dataDex++] ^ mask[j % 4])); 
            }
            
            /* Check whether the next frame is the last one or not. If it is 
             * then set the opcode to zero, 'continuation' */
            if ((opcode != 0b00000000) && (i < frameCount))
            {
                // Set the bottom nibble of the first byte 
                byte1 &= 0b11110000;   
            }
            
            // Check to see if the last frame is about to be processed
            if (i == (frameCount - 1))
            {
                // Flip the fin bit in the first byte, preserving all other bits 
                byte1 &= 0b11111111;
                
                // Change the size used to determine the frame payload data size 
                thisFrameSize = partFrameDataSize;
            }
        }

        // Invoke the call back to pass the framed byte arrays back 
        framesHndlr.onFramed(frames);
    }
    
    /**
     * Method to parse a frame sent by a client.The method takes in a Lambda
     * expression ({@link #WebsocketDataHandler WebsocketStringDataHandler}) 
     * that will handle the payload data send in the frame. The payload could be 
     * either a string of characters or bytes. 
     * 
     * @param frame The byte array containing the bytes that make up the frame
     * @param strHndlr The 
     *        {@link #WebsocketStringDataHandler WebsocketStringDataHandler} lambda
     *        expression to handle the parsed payload.
     * @param clientSoc The {@code Socket} of socket over which this frame 
     *        is coming. This key is also how this connection is being tracked
     *        by this API.
     * @param byteHndlr
     * 
     * @throws Exception If the frame's mask bit is unset or if the 
     *         payload length is out of bounds. These are fatal errors and the socket
     *         connection should be closed.
     * 
     * @TODO If the caller doesn't pass a payload handler should an error be 
     *       thrown or should just nothing happen? Right now nothing happens
     * 
     * @TODO Move the command to store payload data on the socket object to the
     *       else connected to the if(fin) conditional.
     * 
     * @TODO Consider passing a lambda that would allow the caller to collect 
     *       the payload data from frames where the fin bit is unset (0). In 
     *       effect the caller would responsible for reassembling a sequence of 
     *       frames payload data.
     */
    @Override
    public synchronized void unFrame( byte[] frame, Socket clientSoc, 
       WebsocketStringDataHandler strHndlr, WebsocketByteDataHandler byteHndlr ) throws Exception
    {
        // Local Variable Declaration 
        int dataDex = 0; long total = 0, payloadLength = 0;
        
        boolean fin = false, rsv1 = false, rsv2 = false, rsv3 = false, mask = false;
        
        byte aByte = 0x0, opCode = 0x0, maskKeys[] = new byte[4];
        
        ArrayList<Object> payload = new ArrayList<>();
        
        final BigInteger PYLD_HGH_BIT = new BigInteger("-9223372036854775808", 10);        
        final byte FIN = (byte)128, RSV_1 = (byte)64, RSV_2 = (byte)32, 
                   RSV_3 = (byte)16, OPCODE = (byte)15, MASK = (byte)128, 
                   PYLD_LENGTH = (byte)127;
        
        // Get the first byte
        aByte = frame[dataDex];
        
        // Use a flag to determine wheather the fin bit was set
        fin = (aByte & FIN) == FIN ? true : false;
        
        // Use flags to determine which reserve bits were set 
        rsv1 = (aByte & RSV_1) == RSV_1; 
        rsv2 = (aByte & RSV_2) == RSV_2; 
        rsv3 = (aByte & RSV_3) == RSV_3;
        
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
            
            /* Concatenate the second byte (low byte) with the total using a 
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
        if (payloadLength < 0 || ( payloadLength > Long.MAX_VALUE ))
        {
            /* If the payload length was negative or was greater than the 
             * maximum allowed payload length then throw an exception */
            throw new Exception("Payload length is out of bounds: " 
                                + payloadLength, 
                                new Throwable( PY_LD_LGTH ));
        }
        
        /* Next parse the the demasking key form the next 32 bits (4 bytes). */
        for (int i = 0; i < maskKeys.length; i++)
        {
            // Capture the bytes 
            maskKeys[i] = frame[++dataDex];
        }
        
        /* Get the payload data saved in the data mapping for this connection 
         * and put it in a new ArrayList, so that it can be concatenated with 
         * the payload being read from this frame. */
        payload = new ArrayList<>(Arrays.<Object>asList(sockets.get(clientSoc)
                     .<Object[]>getProperty(WebSocketData.PY_LOAD)));
                
        /* Decrypt and store the payload data. This data will be used by the the
         * server how it sees fit. The decryption is done byte by byte from what
         * is left of the frame. Data messages (datagrams) that span multiple 
         * frames will be added to the current payload here. */
        for (int i = 0; i < payloadLength; i++)
        {   
            /* To unmask the each byte from the payload each byte from the 
             * frame is XORed with a byte from the mask key array. That byte is
             * at the loop index modulos with the amount of bytes in the mask 
             * key array. */
             payload.add((byte)(frame[++dataDex] ^ maskKeys[i % maskKeys.length]));
        }
        
        /* Store the payload array in the data wrapper associated with the 
         * client connection */
        this.sockets.get(clientSoc).<Object[]>setProperty(
            WebSocketData.PY_LOAD, payload.toArray()
        );
        
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
                opCode = (byte) this.sockets.get(clientSoc)
                                    .<Object>getProperty(WebSocketData.OPCODE);
            }
            
            // If the opcode is 1 then the payload is expected to be text based
            if (opCode == 1)
            {
                /* Invoke the payload handler (if passed) passing the message to 
                 * this method's caller */
                if (strHndlr != null)
                {   
                    // Unbox the Object array to primitive byte[] for return to caller
                    byte rawData[] = this.unboxByteArray(
                        /* Clear the payload kept by the property mapper, as the
                         * current byte string is passed to this method caller */
                        this.sockets.get(clientSoc).<Object[]>setProperty(WebSocketData
                                              .PY_LOAD, new Object[0])
                    );
                    
                    // Pass the byte array as a String back
                    strHndlr.handleStringData( new String(rawData, "UTF-8"));
                    
                } // ELSE THROW AN ERROR?                
            }
            // If the opcode was 2 then the payload is a byte string
            else if (opCode == 2)
            {
                /* Invoke the payload handler passed (if passed) passing the 
                 * bytes along */
                if (byteHndlr != null)
                {
                    // Unbox the Object array to primitive byte[] for return to caller
                    byte rawData[] = this.unboxByteArray(
                        /* Clear the payload kept by the property mapper, as the
                         * current byte string is passed to this method caller */
                        this.sockets.get(clientSoc).<Object[]>setProperty(WebSocketData
                                              .PY_LOAD, new Object[0])
                    );
                    
                    // Pass the byte array back
                    byteHndlr.handleByteData( rawData );
                    
                } // ELESE THROW AN ERROR?                
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
        else if (opCode != 0)
        {
            /* A zero fin bit means that this was not the last frame in the 
             * frame sequence. Save the opcode from the first frame, so 
             * that the other frames are delt with in the same way. Filter out 
             * any zero opCode. THIS NEEDS TO BE TESTED. */
            this.sockets.get(clientSoc).setProperty(WebSocketData.OPCODE, 
                    Byte.toString(opCode));            
        }
    } 
    
/*------------------------------ PRIVATE METHODS -----------------------------*/
    /**
     * Method to read in the raw HTTP headers from the socket client during the
     * initial connection process. 
     * 
     * @param clientSoc The key representing the client connection in this 
     *        selector thread's selector.
     * 
     * @return The raw serialized string representation of the client socket 
     *         headers
     */
    private String receiveHeaders (Socket clientSoc)
    {
        // Local Variable Declaration 
        int bytesRead = 0; byte[] trimedHeader;
        byte[] headerBuffer = new byte[600];
        String rawHeaders = null; SocketChannel clientSC; 
        
        try 
        {
            // Read the header in from the socket channel  
            bytesRead = clientSoc.getInputStream().read(headerBuffer);

            // Initialize the header byte array to the amount of bytes read
            trimedHeader = new byte[bytesRead];

            // Trim off any empty space 
            System.arraycopy(headerBuffer, 0, trimedHeader, 0, bytesRead);

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
     * empty String ("") is mapped to the valid standard name. 
     * 
     * @param serialHeaders The raw serialized header String.
     * 
     * @return The mapping of header keys to header values as extracted from the
     *         serialHeaders string. 
     * @TODO FIND OUT WHY "(\\/ )" REGEX ISN'T PARSING THE ' ' OUT OF GET METHOD 
     *       HEADER TRY "( \\/ )"
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
     * @param clientSoc The SelectionKey that represents the client socket 
     *        trying to connect
     * 
     * @TODO Add response header for a bad request when an IOException is caught
     */
    private void finishConnection(Map<String, String> headers, Socket clientSoc)
    {
        // Local Variable Declaration
        String SWSKey;
        byte[] rsp; 
        SocketChannel sc = null; 
        
        try 
        {
            // Get the Sec-WebSocketData-Key
            SWSKey = headers.get("Sec-WebSocket-Key");

            // Build response header to send to client to complete handshake
            rsp = (this.HND_SHK_HDR
                + Base64.getEncoder().encodeToString(MessageDigest
                .getInstance("SHA-1").digest((SWSKey + this.magicString)
                .getBytes("UTF-8"))) + "\r\n\r\n").getBytes("UTF-8");

            // Write the response header back to the connection client. That's it!
            clientSoc.getOutputStream().write(rsp);
        } 
        catch (IOException ex) 
        {            
            ex.printStackTrace();
        } 
        catch (NoSuchAlgorithmException ex) 
        {
            Logger.getLogger(RecptionRoom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Method to unbox a {@code Byte[]} wrapper object to a primitive 
     * {@code byte[]}. 
     * 
     * @param unboxByteArray The array who's values will individually be unboxed.
     */
    private byte[] unboxByteArray (Object [] box)
    {
        // Local Variable Declaration 
        byte[] unboxed = new byte[box.length];
        
        // Loop through boxed array and convert 
        for (int i = 0; i < box.length; i++)
        {
            unboxed[i] = (byte) box[i];
        }
        
        return unboxed;
    }
}
