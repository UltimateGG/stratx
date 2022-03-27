package stratx.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Date;

public class LogFile {
    private final File logFile;

    public LogFile(String logFileName, String priceFile) {
        this(logFileName + "-" + priceFile.split("_")[0] + "_" + priceFile.split("_")[1]);
    }

    public LogFile(String type) {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String fileName = String.format("logs/%s-%s.%s.%s", type, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR));
        if (new File(fileName + ".csv").exists()) {
            int index = 1;
            while (new File(fileName + "_" + index + ".csv").exists())
                index++;
            fileName += "_" + index;
        }

        fileName += ".csv";
        logFile = new File(fileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create log file", e);
            }
        }
    }

    public void write(String line) {
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write((line + "\n").getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to log file", e);
        }
    }
}
