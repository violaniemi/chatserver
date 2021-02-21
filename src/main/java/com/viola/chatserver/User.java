package com.viola.chatserver;

import java.util.Hashtable;
import java.util.Map;

public class User {
    String username;
    String password;
    String email;

    public User(){
    this.username = username;
    this.password = password;
    this.email = email;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getPassword(){
        return password;
    }

    public String getUsername(){
        return username;
    }

    public String getEmail(){
        return email;
    }
    
}