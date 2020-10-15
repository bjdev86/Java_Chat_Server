/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatDB;

/**
 * Class to implement multiplexer worker thread. The thread will take a 
 * message from one socketchannel (contact) and enqueue the message for all
 * other participants in the chat.
 * 
 * @author Ben
 */
public class Multiplexor implements Runnable
{
    public Multiplexor() {}
        
        public void processMsg() {}

        @Override
        public void run() 
        {
            // TODO Auto-generated method stub
        }
}
