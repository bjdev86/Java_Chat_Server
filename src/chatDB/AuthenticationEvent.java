/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.nio.channels.SocketChannel;

/**
 *
 * @author Ben
 */
class AuthenticationEvent 
{
    // Private Data Members 
    private ChatServerDB server;
    private SocketChannel socket; 
    
    AuthenticationEvent(ChatServerDB serverThread, SocketChannel soc) 
    {
        // Assign private Data Members
        this.server = serverThread;
        this.socket = soc; 
    }
    
    // Private Member Getters 
    public ChatServerDB getServer() 
    {
        return this.server;
    }
    
    public SocketChannel getSocketChannel() 
    { 
        return this.socket; 
    }
}
