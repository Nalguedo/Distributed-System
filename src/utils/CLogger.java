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
    private String NomeServer;
    private PrintWriter writer;
    private DateFormat dateFormat;
    private PrintStream FileStream;
    private String FilePath;

    public CLogger(String NomeServer) {
        this.NomeServer = NomeServer;
        this.FilePath = "Logs/" + NomeServer;
        CreateLogFile(); //Criação do Ficheiro Log
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); //Formato da data para aparecer no log
        String fileline = "------------------------------------------File created at: "+ dateFormat.format(obDate) + " | Server:" + NomeServer + "------------------------------------------"; // Criação da 1 linha do Ficheiro
        //Criação da Stream que vai ser usada para escrever no Ficheiro
        try {
            FileStream = new PrintStream(new File(FilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WriteToLog(fileline);
        DeleteLog();
    }

    private void CreateLogFile() {
        try {
            writer = new PrintWriter(FilePath + ".txt", StandardCharsets.UTF_8); //Criação do Ficheiro, caso exista apaga e cria novo
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void WriteToLog(String content) {
        //Verificação se o Ficheiro Log existe
        File f = new File(FilePath);
        if(!f.exists()) {
            // Caso não exista cria o ficheiro
            CreateLogFile();
        }
        FileStream.println(content);
    }
    public void LeaderSelectionToLog(String placeMngrID,String placeMngrLeader) {
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); //Formato da data para aparecer no log
        String Type= "| Type:Leader Selection";
        Type=  rightPadding(Type, 25);
        placeMngrLeader = rightPadding(placeMngrLeader,65);
        String fileline = "|"+ dateFormat.format(obDate) + Type + "| " + placeMngrLeader +" |"; // Criação da 1 linha do Ficheiro
        WriteToLog(fileline);
    }

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

    private void DeleteLog() {
        FileStream.close();
        writer.close();
        try {
            Files.deleteIfExists(Paths.get(FilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
