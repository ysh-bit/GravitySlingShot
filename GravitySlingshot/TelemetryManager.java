import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TelemetryManager {
    private static final String LOG_FILE = "slingshot_telemetry.txt";

    public static class MissionRecord {
        public final String timestamp;
        public final int level;
        public final int score;
        public final String status;

        public MissionRecord(int level, int score, String status) {
            this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.level = level;
            this.score = score;
            this.status = status;
        }

        @Override
        public String toString() {
            return String.format("[%s] Level %d | Score: %d | Result: %s", timestamp, level, score, status);
        }
    }

    public static void saveRecord(MissionRecord record) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(record.toString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Telemetry write error: " + e.getMessage());
        }
    }

    public static List<String> loadRecentLogs() {
        List<String> logs = new ArrayList<>();
        File file = new File(LOG_FILE);
        if (!file.exists()) {
            logs.add("No telemetry logs recorded yet.");
            return logs;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException e) {
            logs.add("Error reading logs: " + e.getMessage());
        }
        return logs;
    }
}
