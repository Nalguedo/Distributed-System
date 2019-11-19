import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {

    //List of places
    private ArrayList<Place> places = new ArrayList<>();
    //System View HashMap - <hashID, Leader>
    private HashMap<String, Boolean> sysView = new HashMap<>();
    //Auxiliary HashMap - <hashID, Leader>
    private HashMap<String, Boolean> sysViewAux = new HashMap<>();
    //Leader ID
    private String placeMngrLeader;
    //Placemanager ID
    private String placeMngrID;
    //PlaceManager Port
    private int placeMngrPort;
    //System IP Address
    private InetAddress sysAddr;
    //Thread ID
    private Thread threadID;

    PlacesManager(InetAddress _addr, int _port) throws RemoteException {
        threadID = Thread.currentThread();
        sysAddr = _addr;
        placeMngrPort = _port;
        placeMngrID = hashString();
        sysView.put(placeMngrID, true);
        placeMngrLeader = placeMngrID;

        String message = placeMngrID;

        try {
            //bind socket to the port
            MulticastSocket multicastSocket = new MulticastSocket(_port);
            //join the group in the specified address
            multicastSocket.joinGroup(_addr);
            //create a new datagram packet
            DatagramPacket msg = new DatagramPacket(message.getBytes(), message.getBytes().length, _addr, _port);
            try {
                //send the datagram packet
                multicastSocket.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //create a new thread to listen to other server's messages
            Thread threadListen = (new Thread() {
                public void run() {
                    while(true) {
                        byte[] buffer = new byte[1000];
                        //create a packet to receive the message
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                        try {
                            //receive the message and print it on console
                            multicastSocket.receive(reply);
                            String received = new String(reply.getData());

                            HashMap<String,String> messages = new HashMap<>();

                            for (String type : messages.keySet()) {

                                switch(message) {
                                    case "KeepAlive":
                                        // code block
                                        break;
                                    case "Lider":
                                        //Store received Placemanager ID
                                        sysViewAux.put(messages.get(type), false);
                                        break;
                                    case "Sync":
                                        break;
                                    default:
                                        // code block
                                }
                            }

                            System.out.println("Reply: " + received);

                            //TODO: manage failures
                            //TODO: Receive Message Only From Group
                            //TODO: LOG
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                        /*try {
                            //leave the group and close the socket
                            multicastSocket.leaveGroup(address);
                            multicastSocket.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }*/
                }
            });
            threadListen.start();

            //Only main thread will send messages
            if(threadID == Thread.currentThread()) {
                //create a new thread to send messages to group
                Thread threadSend = (new Thread() {
                    public void run() {
                        int i = 0;
                        String msg = "";
                        DatagramPacket msgDatagram;
                        while (true) {
                            try {
                                //Setup time wait
                                sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            msg = messageCompressor(msg,"Lider",placeMngrID);
                            //msg = String.valueOf(placeMngrID);
                            msgDatagram = new DatagramPacket(msg.getBytes(), msg.getBytes().length, _addr, _port);
                            try {
                                //send the datagram packet
                                multicastSocket.send(msgDatagram);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                //Setup time wait
                                sleep(1000);
                                sysLeaderElection();
                                System.out.println("Placemanager id:" + placeMngrID + "\nSelected Lider:" + placeMngrLeader);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                threadSend.start();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void addPlace(Place p) throws RemoteException {
        places.add(p);
    }

    @Override
    public ArrayList<Place> allPlaces() throws RemoteException {
        return places;
    }

    @Override
    public Place getPlace(String objectID) throws RemoteException {
        for (Place place : places) {
            if (place.getPostalCode().equals(objectID)) {
                return place;
            }
        }
        return null;
    }

    private void sysLeaderElection() {
        String max = "";

        for(Map.Entry<String, Boolean> entry : sysViewAux.entrySet()) {
            String key = entry.getKey();
            Boolean value = entry.getValue();
            entry.setValue(false);
            if (key.compareTo(max) > 0) {
                max = key ;
            }
        }
        //Set highest hash as leader
        sysViewAux.put(max, true);
        placeMngrLeader = max;
        //Clear List of Placemanagers
        sysView.clear();
        //Set updated Placemanagers list
        sysView.putAll(sysViewAux);
        sysViewAux.clear();
    }

    private String hashString() {
        Instant instant = Instant.now();
        String _id = String.valueOf(placeMngrPort) + threadID + instant.toEpochMilli();
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

        return hexString.toString();
    }

    private String messageCompressor(String existingMessage, String param, String value){
        if (existingMessage.isEmpty())
        {
            existingMessage = param + ":" + value;
        }else{
            existingMessage = existingMessage + "&" + param + ":" + value;
        }
        return existingMessage;
    }

    private HashMap<String,String> messageDecompressor(String message){
        String[] parts = message.split("&");
        HashMap<String,String> decompressedMessage = new HashMap<>();
        String[] help;
        for (int i = 0; i < parts.length; i++){
            help = parts[i].split(":");
            decompressedMessage.put(help[0],help[1]);
        }
        return decompressedMessage;
    }

}
