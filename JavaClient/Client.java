import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This approach uses the java.net.http.HttpClient classes, which
 * were introduced in Java11.
 */

public class Client {

    private static String serverName;
    private static String serverPort;

    private static int getResult(String responseBody) throws IllegalStateException {
        if (responseBody.contains("<fault>")) {
            Pattern faultCode = Pattern.compile("<int>(.+?)</int>");
            Matcher faultCodeMatcher = faultCode.matcher(responseBody);
            String faultCodeNumber = "";
            if (faultCodeMatcher.find()) {
                faultCodeNumber = faultCodeMatcher.group(1);
            }

            Pattern faultString = Pattern.compile("<string>(.+?)</string>");
            Matcher faultStringMatcher = faultString.matcher(responseBody);
            String faultStringMessage = "";
            if (faultStringMatcher.find()) {
                faultStringMessage = faultStringMatcher.group(1);
            }            
            
            throw new IllegalStateException("Error code: " + String.valueOf(faultCodeNumber) + ", Message: " + faultStringMessage);
        }
        
        int startIndex = responseBody.indexOf("<i4>") + 4;
        int endIndex = responseBody.indexOf("</i4>");
        return Integer.parseInt(responseBody.substring(startIndex, endIndex));
    }
    
    public static void main(String... args) throws Exception {
        serverName = "localhost";//args[0];
        serverPort = "8080";//args[1];        
        
        // Should print all true
        System.out.println(add() == 0);
        System.out.println(add(1, 2, 3, 4, 5) == 15);
        System.out.println(add(2, 4) == 6);
        System.out.println(subtract(12, 6) == 6);
        System.out.println(multiply(3, 4) == 12);
        System.out.println(multiply(1, 2, 3, 4, 5) == 120);
        System.out.println(divide(10, 5) == 2);
        System.out.println(modulo(10, 5) == 0);

        // Should throw exceptions for these:
        System.out.println(divide(1, 0));
        System.out.println(multiply(999, 999999999));
    }

    private static int add() {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;
        try {
            int[] digits = {0, 0};
            output = client.parseResponse(client.generateRequest("add", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return getResult(output);
        } catch (IllegalStateException ise) {
            System.out.println(ise.getMessage());
            return -1;
        }
    }

    public static int add(int lhs, int rhs) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;
        try {
            int[] digits = {lhs, rhs};
            output = client.parseResponse(client.generateRequest("add", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResult(output);
    }

    public static int add(Integer... params) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;

        int[] digits = toIntArray(params);

        try {
            output = client.parseResponse(client.generateRequest("add", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getResult(output);
    }
    public static int subtract(int lhs, int rhs) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;
        try {
            int[] digits = {lhs, rhs};
            output = client.parseResponse(client.generateRequest("subtract", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResult(output);
    }
    public static int multiply(int lhs, int rhs) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;
        try {
            int[] digits = {lhs, rhs};
            output = client.parseResponse(client.generateRequest("multiply", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResult(output);
    }
    public static int multiply(Integer... params) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;

        int[] digits = toIntArray(params);

        try {
            output = client.parseResponse(client.generateRequest("multiply", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResult(output);
    }
    public static int divide(int lhs, int rhs) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;
        try {
            int[] digits = {lhs, rhs};
            output = client.parseResponse(client.generateRequest("divide", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return getResult(output);
        } catch (IllegalStateException ise) {
            System.out.println(ise.getMessage());
            return -1;
        }
    }
    public static int modulo(int lhs, int rhs) throws Exception {
        ClientHttp client = new ClientHttp(serverName, serverPort);
        String output = null;
        try {
            int[] digits = {lhs, rhs};
            output = client.parseResponse(client.generateRequest("modulo", digits));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResult(output);
    }
    public static int[] toIntArray(Integer... integers) {
        int[] ints = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
            ints[i] = integers[i];
        }
        return ints;
    }
}

class ClientHttp {

    private final URI serverUri;
    private final HttpClient client;

    public ClientHttp(String serverName, String serverPort) {
        this.client = HttpClient.newHttpClient();
        this.serverUri = URI.create("http://" + serverName + ":" + serverPort + "/RPC");
    }

    public HttpResponse<String> generateRequest(String methodName, int[] params) {
        String digits = "";
        for (int i = 0; i < params.length; i++) {
            digits += "<value><i4>" + String.valueOf(params[i]) + "</i4></value>\n";
        }

        String xmlRequestBody = "<?xml version=\"1.0\"?>" +
                                "<methodCall>" +
                                    "<methodName>" + methodName + "</methodName>\n" +
                                        "<params>\n" +
                                            "<param>\n" +
                                                digits +
                                            "</param>\n" +
                                        "</params>\n" +
                                    "</methodCall>";
    
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.serverUri)
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(xmlRequestBody))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String parseResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        // Handle response based on status code
        if (true) {
            return responseBody;
        } else {
            throw new RuntimeException("Request failed with status code " + statusCode + " and response body " + responseBody);
        }
    }
}


