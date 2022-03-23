package stratx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.istack.internal.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.exceptions.LoaderParseException;
import stratx.utils.Candlestick;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Loader {
    private static Loader INSTANCE;
    private static final boolean HEIKIN_ASHI_CANDLES = true;
    private File dataFile;
    private static final Logger LOGGER = LogManager.getLogger("Loader");


    public static List<Candlestick> loadData(@NotNull String file) {
        return loadData(new File(file));
    }

    public static List<Candlestick> loadData(@NotNull File file) {
        try {
            return doLoad(file);
        } catch (Exception e) {
            LOGGER.error("Error while loading data: ", e);
        }

        return new ArrayList<>();
    }

    private static List<Candlestick> doLoad(File file) throws LoaderParseException {
        if (file == null || !file.exists())
            throw new LoaderParseException("File does not exist");

        if (!file.getName().endsWith(".json"))
            throw new LoaderParseException("File is not a JSON file");

        Loader.INSTANCE = new Loader();
        Loader.INSTANCE.dataFile = file;

        ArrayList<Candlestick> data = new ArrayList<>();
        Loader.INSTANCE.loadJSON(data);

        if (data.size() == 0)
            throw new LoaderParseException("Invalid price data file or format");

        if (HEIKIN_ASHI_CANDLES && data.size() > 1) {
            for (int i = 0; i < data.size(); i++) {
                if (i == 0) continue;
                data.get(i).setPrevious(data.get(i - 1));
            }
        }

        System.gc(); // Clear all strings from loading the file
        return Collections.unmodifiableList(data);
    }

    private Loader() {}

    private void loadJSON(ArrayList<Candlestick> dataPoints) {
        try {
            JsonObject data = new JsonParser().parse(new FileReader(this.dataFile)).getAsJsonObject();
            JsonObject opens = data.getAsJsonObject("Open");
            JsonObject highs = data.getAsJsonObject("High");
            JsonObject lows = data.getAsJsonObject("Low");
            JsonObject closes = data.getAsJsonObject("Close");
            JsonObject volumes = data.getAsJsonObject("Volume");

            for (String key : data.getAsJsonObject("Open").keySet()) {
                dataPoints.add(new Candlestick(
                        key,
                        opens.get(key).getAsDouble(),
                        highs.get(key).getAsDouble(),
                        lows.get(key).getAsDouble(),
                        closes.get(key).getAsDouble(),
                        volumes.get(key).getAsLong()
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading data: ", e);
        }
    }
}
