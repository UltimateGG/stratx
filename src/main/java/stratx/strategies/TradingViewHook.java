package stratx.strategies;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import stratx.StratX;
import stratx.utils.Signal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class TradingViewHook extends Strategy {
    private static final int PORT = 80;
    private static final String[] ALLOWED_IPS = {
            "52.89.214.238",
            "34.212.75.30",
            "54.218.53.128",
            "52.32.178.7"
    };

    private int threads = 0;

    public TradingViewHook(String configFile) {
        super("TradingViewHook", configFile);

        startServer();
    }

    @Override
    public Signal getSignal() { return Signal.HOLD; }

    public void onWebhookMessage(JsonObject msg) {
        String action = msg.get("action").getAsString().toUpperCase();
        String symbol = msg.get("ticker").getAsString();
        double price = msg.get("price").getAsDouble();

        if (!("BUY".equals(action) || "SELL".equals(action)) || price < 0.0D) return;
        StratX.getLogger().info("Webhook signal: " + action + " " + symbol + " " + price);

        if (!StratX.getCurrentMode().getCoin().equals(symbol)) {
            StratX.getLogger().info("Webhook signal ignored (Wrong symbol)");
            return;
        }

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
                        if (!isAllowedIP(socket)) continue;
                        StratX.getLogger().info("TradingViewHook server accepted connection from " + socket.getInetAddress().getHostAddress());

                        InputStream inputStream = socket.getInputStream();

                        HashMap<String, String> headers = new HashMap<>();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String httpRequest = reader.readLine();
                        while(!httpRequest.isEmpty()) {
                            if (headers.size() == 0) {
                                headers.put("method", httpRequest.split(" ")[0]);
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

                        int contentLength = Integer.parseInt(headers.get("content-length"));
                        if (contentLength > 1024 * 1024) continue;
                        char[] content = new char[contentLength];
                        reader.read(content);

                        String json = new String(content);
                        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                        hook.onWebhookMessage(jsonObject);

                        OutputStream writer = socket.getOutputStream();
                        writer.write("HTTP/1.1 200 OK\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                        writer.write("OK".getBytes(StandardCharsets.UTF_8));
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
