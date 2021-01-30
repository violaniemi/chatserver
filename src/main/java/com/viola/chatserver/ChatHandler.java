package com.viola.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;



public class ChatHandler implements HttpHandler {

    private String responseBody = "";
    private ArrayList<String> messages = new ArrayList<String>();

    @Override
    public void handle(HttpExchange exchange) throws IOException{
        int code = 200;

        try{
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessage(exchange);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequest(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error in handling the request" + e.getMessage();
        } catch(Exception e){
            code = 500;
            responseBody = "Internal server error" + e.getMessage();
        } 

        if (code >= 400){
            byte [] bytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }
   
    }

    private int handleChatMessage(HttpExchange exchange) throws Exception {
        int code = 200;
        Headers headers = exchange.getRequestHeaders();
        int contentLength = 0;
        String contentType = "";
        if(headers.containsKey("Content-Length")){
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
        } else {
            code = 411;
            return code;
        }
        if (headers.containsKey("Content-Type")){
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "No content type in request";
            return code;
        }
        if (contentType.equalsIgnoreCase("text/plain")){
            InputStream stream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            stream.close();
            if (text.length() > 0) {
                processMessage(text);
                exchange.sendResponseHeaders(code, -1);
            } else {
                code = 400;
                responseBody = "No content in request";
            }
        } else {
            code = 411;
            responseBody = "Content-Type must be text/plain";
        }
        return code;
    }

    private void processMessage (String text) {
        messages.add(text);
    }

    private int handleGetRequest(HttpExchange exchange) throws IOException, SQLException {
        int code = 200;

        if (messages.isEmpty()) {
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            return code;
        }
        responseBody = "";
        for (String message : messages){
            responseBody += message + "\n";
        }

        byte [] bytes;
        bytes = responseBody.toString().getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
        return code;
    }
}