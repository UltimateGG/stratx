package stratx;

import com.google.gson.*;
import com.sun.istack.internal.NotNull;
import stratx.exceptions.LoaderParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.utils.Candlestick;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Loader {
    private static Loader INSTANCE;
    private static boolean isCSV = false;
    private static boolean isJSON = false;
    private File dataFile;
    private static final Logger LOGGER = LogManager.getLogger("Loader");


    public static List<Candlestick> loadData(@NotNull String file) {
        return loadData(new File(file));
    }

    public static List<Candlestick> loadData(@NotNull Path file) {
        return loadData(file.toFile());
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

        isCSV = file.getName().endsWith(".csv");
        isJSON = file.getName().endsWith(".json");

        if (!isCSV && !isJSON)
            throw new LoaderParseException("File is not a CSV or JSON file");

        Loader.INSTANCE = new Loader();
        Loader.INSTANCE.dataFile = file;

        ArrayList<Candlestick> data = new ArrayList<>();
        if (isCSV) Loader.INSTANCE.loadCSV(data);
        else Loader.INSTANCE.loadJSON(data);

        if (data.size() == 0)
            throw new LoaderParseException("Invalid price data file or format");

        System.gc(); // Clear all strings from loading the file
        return Collections.unmodifiableList(data);
    }

    private Loader() {}

    private void loadCSV(ArrayList<Candlestick> dataPoints) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new java.io.FileReader(this.dataFile));
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                if (line.equals("")) break;
                String[] data = line.split(",");
                if (data.length < 6)
                    throw new IllegalArgumentException("Invalid CSV file");

                if (lineNumber == 0) {
                    if (!data[0].equalsIgnoreCase("Date")
                            || !data[1].equalsIgnoreCase("Open")
                            || !data[2].equalsIgnoreCase("High")
                            || !data[3].equalsIgnoreCase("Low")
                            || !data[4].equalsIgnoreCase("Close")
                            || !data[data.length - 1].equalsIgnoreCase("Volume"))
                        throw new IllegalArgumentException("Invalid CSV file");

                    ++lineNumber;
                    continue;
                }

                dataPoints.add(new Candlestick(
                        data[0], // Date TODO: Parse date to long ms
                        Double.parseDouble(data[1]), // Open
                        Double.parseDouble(data[2]), // High
                        Double.parseDouble(data[3]), // Low
                        Double.parseDouble(data[4]), // Close
                        Long.parseLong(data[data.length - 1]) // Volume (Should always be last in case of adj close val)
                ));
                ++lineNumber;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing file: ", e);
                }
            }
        }
    }

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
