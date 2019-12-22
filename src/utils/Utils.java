package utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * @version 1.1
 */

public class Utils {

    /**
     * Create unique server id hash using given params and instant milli
     *
     * @param stringToHash      String to hash
     * @return String           Hash ID using digest "SHA-256"
     */
    public static String hashString(String stringToHash) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert md != null;
        md.update(stringToHash.getBytes());
        byte[] digest = md.digest();

        //Converting the byte array in to HexString format
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            hexString.append(Integer.toHexString(0xFF & b));
        }

        return hexString.toString().trim();
    }

    /**
     * Create string with multi key value pairs divided by split chars provided
     *
     * @param existingMessage Existing string (may be empty)
     * @param type            Message type (key)
     * @param value           Message (value)
     * @param split1          Entry divisor char
     * @param split2          Key value divisor char
     * @return String         "type:value&type:value"
     */
    public static synchronized String messageCompressor(String existingMessage, String type, String value, String split1, String split2) {
        if (existingMessage.isEmpty()) {
            existingMessage = type + split2 + value;
        } else {
            existingMessage = existingMessage + split1 + type + split2 + value;
        }
        return existingMessage;
    }

    /**
     * Extract key value pairs into HashMap divided by split chars provided
     *
     * @param message Existing string
     * @param split1  Entry divisor char
     * @param split2  Key value divisor char
     * @return HashMap      Contains all pairs identified in message string
     */
    public static synchronized HashMap<String, String> messageDecompressor(String message, String split1, String split2) {
        String[] parts = message.split(split1);
        HashMap<String, String> decompressedMessage = new HashMap<>();
        String[] help;
        for (String part : parts) {
            help = part.split(split2);
            decompressedMessage.put(help[0].trim(), help[1].trim());
        }
        return decompressedMessage;
    }

    /**
     * Increase string length with space char
     *
     * @param str Existing string
     * @param num String size limit
     * @return String     num sized string with spaces
     */
    public static String rightPadding(String str, int num) {
        return String.format("%1$-" + num + "s", str);
    }

    /**
     *
     * Get ip address which is used to connect to internet
     *
     * @return      String corresponding to machine IP address removing "/" char
     */
    public static String getIpAddress() {
        Socket socket = new Socket();
        String ipAddr = null;
        try {
            socket.connect(new InetSocketAddress("google.com", 80));
            ipAddr = socket.getLocalAddress().toString().replace("/", "");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ipAddr;
    }

    /**
     * Get current project directory
     * @return      Path String
     */
    public static String CurrDirectory() {
        return System.getProperty("user.dir");
    }
}