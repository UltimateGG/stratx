package stratx.utils;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.config.BinanceApiConfig;
import stratx.StratX;

public class BinanceClient {
    private BinanceApiClientFactory factory;
    private final BinanceApiRestClient CLIENT;
    private final BinanceApiWebSocketClient WEBSOCKET;


    public BinanceClient(Configuration config) {
        if (config.getBoolean("binance.isUS", false))
            BinanceApiConfig.setBaseDomain("binance.us");

        CLIENT = login(config.getString("binance.api-key"), config.getString("binance.api-secret"));
        WEBSOCKET = factory.newWebSocketClient();
    }

    private BinanceApiRestClient login(String apiKey, String secretKey) {
        StratX.log("Logging into Binance...");

        factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
        BinanceApiRestClient newClient = factory.newRestClient();

        if (StratX.MODE.requiresMarketDataStream()) {
            try {
                if (!newClient.getAccount().isCanTrade())
                    throw new Exception("Binance account cannot trade");
            } catch (Exception e) {
                StratX.error("Failed to login to Binance", e);
                System.exit(1);
            }
        }

        StratX.log("Login successful");
        return newClient;
    }

    public BinanceApiRestClient get() {
        return CLIENT;
    }

    public BinanceApiWebSocketClient getWebsocket() {
        return WEBSOCKET;
    }
}
