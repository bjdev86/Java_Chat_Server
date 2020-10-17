
package chatDB;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to define and enforce a deserialization and by consequence a 
 * serialization schema used for sending and receiving messages to and from
 * this server. The class defines constants that are used through out the server
 * to define what commands are known to the server. Additionally, this class 
 * defines constants for errors and error messages. Since this class deserializes
 * command byte strings that are set as a series of key-value pairs the class
 * defines delimiters between each key-value pair entry and between each key and 
 * value in the entry. 
 * <br><br>
 * The following is a list of all currently available commands. 
 * <ul>
 *      <li>LOG_OFF</li>
 *      <li>LOG_IN</li>
 *      <li>SGN_UP</li>
 *      <li>ENTRY_DELIM</li>
 *      <li>KV_DELIM</li>
 *      <li>CMD</li>
 *      <li>UNAME</li>
 *      <li>PSSWRD</li>
 *      <li>FNAME</li>
 *      <li>LNAME</li>
 *      <li>ERR_MSG</li>
 *      <li>ERRORED</li>
 *      <li>CHAT_NAME</li>
 *      <li>CRT_CHT</li>
 *      <li>JOIN_CHT</li>
 *      <li>LEAVE_CHT</li>
 *      <li>SIGN_OFF</li>
 * </ul>
 * 
 * For example, if a client wanted to issue a sign in command to server they 
 * would create a data string of the form <code>CMD + KV_DELIM + LOG_IN + 
 * ENTRY_DELIM + UNAME + KV_DELIM + "usrname" + ENTRY_DELIM + PSSWRD + 
 * "password" </code>. This data string includes a command, "LOG_IN" and then 
 * the dependent data needed to execute that command, namely UNAME + KV_DELIM +
 * "username" + ENTRY_DELIM + PSSWRD + KV_DELIM + "password".
 * <br><br>
 * On the server side the <code>deserializeDate</code> method is used to parse
 * the data string and convert it into a associative map. That map acts as a 
 * Data Access Object and allows for quick easy retrieval and access to the 
 * command and the data sent by the client.
 * 
 * @author Ben Miller
 * @version 1.0
 */
public class DataSerializer 
{
    // Entrace worke parsing constatns 
    public final static String LOG_OFF = "LOG_OFF";   
    public final static String LOG_IN  =  "LOG_IN"; 
    public final static String SGN_UP  =  "SGN_UP"; 
    
    public final static String ENTRY_DELM = ";";
    public final static String KV_DELM    = "=";
    
    public final static String CMD     =    "CMD";
    public final static String UNAME   =  "UNAME";
    public final static String PSSWRD  = "PSSWRD";
    public final static String FNAME   =  "FNAME";
    public final static String LNAME   =  "LNAME";
    public final static String ERR_MSG = "ERR_MSG";
    public final static String ERRORED = "ERRORED";
    
    // WaitingRoom worker command constants 
    public static final String CHAT_NAME  = "CHAT_NAME";
    public static final String CRT_CHT    = "CRT_CHT"; 
    public static final String DLT_CHT    = "DLT_CHT"; 
    public static final String JOIN_CHT   = "JOIN_CHT"; 
    public static final String LEAVE_CHT  = "LEAVE_CHT"; 
    public static final String SIGN_OFF   = "SIGN_OFF"; 
    
    // ChatThread Command constants 
    public static final String MSG = "MSG";
    
     /* Method to deserialize data array sent by the client to this worker thread
     * the method will parse the deserialized data String into a Set which will 
     * be returned */
    public static Map<String, String> deserializeData (byte[] data)
    {
        // Local Variable Declaration 
        Map <String, String> asocArray = new HashMap<>();
        String[] keyVals; 
        
        // Convert the data byte array into a String 
        String dataString = new String(data).trim();
        
        // Proceed only if the empty string wasn't passed
        if(!dataString.equals(""))
        {        
            // Parse the data string over the expected semicolons
            String[] entries = dataString.split(ENTRY_DELM);

            // Loop through each entry 
            for (String entry : entries)
            {
                // Parse the entry string over the equals since, spliting key from value
                keyVals = entry.split(KV_DELM);

                // Put the key-value pair in the associative array 
                asocArray.put(keyVals[0], keyVals[1]);
            }
        }
        return asocArray;
    }
}
