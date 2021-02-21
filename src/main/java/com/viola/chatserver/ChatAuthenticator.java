package com.viola.chatserver;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Map;
import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator {

   

    public ChatAuthenticator() {
        super("chat");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        boolean b = ChatDatabase.getInstance().authUser(username, password);
        return b;
    }

    public boolean addUser(String userName, User user) throws SQLException {
            boolean b = ChatDatabase.getInstance().addUserToDb(userName, user);
            return b;
    }

    
}
