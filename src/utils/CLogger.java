package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static utils.Utils.rightPadding;


public class CLogger {
    private PrintWriter writer;
    private DateFormat dateFormat;
    private PrintStream FileStream;
    private String FilePath;

    /**
     *
     * Class CLogger responsible for create or replace log files with server name and creation date
     *
     * @param NomeServer     Server name
     */
    public CLogger(String NomeServer) {
        this.FilePath = "Logs/Log-" + NomeServer + ".txt";
        CreateLogFile();
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String fileline = "------------------------------------------File created at: "+ dateFormat.format(obDate) + " | Server:" + NomeServer + "------------------------------------------"; // Criação da 1 linha do Ficheiro

        try {
            FileStream = new PrintStream(new File(FilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WriteToLog(fileline);
    }

    /**
     *
     * Create or Replace log file *.txt
     */
    private void CreateLogFile() {
        try {
            writer = new PrintWriter(FilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Log writer
     *
     * @param content      String provided to append into log file
     */
    private void WriteToLog(String content) {
        File f = new File(FilePath);
        if(!f.exists()) {
            CreateLogFile();
        }
        FileStream.println(content);
    }

    /**
     *
     * Add leader selection to log file
     *
     * @param placeMngrID       Place Manager Id
     * @param placeMngrLeader   Selected Leader
     */
    public void LeaderSelectionToLog(String placeMngrID, String placeMngrLeader) {
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String Type= "| Type:Leader Selection";
        Type=  rightPadding(Type, 25);
        placeMngrLeader = rightPadding(placeMngrLeader,65);
        String fileline = "|"+ dateFormat.format(obDate) + Type + "| " + placeMngrLeader +" |";
        WriteToLog(fileline);
    }

    /**
     *
     * Add KeepAlive messages to log
     *
     * @param decompressedKeepAlive Message to log
     */
    public void keepAliveToLog(HashMap<String,String> decompressedKeepAlive) {
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); //Formato da data para aparecer no log
        Object firstKey = decompressedKeepAlive.keySet().toArray()[0];
        String valueForFirstKey;
        valueForFirstKey = decompressedKeepAlive.get(firstKey);
        String Type= "| Type:" + firstKey;
        Type=  rightPadding(Type, 25);
        valueForFirstKey = rightPadding(valueForFirstKey,65);
        String fileline = "|"+ dateFormat.format(obDate) + Type + "| " + valueForFirstKey + " |";
        WriteToLog(fileline);
    }

    public void newLogEntry(String postalCode, String locality, String operation, String registryHash) {
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String Type= "| Operation:" + operation;
        Type=  rightPadding(Type, 25);
        String details = rightPadding("Postal Code: " + postalCode + " Locality: " + locality + " OpID: " + registryHash,95);
        String fileline = "|"+ dateFormat.format(obDate) + Type + "| " + details +" |";
        WriteToLog(fileline);
    }

    /**
     *
     * Log file delete
     */
    public void DeleteLog() {
        FileStream.close();
        writer.close();
        try {
            Files.deleteIfExists(Paths.get(FilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
