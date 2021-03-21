package com.viola.chatserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.io.File;
import java.security.SecureRandom;

import org.apache.commons.codec.digest.Crypt;

public class ChatDatabase {

    private Connection connection;

    private static ChatDatabase singleton = null;

    public static synchronized ChatDatabase getInstance() {
        if (null == singleton) {
            singleton = new ChatDatabase();
        }
        return singleton;
    }

    private ChatDatabase() {
    }

    public void open(String dbName) throws SQLException {
        File database = new File(dbName);
        //tarkistetaan onko tietokanta jo olemassa
        boolean b = database.exists();
        //muodostetaan osoite ja yhteys
        String jdbcAddress = "jdbc:sqlite:" + dbName;
        connection = DriverManager.getConnection(jdbcAddress);
        //jos tietokantaa ei ole luodaan uusi
        if (b == false) {
            initializeDatabase();
        }
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean initializeDatabase() throws SQLException {
        if (null != connection) {
            // luodaan käyttäjätaulu
            String createUsersString = "create table users (username varchar(32) primary key not null, password varchar(32) not null, email varchar(32) not null)";
            Statement createStatement;
            createStatement = connection.createStatement();
            createStatement.executeUpdate(createUsersString);
            createStatement.close();
            // luodaan viestitaulu
            String createMessagesString = "create table messages (nickname varchar(32) not null, message varchar(254) not null, timestamp numeric not null, primary key(nickname, timestamp))";
            Statement Statement;
            Statement = connection.createStatement();
            Statement.executeUpdate(createMessagesString);
            Statement.close();
            ChatServer.log("Database created");
            return true;
        }
        return false;
    }

    public boolean addUserToDb(String userName, User user) throws SQLException {
        SecureRandom secureRandom = new SecureRandom();
        String password = user.getPassword();
        String email = user.getEmail();
        boolean b = false;

        // lisää käyttäjän jos samannimistä ei jo ole
        if (checkUser(userName) == true) {

            // hashaa salasanan
            byte bytes[] = new byte[13];
            secureRandom.nextBytes(bytes);
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes;
            String hashedPassword = Crypt.crypt(password, salt);

            // tallentaa käyttäjän tiedot ja hashatyn salasanan tietokantaan
            String insertUserString = "insert into users " + "VALUES ('" + userName + "', '" + hashedPassword + "', '"
                    + email + "')";
            Statement createStatement;
            createStatement = connection.createStatement();
            createStatement.executeUpdate(insertUserString);
            createStatement.close();
            b = true;
        } else {
            ChatServer.log("User already exists");
            b = false;
        }
        return b;
    }

    public boolean checkUser(String username) throws SQLException {
        String user = "SELECT username FROM users";
        Statement createStatement;
        createStatement = connection.createStatement();
        ResultSet users = createStatement.executeQuery(user);
        //käy läpi tietokannan käyttäjännimet
        // jos sama nimi löytyy palauttaa false
        while (users.next()) {
            String tempusername = users.getString("username");
            if (tempusername.equals(username)) {
                return false;
            }
        }
        createStatement.close();
        return true;
    }

    public boolean authUser(String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE (username = '" + username + "') ";
        Statement createStatement;
        createStatement = connection.createStatement();
        ResultSet rs = createStatement.executeQuery(query);

        while (rs.next()) {
            String dbusername = rs.getString("username");
            String dbpassword = rs.getString("password");
            String hashedPassword = Crypt.crypt(password, dbpassword);
            //vertaa annettua käyttäjännimeä ja salasanaa tietokannasta löytyviin
            //jos kummatkin ovat samat palauttaa true
            if (dbusername.equals(username) && dbpassword.equals(Crypt.crypt(password, dbpassword))) {
                ChatServer.log("User authorized");
                return true;
            }
            createStatement.close();
        }

        return false;
    }

    public void insertMessage(String nick, String message, long sent) throws SQLException {
        //lisää käyttäjännimen, viestin ja lähetysajan tietokantaan
        String insertMessageString = "insert into messages " + "VALUES ('" + nick + "', '" + message + "', '" + sent
                + "')";
        Statement Statement;
        Statement = connection.createStatement();
        Statement.executeUpdate(insertMessageString);
        Statement.close();
        ChatServer.log("Message saved");
    }

    public ArrayList<ChatMessage> readMessages(long since) throws SQLException {
        //luodaan kysely, jossa haetaan uudet viestit annetun ajan jälkeen
        String query = "SELECT nickname, message, timestamp FROM messages WHERE (timestamp > '" + since + "')";
        Statement createStatement;
        createStatement = connection.createStatement();
        ResultSet rs = createStatement.executeQuery(query);
        
        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        
        while (rs.next()) {
            //haetaan tietokannasta viestien tiedot
            String user = rs.getString("nickname");
            String message = rs.getString("message");
            long sent = rs.getLong("timestamp");
            //luodaan chatmessage olio, johon tiedot tallennetaan
            ChatMessage msg = new ChatMessage();
            msg.nick = user;
            msg.message = message;
            msg.setSent(sent);
            //chatmessage olio lisätään chatmessage olioista koostuvaan arraylistiin
            messages.add(msg);
        }
        createStatement.close();
        return messages;
    }

}
