package com.viola.chatserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.File;
import java.security.SecureRandom;

import org.apache.commons.codec.digest.Crypt;

public class ChatDatabase {

    Connection connection;
    final SecureRandom secureRandom = new SecureRandom();
    
    private static ChatDatabase singleton = null;

        public static synchronized ChatDatabase getInstance() {
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    } 

    private ChatDatabase() {
    }

    public void open(String dbName) throws SQLException{
        File database = new File(dbName);
        boolean b = database.exists();
        String jdbcAddress = "jdbc:sqlite:" + dbName;
        connection = DriverManager.getConnection(jdbcAddress);
        if (b==false){
        initializeDatabase();
        }
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean initializeDatabase(){
        if (null != connection) {
            String createUsersString = "create table users (username varchar(32) primary key not null, password varchar(32) not null, email varchar(32) not null)";
            Statement createStatement;
            try {
                createStatement = connection.createStatement();
                createStatement.executeUpdate(createUsersString);
                createStatement.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            String createMessagesString = "create table messages (nickname varchar(32) not null, message varchar(254) not null, timestamp numeric not null, primary key(nickname, timestamp), foreign key(nickname) references users(username))";
            Statement Statement;
            try {
                Statement = connection.createStatement();
                Statement.executeUpdate(createMessagesString);
                Statement.close();
                System.out.println("tietokannan luonti onnistui");
                return true;
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        return false;
    }

    public boolean addUserToDb(String userName, User user) throws SQLException{
        String password = user.getPassword();
        String email = user.getEmail();
        boolean b = false;
        //lisää käyttäjän jos samannimistä ei jo ole
        
        
        if(checkUser(userName) == true){
    
            //hashaa salasanan
            byte bytes[] = new byte[13];
            secureRandom.nextBytes(bytes);
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes;
            String hashedPassword = Crypt.crypt(password, salt);

            //tallentaa käyttäjän tiedot ja hashatyn salasanan tietokantaan
            String insertUserString = "insert into users " +  "VALUES ('"+userName+"', '"+hashedPassword+"', '"+email+"')"; 
            Statement createStatement;
            createStatement = connection.createStatement();
            createStatement.executeUpdate(insertUserString);
            createStatement.close();
            System.out.println("käyttäjän lisäys onnistui");
            b = true;
        }else{
            System.out.println("Käyttäjä jo olemassa");
            b = false;
        }
        return b;
    }

    public boolean checkUser(String username){
        String user = "SELECT username FROM users";
        Statement createStatement;
        try {
            createStatement = connection.createStatement();
            ResultSet users = createStatement.executeQuery(user);
            //jos sama nimi löytyy palauttaa false
            while(users.next()){
                String tempusername = users.getString("username");
                if(tempusername.equals(username)){
                    System.out.println("käyttäjä on jo olemassa");//valittaa primary key constraint ennen ku pääse tähän
                    return false;
                }
                createStatement.close(); 
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    public boolean authUser(String username, String password){
        String query = "SELECT * FROM users WHERE (username = '"+username+"') ";
        Statement createStatement;
        try {
            createStatement = connection.createStatement();
            ResultSet rs = createStatement.executeQuery(query);
           
            while(rs.next()){
                String dbusername = rs.getString("username");
                String dbpassword = rs.getString("password");
                String hashedPassword = Crypt.crypt(password, dbpassword);
                
                if(dbusername.equals(username) && dbpassword.equals(Crypt.crypt(password, dbpassword))){
                    System.out.println("authorisointi onnistui");
                    return true;
                }
                createStatement.close(); 
            } 
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public void insertMessage(String nick, String message, long sent){
        String insertMessageString = "insert into messages " +  "VALUES ('"+nick+"', '"+message+"', '"+sent+"')"; 
        Statement Statement;
        try {
            Statement = connection.createStatement();
            Statement.executeUpdate(insertMessageString);
            Statement.close();
        }catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public ArrayList<ChatMessage> readMessages(long since){
        ArrayList<ChatMessage> messages = null;
        String query = "SELECT nickname, message, timestamp FROM messages WHERE (timestamp > '"+ since +"')";
        Statement createStatement;
        
        try {
            createStatement = connection.createStatement();
            ResultSet rs = createStatement.executeQuery(query);
            while(rs.next()){
                if(null == messages){
                    messages = new ArrayList<ChatMessage>();
                }
                String user = rs.getString("nickname");
                String message = rs.getString("message");
                long sent = rs.getLong("timestamp");
                ChatMessage msg = new ChatMessage();
                msg.nick = user;
                msg.message = message;
                msg.setSent(sent);
                messages.add(msg);
            }
            createStatement.close(); 
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return messages;
    } 

}
