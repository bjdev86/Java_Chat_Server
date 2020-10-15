/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

import java.nio.channels.SocketChannel;

/**
 * ServerDataEvent class (struct). This allows other threads to get access 
 * to the methods in the ChatServerDB class
 * 
 * @author Ben
 */
public class ChatDataEvent 
{
    // Local Variable Declaration 
    private ChatRoom chatThread;
    private SocketChannel socket;
    private String message;;
    private byte[] cmdString; 

    // Constructor
    public ChatDataEvent(ChatRoom ct, SocketChannel socket, String mssg, byte[] cmdString) 
    {
        this.chatThread = ct;
        this.socket = socket;
        this.message = mssg;
        this.cmdString = cmdString;
    }
    
    // Getters
    public ChatRoom getChatThread ()
    {
        return this.chatThread;
    }
    
    public SocketChannel getSocket()
    {
        return this.socket; 
    }
    
    public String getMessage()
    {
        return this.message;
    }
    
    public byte[] getCommandString()
    {
        return this.cmdString;
    }
    
}
