package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;

public class Utils {


    public static String hashString(Integer _placeMngrPort, Thread _threadID) {
        Instant instant = Instant.now();
        String _id = String.valueOf(_placeMngrPort) + _threadID + instant.toEpochMilli();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert md != null;
        md.update(_id.getBytes());
        byte[] digest = md.digest();

        //Converting the byte array in to HexString format
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            hexString.append(Integer.toHexString(0xFF & b));
        }

        return hexString.toString().trim();
    }

    //Compress diferent messages in one string, receive existing message (can be empty), the Type of Message and the Value
    //Message Compressed looks like this "type:value&type:value"
    public static synchronized String messageCompressor(String existingMessage, String _type, String _value){
        if (existingMessage.isEmpty())
        {
            existingMessage = _type + ":" + _value; //param:value
        }else{
            existingMessage = existingMessage + "&" + _type + ":" + _value; //existingMessage&param:value
        }
        return existingMessage;
    }



    //Decompress the String with the diferent types and values into a Hash<String,String>
    public static synchronized HashMap<String,String> messageDecompressor(String message){
        String[] parts = message.split("&"); //First Split the String in a String[] (array) with the diferent messages "type:value"
        HashMap<String,String> decompressedMessage = new HashMap<>();
        String[] help;
        for (String part : parts) {
            help = part.split(":"); //Split the message in Type and Value
            decompressedMessage.put(help[0].trim(), help[1].trim()); //Add the type as key and the Value as value to the HashMap
        }
        return decompressedMessage;
    }
}
