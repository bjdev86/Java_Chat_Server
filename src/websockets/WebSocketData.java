
package websockets;

import java.util.HashMap;
import java.util.Map;
import static java.util.Map.entry;
        

/**
 * Class to define a WebSocketData and all the data that is pernenet to it such as
 * name and opcode and payload
 * 
 * @author Ben
 */
public class WebSocketData 
{
    // Public Constants 
    public static final String OPCODE  = "OPCODE";
    public static final String PY_LOAD = "PY_LOAD";
    
    /* Provide a public mapping of properties for this websocket. 
     * Allow wild card value type */
    private Map<String, Object> properties = new HashMap<>(Map.ofEntries
    (
        entry(OPCODE, ""),
        entry(PY_LOAD, new Byte[0])
    ));
/*----------------------------------------------------------------------------*/    

    // Class constructor 
    public WebSocketData() {}
    
    /**
     * Method to set a property for a give property name key. The current value 
     * for the given property name key will be returned. 
     * 
     * @param <T> The type of value to be set 
     * @param key
     * @param value
     * @return
     */
    public <T> T setProperty(String name, T value)
    {
        return (T) this.properties.put(name, value);
    }
//    Try this version out!!!!
//    public <T> T setProperty(Entry<String, ?> entry)
    /**
     * Fetches the property associated with the given name key. If the name 
     * doesn't exist in the property mapping then {@code null} will be returned. 
     * <br><br>
     * A {@code Map <String, Object>} is maintained to associate property names 
     * with values, much the same way an associative array works. 
     * 
     * @param <T> The typing property used to cast the {@code Object} value in 
     *        the property map. Take care to use safe casting between types.
     * 
     * @param name The name of property value in this mapping.
     * 
     * @return The value associated with the property name in the property 
     *         mapping casted to the <code>T</code> type.
     */
    public <T> T getProperty(String name)
    {
        return (T) this.properties.get(name);
    }
    
    
/*----------------------------------------------------------------------------*/   
/* This code demonstrates how generics work in terms of capturing groups in 
 * Java. I don't quite understand why it exactly works yet. As near as I can tell 
 * the helper function helps define the wildcard for the compiler, so that it 
 * can be captured. Play around with this code more!*/
//    void foo(List<?> i) 
//    {
//        this.fooHelper(i);
//    }
//    
//    // Helper method created so that the wildcard can be captured
//    // through type inference.
//    private <T> T fooHelper(List<T> l) 
//    {
//        return l.set(0, l.get(0));
//    }
}
