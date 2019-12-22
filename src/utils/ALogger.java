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

    /**
     *
     * Add new entry to Append log file
     *
     * @param postalCode    Postal code
     * @param locality      Locality
     * @param operation     Operation type
     */
    public synchronized void newEntry(String postalCode, String locality, String operation) {
        String registryID = Utils.hashString(postalCode + locality + operation).trim();
        historyLog.add(registryID);
        cLogger.newLogEntry(postalCode, locality, operation, registryID);
    }

    /**
     *
     * Get last Append log entry
     *
     * @return      String - entry hash id
     */
    public synchronized String getLastEntry() {
        if (historyLog.size() == 0) {
            return "empty";
        }
        else {
            return historyLog.get(historyLog.size() - 1);
        }
    }


    /**
     *
     * Get full list of append log entries
     *
     * @return      Arraylist containing full list of append log entries
     */
    public synchronized ArrayList<String> getALogger() {
        return historyLog;
    }

    public synchronized void setALogger(ArrayList<String> histLog) {
        historyLog = histLog;
    }
}
