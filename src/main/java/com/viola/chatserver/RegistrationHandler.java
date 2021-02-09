package com.viola.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator auth = null;

    public RegistrationHandler(ChatAuthenticator authenticator) {
        this.auth = authenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String responseBody = "";
        int code = 200;
        try{
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")){
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";
                if(headers.containsKey("Content-Length")){
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                } else {
                    code = 411;
                }
                if (headers.containsKey("Content-Type")){
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    code = 400;
                    responseBody = "No content type in request\n";
                }
                if (contentType.equalsIgnoreCase("application/json")){
                    InputStream stream = exchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                    stream.close();
                    try {
                    JSONObject registrationMsg = new JSONObject(text);
                    String username = registrationMsg.getString("username");
                    String password = registrationMsg.getString("password");
                    String email = registrationMsg.getString("email");
                    User user = new User();
                    user.setPassword(password);
                    user.setUsername(username);
                    user.setEmail(email);
                    auth.addUser(username, user);
                    } catch (JSONException e) {
                        code = 500;
                        responseBody = "JSON is not valid\n" +e.getMessage();
                    }
            } else {
                code = 411;
                responseBody = "Content-Type must be application/json\n";
            }
        } else {
            code = 400;
            responseBody = "Not supported\n";
        }
    } catch (IOException e) {
        code = 500;
        responseBody = "Error in handling the request: \n" + e.getMessage();
    } catch (Exception e) {
        code = 500;
        responseBody = "Server error: \n" + e.getMessage();
    }
    if(code >= 400){
        byte [] bytes = responseBody.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
        }
    }
    
} 

