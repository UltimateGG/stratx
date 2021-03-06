package stratx;

import com.sun.istack.internal.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.utils.Candlestick;
import stratx.utils.Utils;

import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Loader {
    private static Loader INSTANCE;
    private File dataFile;
    private static final Logger LOGGER = LogManager.getLogger("Loader");
    public static String lastDataRange = "";


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

    private static List<Candlestick> doLoad(File file) throws ParseException {
        if (file == null || !file.exists())
            throw new ParseException("File does not exist");

        if (!file.getName().endsWith(".strx"))
            throw new ParseException("File is not a strx file");

        Loader.INSTANCE = new Loader();
        Loader.INSTANCE.dataFile = file;

        ArrayList<Candlestick> data = new ArrayList<>();
        Loader.INSTANCE.load(data);

        if (data.size() == 0)
            throw new ParseException("Invalid price data file or format");

        System.gc(); // Clear all strings from loading the file
        return Collections.unmodifiableList(data);
    }

    private Loader() {}

    private void load(ArrayList<Candlestick> dataPoints) {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(this.dataFile.toPath()))) {
            // the magic string is 'b4 ff b4 ff' + the version number
            if (input.readUnsignedByte() != 0xb4
                    || input.readUnsignedByte() != 0xff
                    || input.readUnsignedByte() != 0xb4
                    || input.readUnsignedByte() != 0xff
                    || input.readUnsignedByte() != 0x01) {
                throw new ParseException("Not a valid strx file! (Or outdated version)");
            }

            long startTime = input.readLong();
            long endTime = input.readLong();
            lastDataRange = String.format("Price data range: %s", Utils.msToNice(endTime - startTime, true, false, false));
            LOGGER.info(lastDataRange);
            Candlestick previous = null;

            while (input.available() > 0) {
                Candlestick candle = new Candlestick(
                        input.readLong(),
                        input.readDouble(),
                        input.readDouble(),
                        input.readDouble(),
                        input.readDouble(),
                        input.readLong(),
                        previous
                );

                dataPoints.add(candle);
                previous = candle;
            }
        } catch (Exception e) {
            LOGGER.error("Error while loading data: ", e);
        }
    }

    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }
}
