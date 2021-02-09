package com.viola.chatserver;

import java.util.Hashtable;
import java.util.Map;
import com.sun.net.httpserver.BasicAuthenticator;


public class ChatAuthenticator extends BasicAuthenticator {

    private Map<String, User> users = null;

    public ChatAuthenticator() {
        super("chat");
        users = new Hashtable<String, User>();
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        if (users.containsKey(username)) {
            if(users.get(username).getPassword().equals(password)){
            return true;
            }
        }
        return false;
    }

    public boolean addUser(String userName, User user) {
        if(!users.containsKey(userName)){
            users.put(userName, user);
            return true;
        }
        return false;
    }

    


    
}
