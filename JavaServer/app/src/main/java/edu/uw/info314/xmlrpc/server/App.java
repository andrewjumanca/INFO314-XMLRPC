package edu.uw.info314.xmlrpc.server;

import java.util.*;
import java.util.logging.*;
import static spark.Spark.*;

class Call {
    public String name;
    public List<Object> args = new ArrayList<Object>();
}

public class App {
    public static final Logger LOG = Logger.getLogger(App.class.getCanonicalName());
    private static final HashMap<Integer, String> FAULTCODES;
    static {
        FAULTCODES = new HashMap<Integer, String>();
        FAULTCODES.put(400, "<?xml version=\"1.0\"?>" +
        "<methodResponse>" +
        "    <fault>" +
        "        <value>" +
        "            <struct>" +
        "                <member>" +
        "                    <name>faultCode</name>" +
        "                    <value><int>400</int></value>" +
        "                </member>" +
        "                <member>" +
        "                    <name>faultString</name>" +
        "                    <value><string>Bad Request: Too many parameters or invalid method.</string></value>" +
        "                </member>" +
        "            </struct>" +
        "        </value>" +
        "    </fault>" +
        "</methodResponse>");

        FAULTCODES.put(413, "<?xml version=\"1.0\"?>" +
        "<methodResponse>" +
        "    <fault>" +
        "        <value>" +
        "            <struct>" +
        "                <member>" +
        "                    <name>faultCode</name>" +
        "                    <value><int>413</int></value>" +
        "                </member>" +
        "                <member>" +
        "                    <name>faultString</name>" +
        "                    <value><string>Payload too large: Overflow</string></value>" +
        "                </member>" +
        "            </struct>" +
        "        </value>" +
        "    </fault>" +
        "</methodResponse>");

        FAULTCODES.put(403, "<?xml version=\"1.0\"?>" +
        "<methodResponse>" +
        "    <fault>" +
        "        <value>" +
        "            <struct>" +
        "                <member>" +
        "                    <name>faultCode</name>" +
        "                    <value><int>403</int></value>" +
        "                </member>" +
        "                <member>" +
        "                    <name>faultString</name>" +
        "                    <value><string>Forbidden: Cannot divide by zero</string></value>" +
        "                </member>" +
        "            </struct>" +
        "        </value>" +
        "    </fault>" +
        "</methodResponse>");
    }
    private static ArrayList<String> methodTypes = new ArrayList<String>();
    static {
        methodTypes.add("add");
        methodTypes.add("subtract");
        methodTypes.add("multiply");
        methodTypes.add("divide");
        methodTypes.add("modulo");
    }

    public static HashMap<String, Object> parseRequest(spark.Request req, spark.Response res) throws NumberFormatException {
        HashMap<String, Object> result = new HashMap<>();
        String body = req.body();
        
        if (!body.startsWith("<?xml")) {
            result.put("error", "Invalid request body format");
            return result;
        }
    
        String methodName = body.substring(body.indexOf("<methodName>") + 12, body.indexOf("</methodName>"));
        result.put("method", methodName);
        
        if (methodTypes.contains(methodName)) {
            List<Integer> digits = new ArrayList<>();
            int startIndex = body.indexOf("<i4>");
            int endIndex = body.indexOf("</i4>", startIndex);
            while (startIndex >= 0 && endIndex > startIndex) {
                String digitStr = body.substring(startIndex + 4, endIndex).trim();
                if (digitStr.length() > 9) {
                    result.put("error", FAULTCODES.get(413));
                    return result;
                }
                int digit = Integer.parseInt(digitStr);
                digits.add(digit);  
                startIndex = body.indexOf("<i4>", endIndex);
                endIndex = body.indexOf("</i4>", startIndex);
            }

            int[] params = new int[digits.size()];
            for (int i = 0; i < digits.size(); i++) {
                params[i] = digits.get(i);
            }

            result.put("params", params);

            if (result.get("method").equals("divide")) {
                int[] divisors = (int[]) result.get("params");
                if (divisors[1] == 0) {
                    result.put("error", FAULTCODES.get(403));
                    return result;
                }
            }
        } else {
            result.put("error", FAULTCODES.get(400));
            return result;
        }
        return result;
    }
    
