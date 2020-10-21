package websockets;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

/**
 *
 * @author Ben
 */
public class HeaderStandards 
{
    public static final String WLD_CRD = "*";
    
    // Seed the standards map
    private static final  Map <String, List<String>> standards = Map.ofEntries
    (    
        entry("Origin", List.of("http://localhost:4200", "http://localhost")),
        entry("Host", List.of("127.0.0.1", "localhost", WLD_CRD)),
        entry("GET", List.of("")),
        entry("Connection", List.of("Upgrade")),
        entry("Pragma", List.of("no-cache")),
        entry("Cache-Control", List.of("no-cache")),
        entry("Sec-WebSocket-Key", List.of("kwhjAzx2OKPyh0LwS0NDQA==", WLD_CRD)),
        entry("Upgrade", List.of("")),
        entry("Accept-Encoding", List.of("gzip, deflate, br")),
        entry("Accept-Language", List.of("en-US,en;q=0.9")),
        entry("Sec-WebSocket-Extensions", List.of("permessage-deflate; client_max_window_bits")),
        entry("Sec-WebSocket-Version", List.of("13", WLD_CRD))
    );    
    
    /**
     * Method to check to see if the is valid against the standards set in this
     * class. The standard is check to see if it exists and the value is then 
     * tested to see if it's on the list of valid standards. 
     * 
     * @param standard The category or type to be validated 
     * @param value The value to of the category to be validated
     * 
     * @return boolean to indicate validity
     */
    public static boolean check(String standard, String value)
    {
        /* See if the standard exsists. If it does see the value passed is on 
         * the standard's list */
        return checkStandard(standard) ? standards.get(standard)
                                                  .contains(value) : false;
    }
    
    /**
     * Method to see if the standard exists in the mapping 
     * 
     * @param standard The standard to be checked
     * 
     * @return boolean indicating existence.
     */
    public static boolean checkStandard(String standard)
    {
        return standards.containsKey(standard);
    }
}
