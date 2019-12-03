package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;

/**
 * @author Gonçalo
 * @version 1.0
 * */

public class Utils {

    /**
     * Create unique server id hash using given params and instant milli
     *
     * @param _placeMngrPort Port number
     * @param _threadID      Server main thread id
     * @return String - Hash ID using digest "SHA-256"
     */
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

    /**
     * Create unique server id hash using given params and instant milli
     *
     * @param existingMessage Port number
     * @param _type      Server main thread id
     * @param _value      Server main thread id
     * @return String - Hash ID using digest "SHA-256"
     */
    //Compress diferent messages in one string, receive existing message (can be empty), the Type of Message and the Value
    //Message Compressed looks like this "type:value&type:value"
    public static synchronized String messageCompressor(String existingMessage, String _type, String _value) {
        if (existingMessage.isEmpty()) {
            existingMessage = _type + ":" + _value; //param:value
        } else {
            existingMessage = existingMessage + "&" + _type + ":" + _value; //existingMessage&param:value
        }
        return existingMessage;
    }


    //Decompress the String with the diferent types and values into a Hash<String,String>
    public static synchronized HashMap<String, String> messageDecompressor(String message, String split1, String split2) {
        String[] parts = message.split(split1); //First Split the String in a String[] (array) with the diferent messages "type:value"
        HashMap<String, String> decompressedMessage = new HashMap<>();
        String[] help;
        for (String part : parts) {
            help = part.split(split2); //Split the message in Type and Value
            decompressedMessage.put(help[0].trim(), help[1].trim()); //Add the type as key and the Value as value to the HashMap
        }
        return decompressedMessage;
    }

    //Preenche a String com espaços até chegar a um dado comprimento
    public static String rightPadding(String str, int num) {
        return String.format("%1$-" + num + "s", str);
    }
}