    private static String buildXMLResponse(int[] params, String methodType, spark.Response res) {
        Calc calc = new Calc();

        switch (methodType) {
            case "add":
                for (int i = 0; i < params.length; i++) {
                    if (Integer.toString(params[i]).length() > 9) {
                        res.status(413);
                        return FAULTCODES.get(413);
                    }
                }
                res.status(200);
                return "<?xml version=\"1.0\"?>\n" +
                        "<methodResponse>\n" +
                        "    <params>\n" +
                        "        <param>\n" +
                        "            <value><i4>" + String.valueOf(calc.add(params)) + "</i4>\n" +
                        "        </param>\n" +
                        "    </params>\n" +
                        "</methodResponse>\n";

            case "subtract":
                if (params.length > 2) {
                    res.status(400);
                    return FAULTCODES.get(400);
                }
                res.status(200);
                return "<?xml version=\"1.0\"?>\n" +
                        "<methodResponse>\n" +
                        "    <params>\n" +
                        "        <param>\n" +
                        "            <value><i4>" + String.valueOf(calc.subtract(params[0], params[1])) + "</i4>\n" +
                        "        </param>\n" +
                        "    </params>\n" +
                        "</methodResponse>\n";

            case "multiply":
                for (int i = 0; i < params.length; i++) {
                    if (Integer.toString(params[i]).length() >= 9) {
                        res.status(413);
                        return FAULTCODES.get(413);
                    }
                }
                res.status(200);
                return "<?xml version=\"1.0\"?>\n" +
                        "<methodResponse>\n" +
                        "    <params>\n" +
                        "        <param>\n" +
                        "            <value><i4>" + String.valueOf(calc.multiply(params)) + "</i4>\n" +
                        "        </param>\n" +
                        "    </params>\n" +
                        "</methodResponse>\n";

            case "divide":
                if (params.length > 2) {
                    halt(400, "Bad Request: This operation only accepts two parameters");
                }
                return "<?xml version=\"1.0\"?>\n" +
                        "<methodResponse>\n" +
                        "    <params>\n" +
                        "        <param>\n" +
                        "            <value><i4>" + String.valueOf(calc.divide(params[0], params[1])) + "</i4>\n" +
                        "        </param>\n" +
                        "    </params>\n" +
                        "</methodResponse>\n";

            case "modulo":
                if (params.length > 2) {
                    halt(400, "Bad Request: This operation only accepts two parameters");
                }
                return "<?xml version=\"1.0\"?>\n" +
                        "<methodResponse>\n" +
                        "    <params>\n" +
                        "        <param>\n" +
                        "            <value><i4>" + String.valueOf(calc.modulo(params[0], params[1])) + "</i4>\n" +
                        "        </param>\n" +
                        "    </params>\n" +
                        "</methodResponse>\n";
            case "error":
                // return 
                break;
            default:
                break;
        }
        // calc.add(params[0], params[1]);
        // calc.subtract(params[0], params[1]);
        // calc.multiply(params);
        // calc.divide(params[0], params[1]);
        // calc.modulo(params[0], params[1]);
        return null;
    }

    public static void main(String[] args) {
        LOG.info("Starting up server on specified port, or port 4567 by default!");
        port(8080);

        // Return a 404 for any URL other than "/RPC"
        before((req, res) -> {
            if (!req.uri().equals("/RPC")) {
                String body = "<?xml version=\"1.0\"?>" +
                "<methodResponse>" +
                "    <fault>" +
                "        <value>" +
                "            <struct>" +
                "                <member>" +
                "                    <name>faultCode</name>" +
                "                    <value><int>404</int></value>" +
                "                </member>" +
                "                <member>" +
                "                    <name>faultString</name>" +
                "                    <value><string>Not Found.</string></value>" +
                "                </member>" +
                "            </struct>" +
                "        </value>" +
                "    </fault>" +
                "</methodResponse>";
                halt(404, body);
            }
        });

        // Return a 405 (Method Not Supported) for any operation other than POST
        before((req, res) -> {
            if (!req.requestMethod().equals("POST")) {
                String body = "<?xml version=\"1.0\"?>" +
                "<methodResponse>" +
                "    <fault>" +
                "        <value>" +
                "            <struct>" +
                "                <member>" +
                "                    <name>faultCode</name>" +
                "                    <value><int>405</int></value>" +
                "                </member>" +
                "                <member>" +
                "                    <name>faultString</name>" +
                "                    <value><string>Method Not Allowed</string></value>" +
                "                </member>" +
                "            </struct>" +
                "        </value>" +
                "    </fault>" +
                "</methodResponse>";
                halt(405, body);
            }
        });

        // The Host must reflect the hostname it is running on
        before((req, res) -> {
            String host = req.host();
            res.header("Host", host);
        });

        // Start the server
        init();
        LOG.info("Server started on port 8080");
        post("/RPC", (req, res) -> {
            try {
                HashMap<String, Object> operationInfo = parseRequest(req, res);
                if (operationInfo.equals(null) | operationInfo.containsKey("error")) {
                    return operationInfo.get("error");
                } else {
                    // Testing parameter cleanliness:
                    // System.out.println(operationInfo.get("method"));
                    // System.out.println(Arrays.toString((int[]) operationInfo.get("digits")));

                    String methodType = (String) operationInfo.get("method");
                    int[] params = (int[]) operationInfo.get("params");

                    return buildXMLResponse(params, methodType, res);

                }
            } catch(NumberFormatException nfe) {
                halt(413, FAULTCODES.get(413));
            } catch (Exception e) {
                halt(400, FAULTCODES.get(400));
            }
            
            res.status(418);
            return "I'm a teapot";
        });
    }
}
