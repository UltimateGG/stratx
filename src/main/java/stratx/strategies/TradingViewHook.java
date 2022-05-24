package stratx.strategies;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import stratx.StratX;
import stratx.utils.Signal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TradingViewHook extends Strategy {
    private static final int PORT = 80;
    /** TradingView IPs */
    private static final String[] ALLOWED_IPS = {
            "52.89.214.238",
            "34.212.75.30",
            "54.218.53.128",
            "52.32.178.7",
            StratX.DEVELOPMENT_MODE ? "0:0:0:0:0:0:0:1" : null
    };

    private int threads = 0;

    public TradingViewHook(String configFile) {
        super("TradingViewHook", configFile);

        startServer();
    }

    @Override
    public Signal getSignal() { return Signal.HOLD; }

    public void onWebhookMessage(JsonObject msg) throws Exception {
        String action = msg.get("action").getAsString().toUpperCase();
        String symbol = msg.get("ticker").getAsString();
        double price = msg.get("price").getAsDouble();
        String source = msg.get("source").getAsString();

        if (!("BUY".equals(action) || "SELL".equals(action)) || price < 0.0D) return;
        StratX.getLogger().info("Webhook signal ({}): {} {} {}", source, action, symbol, price);

        if (!StratX.getCurrentMode().getCoin().toString().equals(symbol)) {
            StratX.getLogger().info("Webhook signal ignored (Wrong symbol)");
            throw new Exception("Incorrect symbol");
        }

        if (!StratX.getCurrentMode().isConnectedToMarket())
            throw new Exception("Not yet connected to market");

        if (action.equals("BUY")) StratX.getCurrentMode().forceBuy();
        else StratX.getCurrentMode().forceSell();
    }

    private void startServer() {
        threads++;

        if (threads > 8) {
            StratX.getLogger().error("Too many threads/server restarts. Exiting.");
            System.exit(1);
            return;
        }

        TradingViewHook hook = this;
        new Thread(() -> {
            try {
                Server.start(hook);
            } catch (IOException e) {
                StratX.error("Exception during TradingViewHook server", e);
                startServer();
            }
        }).start();
    }

    private static final class Server {
        public static void start(TradingViewHook hook) throws IOException {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                StratX.getLogger().info("TradingViewHook server started on port " + PORT);

                while (true) {
                    try (Socket socket = serverSocket.accept()) {
                        if (!isAllowedIP(socket)) {
                            socket.close();
                            continue;
                        }

                        StratX.getLogger().info("TradingViewHook server accepted connection from " + socket.getInetAddress().getHostAddress());

                        HashMap<String, String> headers = new HashMap<>();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String httpRequest = reader.readLine();
                        while (!httpRequest.isEmpty()) {
                            if (headers.size() == 0) {
                                headers.put("method", httpRequest.split(" ")[0]);
                                headers.put("path", httpRequest.split(" ")[1]);
                                httpRequest = reader.readLine();
                                continue;
                            }
                            String[] header = httpRequest.split(": ");
                            headers.put(header[0].toLowerCase(), header[1]);
                            httpRequest = reader.readLine();
                        }

                        if (!headers.containsKey("method") || !"POST".equals(headers.get("method")) || !headers.containsKey("content-length") || !headers.containsKey("content-type")
                            || !headers.get("content-type").contains("application/json")) {
                            StratX.getLogger().error("TradingViewHook server received invalid request");
                            continue;
                        }

                        if (!headers.get("path").equals("/tradehook")) {
                            StratX.getLogger().error("TradingViewHook server received invalid request with path of: " + headers.get("path"));
                            continue;
                        }

                        int contentLength = Integer.parseInt(headers.get("content-length"));
                        if (contentLength > 1024 * 1024) continue;
                        char[] content = new char[contentLength];
                        reader.read(content);

                        String json = new String(content);
                        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                        String status = "200 OK";
                        String response = "{ \"submitted\": true }";

                        try {
                            hook.onWebhookMessage(jsonObject);
                        } catch (Exception msg) {
                            status = "500 Internal Server Error";
                            response = "{ \"error\": \"" + msg.getMessage() + "\" }";
                        }

                        OutputStream writer = socket.getOutputStream();
                        writer.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        writer.write(response.getBytes(StandardCharsets.UTF_8));
                        writer.close();
                    } catch (Exception e) { // malformed request, etc.
                        StratX.error("Exception during TradingViewHook server", e);
                    }
                }
            }
        }
    }

    private static boolean isAllowedIP(Socket conn) {
        String ip = conn.getInetAddress().getHostAddress();

        for (String allowedIP : ALLOWED_IPS)
            if (allowedIP.equals(ip)) return true;

        return false;
    }
}
