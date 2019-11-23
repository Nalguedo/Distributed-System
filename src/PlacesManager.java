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
    //Terminator Flag
    private boolean terminate = false;
    //Message types
    private String strKeepAlive;
    //List of places
    private ArrayList<Place> places = new ArrayList<>();
    //System View HashMap - <hashID, Leader>
    private HashMap<String, Boolean> sysView = new HashMap<>();
    //Auxiliary HashMap - <hashID, Leader>
    private HashMap<String, Boolean> sysViewAux = new HashMap<>();
    //Voting Board
    private HashMap<String, ArrayList<String>> sysVotingBoard = new HashMap<>();
    //Candidate ID
    private String placeMngrLeaderCandidate;
    //Leader ID
    private String placeMngrLeader = "noleader";
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
        placeMngrLeaderCandidate = placeMngrID;
        //Type of Messages
        String strHello = "hello:" + placeMngrID;
        strKeepAlive = "keepalive:" + placeMngrID;

        //First message sending - Server announce
        try {
            //bind socket to the port
            MulticastSocket multicastSocket = new MulticastSocket(_port);
            //join the group in the specified address
            multicastSocket.joinGroup(_addr);
            //create a new datagram packet
            DatagramPacket msg = new DatagramPacket(strHello.getBytes(), strHello.getBytes().length, _addr, _port);
            try {
                //send the datagram packet
                multicastSocket.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //create a new thread to listen to other server's messages
            Thread threadListen = (new Thread() {
                public void run() {

                    //Hash that will receive the decompressed message Type : Value
                    HashMap<String,String> messages;
                    HashMap<String,String> messagesAux = new HashMap<>();

                    while(!terminate) {
                        byte[] buffer = new byte[1000];
                        //create a packet to receive the message
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                        try {
                            //receive the message and print it on console
                            multicastSocket.receive(reply);
                            String received = new String(reply.getData());

                            //Decompress message
                            messages = messageDecompressor(received);
                            messagesAux.clear();
                            messagesAux.putAll(messages);

                            //For each type in the Hash, evaluate and execute the different actions
                            for (String type : messages.keySet()) {

                                switch(type) {
                                    case "keepalive":
                                        //Store received Placemanager ID
                                        if(!messagesAux.containsKey(messagesAux.get(type)))
                                            sysViewAux.put(messages.get(type), false);
                                        break;
                                    case "voteleader":
                                        //count vote
                                        if (sysVotingBoard.containsKey(messages.get(type))){
                                            ArrayList<String> auxVoters = sysVotingBoard.get(messages.get(type));
                                            auxVoters.add(messagesAux.get("keepalive"));
                                            sysVotingBoard.put(messages.get(type), auxVoters);
                                        }
                                        else {
                                            ArrayList<String> auxVoters = new ArrayList<>();
                                            auxVoters.add(messagesAux.get("keepalive"));
                                            sysVotingBoard.put(messages.get(type), auxVoters);
                                        }
                                        //SetLeader
                                        if (!sysViewChange() &&
                                                sysVotingBoard.size() == 1 &&
                                                sysVotingBoard.containsKey(placeMngrID)) {
                                            sysSendMsg(multicastSocket, strKeepAlive + "&setleader:" + placeMngrID);
                                        }
                                        //Exclude erratic servers
                                        if (!sysViewChange() && sysVotingBoard.size() > 1) {
                                            ArrayList<String> badLeaders = new ArrayList<>();
                                            int max = 0;
                                            String _leader ="";
                                            //Check least voted PlaceManagers
                                            for (String mngrID : sysVotingBoard.keySet()) {
                                                if (sysVotingBoard.get(mngrID).size() > max) {
                                                    max = sysVotingBoard.get(mngrID).size();
                                                    _leader = mngrID;
                                                }
                                                badLeaders.add(mngrID);
                                            }
                                            badLeaders.remove(_leader);
                                            //Check who voted against majority and exclude them
                                            for (String badLeaderID : sysVotingBoard.keySet()) {
                                                if (badLeaders.contains(badLeaderID)) {
                                                    for (String badPlaceMngr : sysVotingBoard.get(badLeaderID)) {
                                                        if (placeMngrID.equals(badPlaceMngr)) {
                                                            terminate = true;
                                                            try {
                                                                //leave the group and close the socket
                                                                multicastSocket.leaveGroup(sysAddr);
                                                                multicastSocket.close();
                                                            }
                                                            catch (IOException e){
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    case "startvote":
                                        if(messagesAux.get("keepalive").equals(placeMngrLeader) || placeMngrLeader.equals("noleader")) {
                                            sysLeaderElection();
                                            sysSendMsg(multicastSocket, strKeepAlive + "&voteleader:" + placeMngrLeaderCandidate);
                                        }
                                        break;
                                    case "setleader":
                                        setPlaceMngrLeader(messagesAux.get("setleader"));
                                        break;
                                    case "sync":
                                        //reserved
                                        break;
                                    case "hello":
                                        //New PlaceManager announced - reply with keepAlive and current Leader if exists
                                        if (!messagesAux.get("hello").equals(placeMngrID) && placeMngrLeader.equals(placeMngrID)) {
                                            //sysSendMsg(multicastSocket, strKeepAlive);
                                            sysSendMsg(multicastSocket, strKeepAlive + "&setleader:" + placeMngrID);
                                        }
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
                try {
                    //Setup time wait
                    Thread.sleep(500);
                    //If no setLeader received on first run ... you're the leader
                    if(placeMngrLeader.equals("noleader")) {
                        sysLeaderElection();
                        sysSendMsg(multicastSocket, strKeepAlive + "&startvote:" + placeMngrID);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Create a new thread to send messages to group
                Thread threadSend = (new Thread() {
                    public void run() {
                        while (!terminate) {
                            try {
                                //Setup time wait
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            sysSendMsg(multicastSocket, strKeepAlive);

                            try {
                                //Setup time wait
                                Thread.sleep(1000);
                                System.out.println("Placemanager id:" + placeMngrID + "\nSelected Lider:" + placeMngrLeader);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //if sysView size changed && is Leader -> Voting takes place
                            if(sysViewChange() && placeMngrID.equals(placeMngrLeader)) {
                                sysLeaderElection();
                                sysSendMsg(multicastSocket, strKeepAlive + "&startvote:" + placeMngrID);
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
        placeMngrLeaderCandidate = max;
        //Clear List of Placemanagers
        sysView.clear();
        //Set updated Placemanagers list
        sysView.putAll(sysViewAux);
        sysViewAux.clear();
        if (placeMngrLeaderCandidate.isEmpty())
            setPlaceMngrLeader(placeMngrID);
        else
            setPlaceMngrLeader(placeMngrLeaderCandidate);
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

    //Compress diferent messages in one string, receive existing message (can be empty), the Type of Message and the Value
    //Message Compressed looks like this "type:value&type:value"
    private synchronized String messageCompressor(String existingMessage, String _type, String _value){
        if (existingMessage.isEmpty())
        {
            existingMessage = _type + ":" + _value; //param:value
        }else{
            existingMessage = existingMessage + "&" + _type + ":" + _value; //existingMessage&param:value
        }
        return existingMessage;
    }

    //Decompress the String with the diferent types and values into a Hash<String,String>
    private synchronized HashMap<String,String> messageDecompressor(String message){
        String[] parts = message.split("&"); //First Split the String in a String[] (array) with the diferent messages "type:value"
        HashMap<String,String> decompressedMessage = new HashMap<>();
        String[] help;
        for (String part : parts) {
            help = part.split(":"); //Split the message in Type and Value
            decompressedMessage.put(help[0], help[1]); //Add the type as key and the Value as value to the HashMap
        }
        return decompressedMessage;
    }

    private synchronized void sysSendMsg(MulticastSocket _multicastSocket, String _message) {
        DatagramPacket msgDatagram = new DatagramPacket(_message.getBytes(), _message.getBytes().length, sysAddr, placeMngrPort);
        try {
            //send the datagram packet
            _multicastSocket.send(msgDatagram);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean sysViewChange() {
        return sysView.size() != sysViewAux.size();
    }

    private synchronized void setPlaceMngrLeader(String _placeMngrLeaderID) {
        placeMngrLeader = _placeMngrLeaderID;
    }
}
