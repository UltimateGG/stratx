package stratx;

import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.CandlestickInterval;

import java.io.Closeable;


public class Simulation {

    public static void main(String[] args) {
        BinanceApiWebSocketClient socket = StratX.API.getWebsocket();

        Closeable eventListener = socket.onCandlestickEvent("ETHUSDT", CandlestickInterval.FIFTEEN_MINUTES, event -> {
            System.out.println(event);
        });
    }
}
