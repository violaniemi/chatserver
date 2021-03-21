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

    final ChatAuthenticator auth;

    public RegistrationHandler(ChatAuthenticator authenticator) {
        this.auth = authenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
       
        System.out.println("Request handled in thread" + Thread.currentThread().getId());

        String responseBody = "";
        int code = 200;
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";
                //tarkistetaan, että content length löytyy
                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                } else {
                    code = 411;
                }
                //tarkistetaan, että content type löytyy
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    code = 400;
                    responseBody = "No content type in request\n";
                }
                //tarkisteaan, että content type on application/json
                if (contentType.equalsIgnoreCase("application/json")) {
                    InputStream stream = exchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining("\n"));
                    stream.close();
                    //luodaan jsonobjekti
                    JSONObject registrationMsg = new JSONObject(text);

                    //tarkistetaan, että jsonista löytyy kaikki elementit                                                                                      
                    if (text.contains("username") && text.contains("password") && text.contains("email")) {  
                        String username = registrationMsg.getString("username");
                        String password = registrationMsg.getString("password");
                        String email = registrationMsg.getString("email");
                        // tarkistetaan ettei mikään info ole tyhjä
                        if (!username.equals(" ") && !password.equals(" ") && !email.equals(" ") && !username.equals("") 
                        && !password.equals("") && !email.equals("") && !username.equals("  ") && !password.equals("  ")
                        && !email.equals("  ")) { 
                            User user = new User();
                            user.setPassword(password);
                            user.setUsername(username);
                            user.setEmail(email);
                            boolean b = auth.addUser(username, user);
                            if (b == true){
                                ChatServer.log("User added");
                            }else{
                                code = 409;
                                responseBody = "User already exists";
                            }
                        } else {
                            code = 400;
                            responseBody = "Registration info can't be empty";
                        }
                    } else {
                        code = 400;
                        responseBody = "Json doesn't have required elements";
                    }
                } else {
                    code = 400;
                    responseBody = "Content-Type must be application/json\n";
                }
            } else {
                code = 400;
                responseBody = "Not supported\n";
            }
        } catch (JSONException e) {
            code = 400;
            responseBody = "JSON is not valid\n" + e.getMessage();
        } catch (IOException e) {
            code = 500;
            responseBody = "Error in handling the request: \n" + e.getMessage();
        } catch (Exception e) {
            code = 500;
            responseBody = "Server error: \n" + e.getMessage();
        }

        byte[] bytes = responseBody.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();

    }

}
