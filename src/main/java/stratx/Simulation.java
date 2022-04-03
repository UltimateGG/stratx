package stratx;

import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import stratx.utils.binance.BinanceClient;

import java.io.Closeable;


public class Simulation {

    public static void main(String[] args) {
        BinanceApiWebSocketClient client = BinanceClient.getWS();

        Closeable eventListener = BinanceClient.getWS().onCandlestickEvent("ETHUSDT", CandlestickInterval.FIFTEEN_MINUTES, event -> {
            System.out.println(event);
        });
    }
}
