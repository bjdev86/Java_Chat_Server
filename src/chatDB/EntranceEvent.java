/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 *
 * @author Ben
 */
class EntranceEvent
{
    // Local Variable Declaration 
    private ChatServerDB chatServer;
    private SocketChannel socket;
    private String command; 
    private Map <String, String> variables;
    
    public EntranceEvent(ChatServerDB chatServer, SocketChannel sc, String command, 
                         Map<String, String> vars) 
    {
        this.chatServer = chatServer;
        this.socket = sc; 
        this.command = command;
        this.variables = vars; 
    }
    
    // Getters 
    public ChatServerDB getChatServer()
    {
        return this.chatServer;
    }
    
    public SocketChannel getSocket()
    {
        return this.socket;
    }
    
    public String getCommand()
    {
        return this.command;
    }
    
    public Map<String, String> getVariables()
    {
        return this.variables;
    }
}
