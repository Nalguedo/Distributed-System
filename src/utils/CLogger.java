package utils;

import java.io.*;
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

    public CLogger(String NomeServer) {
        this.NomeServer = NomeServer;
        try {
            writer = new PrintWriter(NomeServer, "UTF-8"); //Criação do Ficheiro, caso exista apaga e cria novo
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Date obDate = new Date();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); //Formato da data para aparecer no log
        String fileline = "------------------------------------------File created at: "+ dateFormat.format(obDate) + " | Server:" + NomeServer + "------------------------------------------"; // Criação da 1 linha do Ficheiro
        //Criação da Stream que vai ser usada para escrever no Ficheiro
        try {
            FileStream = new PrintStream(new File(NomeServer));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WriteToLog(fileline);
    }

    public void WriteToLog(String content) {
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
        Object valueForFirstKey = decompressedKeepAlive.get(firstKey);
        String Type= "| Type:" + firstKey;
        Type=  rightPadding(Type, 25);
        valueForFirstKey = rightPadding((String) valueForFirstKey,65);
        String fileline = "|"+ dateFormat.format(obDate) + Type + "| " + valueForFirstKey + " |";
        WriteToLog(fileline);
    }

}
