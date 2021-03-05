package com.viola.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHandler implements HttpHandler {


    //System.out.println("Request handled in thread" + 
    //Thread.currentThread().getId());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        String responseBody = "";

        try {
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
        } catch (Exception e) {
            code = 500;
            responseBody = "Internal server error" + e.getMessage();
        }

        if (code >= 400) {
            byte[] bytes = responseBody.getBytes("UTF-8");
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
        String responseBody = "";

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
        } else {
            code = 411;
            return code;
        }
        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "No content type in request";
            return code;
        }
        if (contentType.equalsIgnoreCase("application/json")) {
            InputStream stream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
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

    private int processMessage(String text) {
        int code = 200;
        String responseBody = "";

        try {
            JSONObject chatMessage = new JSONObject(text);
            ChatMessage newMessage = new ChatMessage();
            newMessage.nick = chatMessage.getString("user");
            newMessage.message = chatMessage.getString("message");
            String dateStr = chatMessage.getString("sent");
            OffsetDateTime odt = OffsetDateTime.parse(dateStr);
            newMessage.sent = odt.toLocalDateTime();
            long sent = newMessage.dateAsInt();
            String nick = newMessage.nick;
            String message = newMessage.message;
            ChatDatabase.getInstance().insertMessage(nick, message, sent);
            System.out.println("viesti lähetetty");
        } catch (JSONException e) {
            code = 500;
            responseBody = "JSON is not valid" + e.getMessage();
        }
        return code;
    }

    private int handleGetRequest(HttpExchange exchange) throws IOException, SQLException {
        int code = 200;
        JSONArray responseMessages = new JSONArray();
        String responseBody = "";
        ArrayList<ChatMessage> messages;

        DateTimeFormatter httpDateFormatter = DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
        LocalDateTime newest = null;

        Headers requestHeaders = exchange.getRequestHeaders();

        if (requestHeaders.containsKey("If-Modified-Since")) {
            String requestString = requestHeaders.getFirst("If-Modified-Since");
            ZonedDateTime zdTime = ZonedDateTime.parse(requestString, httpDateFormatter);
            LocalDateTime fromWhichDate = zdTime.toLocalDateTime();
            long messagesSince = -1;
            messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();
            messages = ChatDatabase.getInstance().readMessages(messagesSince);
        } else {
            // hakee kaikki viestit, keksi myöhemmin joku rajaus
            messages = ChatDatabase.getInstance().readMessages(0);
        }

        if (messages.isEmpty()) {
            code = 204;
            exchange.sendResponseHeaders(code, -1);
        } else {
            try {
                for (ChatMessage message : messages) {
                    // luodaan json objekti viestille
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("user", message.nick);
                    jsonMessage.put("message", message.message);
                    LocalDateTime date = message.sent;

                    // jos nykyinen aika on uusimman jälkeen, siitä tulee uusin
                    if (newest == null) {
                        newest = date;
                    }
                    if (date.isAfter(newest)) {
                        newest = date;
                    }
                    ZonedDateTime now = ZonedDateTime.of(date, ZoneId.of("UTC"));
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    String dateString = now.format(formatter);
                    jsonMessage.put("sent", dateString);
                    responseMessages.put(jsonMessage);
                }
                if (newest != null) {
                    // formatoi http-date muotoon
                    String newTime = newest.format(httpDateFormatter);
                    Headers headers = exchange.getResponseHeaders();
                    headers.add("Last-Modified", newTime);
                }

                byte[] bytes;
                bytes = responseMessages.toString().getBytes("UTF-8");
                exchange.sendResponseHeaders(code, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (JSONException e) {
                code = 500;
                responseBody = "JSON is not valid" + e.getMessage();
            }

        }
        return code;
    }
}