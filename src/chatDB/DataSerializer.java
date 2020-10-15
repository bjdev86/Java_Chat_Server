/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ben
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
