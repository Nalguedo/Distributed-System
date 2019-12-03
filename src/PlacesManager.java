import utils.Utils;
import utils.CLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    //Flags
    private boolean terminateFlag = false;
    private boolean votingFlag = false;
    private boolean sysChangeFlag = false;
    private int votes = 0;
    //Message types
    private String strKeepAlive;
    //List of places
    private ArrayList<Place> places = new ArrayList<>();
    //System View HashMap - <hashID, Leader>
    private ArrayList<String> sysView = new ArrayList<>();
    //Auxiliary HashMap - <hashID, Leader>
    private ArrayList<String> sysViewAux = new ArrayList<>();
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
    //System RMI Port
    private int sysRMIPort;
    //System IP Address
    private InetAddress sysAddr;
    //Multicast Socket
    private MulticastSocket multicastSocket;

    PlacesManager(InetAddress addr, int multicastPort, int rmiPort, String placeMID, CLogger LogFile) throws RemoteException {
        //Thread ID
        Thread threadID = Thread.currentThread();
        sysAddr = addr;
        placeMngrPort = multicastPort;
        sysRMIPort = rmiPort;
        placeMngrID = placeMID;
        placeMngrLeaderCandidate = placeMngrID;
        //Type of Messages
        String strHello = "hello:" + placeMngrID;
        strKeepAlive = "keepalive:" + placeMngrID;

        //First message sending - Server announce
        try {
            //bind socket to the port
            multicastSocket = new MulticastSocket(multicastPort);
            //join the group in the specified address
            multicastSocket.joinGroup(addr);
            //create a new datagram packet
            DatagramPacket msg = new DatagramPacket(strHello.getBytes(), strHello.getBytes().length, addr, multicastPort);
            try {
                //send the datagram packet
                multicastSocket.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //create a new thread to listen to other server's messages
            Thread threadListen = (new Thread(() -> {

                //Hash that will receive the decompressed message Type : Value
                HashMap<String,String> messages;
                HashMap<String,String> messagesAux = new HashMap<>();

                while(!terminateFlag) {
                    byte[] buffer = new byte[1000];
                    //create a packet to receive the message
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    try {
                        //receive the message and print it on console
                        multicastSocket.receive(reply);
                        String received = new String(reply.getData()).trim();

                        //Decompress message
                        messages = Utils.messageDecompressor(received, "&", ":");
                        messagesAux.clear();
                        messagesAux.putAll(messages);

                        //For each type in the Hash, evaluate and execute the different actions
                        for (String type : messages.keySet()) {

                            switch(type) {
                                case "keepalive":
                                    //Store received Placemanager ID
                                    addSysViewAux(messages.get(type));
                                    break;
                                case "hello":
                                    if (messagesAux.get(type).trim().equals(placeMngrID.trim()))
                                        break;
                                    //Store received Placemanager ID
                                    addSysViewAux(messagesAux.get(type));
                                    //New PlaceManager announced - reply with keepAlive and current Leader if exists
                                    if (placeMngrLeader.equals(placeMngrID)) {
                                        sysSendMsg(multicastSocket, strKeepAlive + "&setleader:" + placeMngrID);
                                    }
                                    break;
                                case "setleader":
                                    setPlaceMngrLeader(messagesAux.get("setleader"));
                                    votingFlag = false;
                                    votes = 0;
                                    sysVotingBoard.clear();
                                    break;
                                case "startvote":
                                    if(votes == 0 && sysView.size() > 0) {
                                        votingFlag = true;
                                        sysSendMsg(multicastSocket, strKeepAlive + "&voteleader:" + placeMngrLeaderCandidate);
                                    }
                                    break;
                                case "voteleader":
                                    if (!votingFlag)
                                        break;
                                    //Prevent index mismatch
                                    //Store received Placemanager ID
                                    addSysViewAux(messages.get("keepalive"));
                                    //count vote
                                    if (sysVotingBoard.containsKey(messages.get(type))){
                                        ArrayList<String> auxVoters = sysVotingBoard.get(messages.get(type));
                                        auxVoters.add(messages.get("keepalive").trim());
                                        sysVotingBoard.put(messages.get(type).trim(), auxVoters);
                                    }
                                    else {
                                        ArrayList<String> auxVoters = new ArrayList<>();
                                        auxVoters.add(messages.get("keepalive"));
                                        sysVotingBoard.put(messages.get(type).trim(), auxVoters);
                                    }
                                    //SetLeader
                                    if (!sysViewChange() &&
                                            sysVotingBoard.size() == 1 &&
                                            sysVotingBoard.containsKey(placeMngrID)) {
                                        sysSendMsg(multicastSocket, strKeepAlive + "&setleader:" + placeMngrID);
                                    }
                                    break;
                                case "callmethod":
                                    //Server
                                    if (placeMngrLeader.equals(messagesAux.get("keepalive"))) {
                                        callMethodByName(messagesAux.get(type), messagesAux.get("params"));
                                    }
                                    break;
                                default:
                                    // code block
                            }
                        }

                        System.out.println("S: " + sysView.size() +  " Reply: " + received.trim() + " Who Received: " + placeMngrID);
                        HashMap<String,String> decompressedKeepAlive = Utils.messageDecompressor(received.trim(), "&", ":");
                        LogFile.keepAliveToLog(decompressedKeepAlive);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    //leave the group and close the socket
                    multicastSocket.leaveGroup(sysAddr);
                    multicastSocket.close();
                    System.out.println("EXIT----------------------------------------------" + placeMngrID);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            threadListen.start();

            //Only main thread will send messages
            if(threadID == Thread.currentThread()) {
                try {
                    //Setup time wait
                    Thread.sleep(2000);
                    //If no setLeader received on first run ... you're the leader
                    if(placeMngrLeader.equals("noleader")) {
                        sysLeaderElection();
                        placeMngrLeader = placeMngrLeaderCandidate;
                        System.out.println("Placemanager id:" + placeMngrID + "\nSelected Lider:" + placeMngrLeader);
                        LogFile.LeaderSelectionToLog(placeMngrID, placeMngrLeader);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Create a new thread to send messages to group
                Thread threadSend = (new Thread(() -> {
                    while (!terminateFlag) {
                        try {
                            //Setup time wait
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //if current leader is no longer the candidate
                        if(placeMngrID.trim().equals(placeMngrLeader.trim()) && !placeMngrID.trim().equals(placeMngrLeaderCandidate.trim())) {
                            //terminateFlag = true; //test leader exits
                            sysSendMsg(multicastSocket, strKeepAlive + "&startvote:" + placeMngrLeaderCandidate);
                        } //if leader exits
                        else if (placeMngrID.trim().equals(placeMngrLeaderCandidate.trim()) && !sysViewAux.contains(placeMngrLeader.trim())) {
                            if (placeMngrLeaderCandidate.equals(placeMngrID))
                                sysSendMsg(multicastSocket, "keepalive:" + placeMngrLeader + "&startvote:" + placeMngrLeaderCandidate);
                        }

                        //check leader candidate each iteration
                        sysLeaderElection();
                        sysChangeFlag = !sysChangeFlag;
                        if (!sysChangeFlag) {
                            sysViewSync();
                            //if (placeMngrID.contains("ee"))
                                //terminateFlag = true; //test random exit
                        }
                        else {
                            sysViewAux.clear();
                        }

                        sysSendMsg(multicastSocket, strKeepAlive);
                        System.out.println("\n\nPlacemanager id:" + placeMngrID + "\nSelected Lider:" + placeMngrLeader);
                    }
                }));
                threadSend.start();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void addPlace(Place p) throws RemoteException {
        if (!places.contains(p))
            places.add(p);
        //TODO messageCompressor implementation
        sysSendMsg(multicastSocket, strKeepAlive + "&callmethod:addplace&params:locality," + p.getLocality() + ";postalcode," + p.getPostalCode());
        System.out.println("\n\nNew Place Added: " + p.getLocality() + " : " + p.getPostalCode());
    }

    @Override
    public synchronized ArrayList<Place> allPlaces() throws RemoteException {
        return places;
    }

    @Override
    public synchronized Place getPlace(String objectID) throws RemoteException {
        for (Place place : places) {
            if (place.getPostalCode().equals(objectID)) {
                return place;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean removePlace(String objectID) throws RemoteException {
        for (Place place : places) {
            if (place.getPostalCode().equals(objectID)) {
                places.remove(place);
                return true;
            }
        }
        return false;
    }

    private synchronized void sysLeaderElection() {
        String max = "";

        for(String entry : sysViewAux) {
            if (entry.compareTo(max) > 0) {
                max = entry ;
            }
        }
        //Set highest hash as leader candidate
        placeMngrLeaderCandidate = max;

        if (placeMngrLeaderCandidate.isEmpty())
            placeMngrLeaderCandidate = placeMngrID;
    }

    private synchronized void sysSendMsg(MulticastSocket multicastSocket, String message) {
        DatagramPacket msgDatagram = new DatagramPacket(message.getBytes(), message.getBytes().length, sysAddr, placeMngrPort);
        try {
            //send the datagram packet
            multicastSocket.send(msgDatagram);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean sysViewSync() {
        //check for new servers
        if (sysViewAux.size() > sysView.size()) {
            sysView.clear();
            sysView.addAll(sysViewAux);
            return true;
        }
        //check if servers exited
        else {
            for (String sysServerId : sysView) {
                if (!sysViewAux.contains(sysServerId)) {
                    sysViewAux.clear();
                    sysViewAux.addAll(sysView);
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized boolean sysViewChange() {
        return sysViewAux.size() != sysView.size();
    }

    private synchronized void setPlaceMngrLeader(String placeMngrLeaderID) {
        placeMngrLeader = placeMngrLeaderID.trim();
    }

    private synchronized void addSysViewAux(String id) {
        String idTrimmed = id.trim();
        if (!sysViewAux.contains(idTrimmed)) {
            sysViewAux.add(idTrimmed);
        }
    }

    /**
     * Call placesManager methods and reply requests or set places
     *
     * @param methodName Method which will be invoked
     * @param params      String with all params split [addplace$3500,Viseu]
     */
    private void callMethodByName(String methodName, String params) throws RemoteException {
        HashMap<String,String> hashParams = Utils.messageDecompressor(params, ";", ",");
        Place newPlace;
        switch (methodName) {
            case "addplace":
                newPlace = new Place(hashParams.get("postalcode"), hashParams.get("locality"));
                if (!places.contains(newPlace))
                    places.add(newPlace);
                System.out.println("\n\nNew Place Added: " + newPlace.getLocality() + " : " + newPlace.getPostalCode() + "\nPlacemanager: " + placeMngrID);
                break;
            case "removeplace":
                removePlace(hashParams.get(methodName));
                break;
            case "getplace":
                newPlace = getPlace(hashParams.get(methodName));
                //TODO send RMI message with args or object serialized

                break;
            case "getallplaces":
                //TODO send RMI message with args or object serialized

                break;
            case "setallplaces":
                places.clear();
                for (String postCode : hashParams.keySet()) {
                    newPlace = new Place(postCode, hashParams.get(postCode));
                    places.add(newPlace);
                }
                break;
            default:
                //code block
        }
    }

    private synchronized PlacesListInterface getRemotePlaceMngr(String remotePlaceMngrID) {
        try {
            return (PlacesListInterface) Naming.lookup("rmi://localhost:" + sysRMIPort + "/" + remotePlaceMngrID);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }
}