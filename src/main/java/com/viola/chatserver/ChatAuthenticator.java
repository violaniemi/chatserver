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
    public boolean checkCredentials(String username, String password)  {
        boolean b=false;
        try {
            b = ChatDatabase.getInstance().authUser(username, password);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return b;
    }

    public boolean addUser(String userName, User user) throws SQLException {
            boolean b = ChatDatabase.getInstance().addUserToDb(userName, user);
            return b;
    }

    
}
