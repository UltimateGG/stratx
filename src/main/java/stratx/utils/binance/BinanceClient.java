package stratx.utils.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.config.BinanceApiConfig;
import stratx.StratX;
import stratx.utils.Configuration;
import stratx.utils.Mode;

public class BinanceClient {
    private BinanceApiClientFactory factory;
    private BinanceApiRestClient client;
    private BinanceApiWebSocketClient ws_client;

    public BinanceClient(Configuration config) {
        if (config.getBoolean("binance.isUS", false))
            BinanceApiConfig.setBaseDomain("binance.us");

        client = login(config.getString("binance.api-key"), config.getString("binance.api-secret"));
    }

    private BinanceApiRestClient login(String apiKey, String secretKey) {
        factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
        BinanceApiRestClient newClient = factory.newRestClient();

        if (StratX.MODE != Mode.BACKTEST && StratX.MODE != Mode.DOWNLOAD) {
            try {
                if (!newClient.getAccount().isCanTrade())
                    throw new Exception("Binance account cannot trade");
            } catch (Exception e) {
                StratX.error("Failed to login to Binance", e);
                System.exit(1);
            }
        }

        return newClient;
    }

    public BinanceApiRestClient get() {
        return client;
    }

    public BinanceApiWebSocketClient getWS() {
        if (ws_client == null)
            ws_client = factory.newWebSocketClient();

        return ws_client;
    }
}
