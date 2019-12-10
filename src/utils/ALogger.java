package utils;

import java.util.ArrayList;

public class ALogger {
    private ArrayList<String> historyLog = new ArrayList<>();
    private CLogger cLogger;
    /**
     *
     * Class Append Logger to keep data transactions history
     *
     * @param NomeServer     Server name
     */
    public ALogger(String NomeServer) {
        cLogger = new CLogger("Append-" + NomeServer);
    }

    public synchronized void newEntry(String postalCode, String locality, String operation) {
        int logSize = historyLog.size();
        String registryID = Utils.hashString(postalCode + locality + operation).trim();
        historyLog.add(registryID);
        cLogger.newLogEntry(postalCode, locality, operation, registryID);
    }

    public synchronized String getLastEntry() {
        if (historyLog.size() == 0) {
            return "empty";
        }
        else {
            return historyLog.get(historyLog.size() - 1);
        }
    }

    public synchronized ArrayList<String> getALogger() {
        return historyLog;
    }

    public synchronized void setALogger(ArrayList<String> histLog) {
        historyLog = histLog;
    }
}
