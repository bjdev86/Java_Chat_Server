/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ben
 */
class WaitRoomEvent
{
    // Local Variable Declaration 
    private WaitingRoom wtrThread;
    private SocketChannel socket;
    private Map<String, String> vars;
    
    public WaitRoomEvent(WaitingRoom wrtrd, SocketChannel sc, Map vars) 
    {
        this.wtrThread = wrtrd;
        this.socket = sc; 
        this.vars = vars;
    }
    
    // Getters 
    public WaitingRoom getWaitingRoomThread()
    {
        return this.wtrThread;
    }
    
    public SocketChannel getSocket()
    {
        return this.socket;
    }
    
    public Map<String, String> getVariables()
    {
        return vars;
    }
}
