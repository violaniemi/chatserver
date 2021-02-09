package com.viola.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class ChatHandler implements HttpHandler {

    private String responseBody = "";
    private ArrayList<ChatMessage> messages;

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
        if (contentType.equalsIgnoreCase("application/json")){
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
            responseBody = "Content-Type must be application/json";
        }
        return code;
    }

    private void processMessage (String text) {
        ChatMessage newMessage = new ChatMessage();
        try{
        JSONObject chatMessage = new JSONObject(text);
        String dateStr = chatMessage.getString("sent");
        OffsetDateTime odt = OffsetDateTime.parse(dateStr);
        newMessage.sent = odt.toLocalDateTime();
        messages.add(newMessage);
        Collections.sort(messages, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage lhs, ChatMessage rhs) {
            return lhs.sent.compareTo(rhs.sent);
            }
            });
        } catch (JSONException e) {
            int code = 500;
            responseBody = "JSON is not valid" +e.getMessage();
        }
    }

    private int handleGetRequest(HttpExchange exchange) throws IOException, SQLException {
        int code = 200;

        if (messages.isEmpty()) {
            code = 204;
            exchange.sendResponseHeaders(code, -1);
        } else {
            try{
                JSONArray responseMessages = new JSONArray();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMdd'T'HH:mm:ss.SSSX");
                for (ChatMessage message : messages){
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("user", message.nick);
                    jsonMessage.put("message", message.message);
                    jsonMessage.put("sent", message.sent);
                    responseMessages.put(jsonMessage);
                }
            } catch(JSONException e){
                code = 500;
                responseBody = "JSON is not valid" +e.getMessage();
            }
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