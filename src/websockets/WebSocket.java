
package websockets;

import java.util.HashMap;
import java.util.Map;
import static java.util.Map.entry;

/**
 * Class to define a WebSocket and all the data that is pertenet to it such as
 * name and opcode and payload
 * 
 * @author Ben
 */
public class WebSocket 
{
    // Public Constants 
    public static final String OPCODE  = "OPCODE";
    public static final String PY_LOAD = "PY_LOAD";
    
    // Provide a public mapping of properties for this websocket
    public Map<String, String> properties = new HashMap<>(Map.ofEntries
    (
        entry(OPCODE, ""),
        entry(PY_LOAD, "")
    ));
}
