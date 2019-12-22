import utils.ALogger;
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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * PlacesManager contains all places objects, and communications threads tha will keep the DistributedSystem updated and working
 */
public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private static final long serialVersionUID = 1L;
    //Flags
    private boolean terminateFlag = false;
    private boolean votingFlag = false;
    private boolean sysChangeFlag = false;
    private int votes = 0;
    //Message types
    private String strKeepAlive;
    private String msgSetLeader;
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
    //System IP Multicast Address
    private InetAddress sysAddr;
    //System IP Address
    private String sysIPAddr;
    //Multicast Socket
    private MulticastSocket multicastSocket;
    //Append Log
    private ALogger aLogger;

    /**
     *
     * PlacesManager Constructor - Creates 2 threads (ThreadListen and ThreadSend) for multicast communication
     *
     *
     * @param ipAddress         Server IP Address
     * @param mcastAddr         Multicast IP address
     * @param multicastPort     System Multicast Port
     * @param rmiPort           System RMI Port
     * @param placeMID          PlaceManager ID unique hash [Utils.hashString(Integer placeMngrPort, Thread threadID)]
     * @param LogFile           CLogger object to store custom log messages
     * @throws RemoteException  Remote exception
     */
    PlacesManager(String ipAddress, InetAddress mcastAddr, int multicastPort, int rmiPort, String placeMID, CLogger LogFile) throws RemoteException {
        //Thread ID
        Thread threadID = Thread.currentThread();
        sysIPAddr = ipAddress;
        sysAddr = mcastAddr;
        placeMngrPort = multicastPort;
        sysRMIPort = rmiPort;
        placeMngrID = placeMID;
        placeMngrLeaderCandidate = placeMngrID;
        //Type of Messages
        String strHello = "hello:" + placeMngrID;
        strKeepAlive = "keepalive:" + placeMngrID;
        aLogger = new ALogger(placeMID);

        //First message sending - Server announce
        try {
            //bind socket to the port
            multicastSocket = new MulticastSocket(multicastPort);
            //join the group in the specified address
            multicastSocket.joinGroup(mcastAddr);
            //create a new datagram packet
            DatagramPacket msg = new DatagramPacket(strHello.getBytes(), strHello.getBytes().length, mcastAddr, multicastPort);
            try {
                //send the datagram packet
                multicastSocket.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //create a new thread to listen to other server's messages
            Thread threadListen = (new Thread(() -> {

                //Hash that will receive the decompressed message Type : Value
                HashMap<String, String> messages;
                HashMap<String, String> messagesAux = new HashMap<>();

                while (!terminateFlag) {
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

                            switch (type) {
                                case "keepalive":
                                    //Store received Placemanager ID
                                    addSysViewAux(messages.get(type));
                                    break;
                                case "alogger":
                                    if (!messagesAux.get(type).trim().equals(aLogger.getLastEntry()) &&
                                            messagesAux.get("keepalive").equals(placeMngrLeader)) {
                                        PlacesListInterface placesListInterface = getPlaceMngrRMI(placeMngrLeader);
                                        assert placesListInterface != null;
                                        places = placesListInterface.allPlaces();
                                        //aLogger = new ALogger(placeMngrID);
                                        for (Place place : places) {
                                            aLogger.newEntry(place.getPostalCode(), place.getLocality(), "addplace");
                                        }
                                    }
                                    break;
                                case "hello":
                                    if (messagesAux.get(type).trim().equals(placeMngrID.trim()))
                                        break;
                                    //Store received Placemanager ID
                                    addSysViewAux(messagesAux.get(type));
                                    //New PlaceManager announced - reply with keepAlive, current Leader if exists and set all places
                                    msgSetLeader = strKeepAlive;
                                    msgSetLeader = Utils.messageCompressor(msgSetLeader, "setleader", placeMngrID, "&", ":");
                                    if (placeMngrID.equals(placeMngrLeader)) {
                                        if (places.size() > 0) {
                                            String allPlacesStr = "";
                                            for (Place place : places) {
                                                allPlacesStr = Utils.messageCompressor(allPlacesStr, place.getPostalCode(), place.getLocality(), ";", ",");
                                            }
                                            msgSetLeader = Utils.messageCompressor(msgSetLeader, "callmethod", "setallplaces", "&", ":");
                                            msgSetLeader = Utils.messageCompressor(msgSetLeader, "params", allPlacesStr, "&", ":");
                                        }
                                        sysSendMsg(multicastSocket, msgSetLeader.trim());
                                    }
                                    break;
                                case "setleader":
                                    setPlaceMngrLeader(messagesAux.get("setleader"));
                                    votingFlag = false;
                                    votes = 0;
                                    sysVotingBoard.clear();
                                    break;
                                case "startvote":
                                    if (votes == 0 && sysView.size() > 0) {
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
                                    if (sysVotingBoard.containsKey(messages.get(type))) {
                                        ArrayList<String> auxVoters = sysVotingBoard.get(messages.get(type));
                                        auxVoters.add(messages.get("keepalive").trim());
                                        sysVotingBoard.put(messages.get(type).trim(), auxVoters);
                                    } else {
                                        ArrayList<String> auxVoters = new ArrayList<>();
                                        auxVoters.add(messages.get("keepalive"));
                                        sysVotingBoard.put(messages.get(type).trim(), auxVoters);
                                    }
                                    //SetLeader
                                    if (!sysViewChange() &&
                                            sysVotingBoard.size() == 1 &&
                                            sysVotingBoard.containsKey(placeMngrID)) {
                                        msgSetLeader = strKeepAlive;
                                        msgSetLeader = Utils.messageCompressor(msgSetLeader, "setleader", placeMngrID, "&", ":");
                                        sysSendMsg(multicastSocket, msgSetLeader.trim());
                                    }
                                    break;
                                case "callmethod":
                                    //Server
                                    if (placeMngrLeader.equals(messagesAux.get("keepalive")) && !placeMngrID.equals(placeMngrLeader)) {
                                        callMethodByName(messagesAux.get(type), messagesAux.get("params"));
                                    }
                                    break;
                                case "syncplaces":
                                    // code block
                                    break;
                                default:
                                    // code block
                            }
                        }

                        System.out.println("S: " + sysView.size() + " Reply: " + received.trim() + " Who Received: " + placeMngrID);
                        HashMap<String, String> decompressedKeepAlive = Utils.messageDecompressor(received.trim(), "&", ":");
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            threadListen.start();

            //Only main thread will send messages
            if (threadID == Thread.currentThread()) {
                try {
                    //Setup time wait
                    Thread.sleep(2000);
                    //If no setLeader received on first run ... you're the leader
                    if (placeMngrLeader.equals("noleader")) {
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
                        if (placeMngrID.trim().equals(placeMngrLeader.trim()) && !placeMngrID.trim().equals(placeMngrLeaderCandidate.trim())) {
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
                        } else {
                            sysViewAux.clear();
                        }

                        //Leader shares last entry hash
                        if (placeMngrID.equals(placeMngrLeader))
                            sysSendMsg(multicastSocket, Utils.messageCompressor(strKeepAlive, "alogger", aLogger.getLastEntry(), "&", ":"));
                        else
                            sysSendMsg(multicastSocket, strKeepAlive);

                        System.out.println("\n\nPlacemanager id:" + placeMngrID + "\nSelected Lider:" + placeMngrLeader);
                    }
                }));
                threadSend.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * addPlace Override - Calling addPlace on Leader will add place to the list of places a send a multicast message for replication
     *
     * @param p     Place to add
     * @return      Boolean for success
     */
    @Override
    public synchronized boolean addPlace(Place p) {
        for (Place place : places) {
            if (place.getPostalCode().trim().equals(p.getPostalCode().trim())) {
                return false;
            }
        }
        places.add(p);
        String msgAddPlace = strKeepAlive;
        msgAddPlace = Utils.messageCompressor(msgAddPlace, "callmethod", "addplace", "&", ":");
        msgAddPlace = Utils.messageCompressor(msgAddPlace, "params", "locality," + p.getLocality() + ";postalcode," + p.getPostalCode(), "&", ":");
        sysSendMsg(multicastSocket, msgAddPlace);
        aLogger.newEntry(p.getPostalCode(), p.getLocality(), "addplace");
        System.out.println("\n\nNew Place Added: " + p.getLocality() + " : " + p.getPostalCode());
        return true;
    }

    /**
     *
     * Get current list of all places
     *
     * @return      Arraylist containing list of all places
     */
    @Override
    public synchronized ArrayList<Place> allPlaces() {
        return places;
    }

    /**
     *
     * Get a specific place giving postalCode (objectID)
     *
     * @param objectID      Place ID corresponds to postalCode
     * @return              Place with postalcode == objectID if exists or null if don't exists
     */
    @Override
    public synchronized Place getPlace(String objectID) {
        for (Place place : places) {
            if (place.getPostalCode().equals(objectID)) {
                return place;
            }
        }
        return null;
    }

    /**
     *
     * Remove a specific place giving postalCode (objectID)
     *
     * @param objectID      Place ID corresponds to postalCode
     * @return              True if deletion succeeds, false if it don't
     */
    @Override
    public synchronized boolean removePlace(String objectID) {
        for (Place place : places) {
            if (place.getPostalCode().equals(objectID.trim())) {
                places.remove(place);
                String msgRemovePlace = strKeepAlive;
                msgRemovePlace = Utils.messageCompressor(msgRemovePlace, "callmethod", "removeplace", "&", ":");
                msgRemovePlace = Utils.messageCompressor(msgRemovePlace, "params", "postalcode," + objectID, "&", ":");
                sysSendMsg(multicastSocket, msgRemovePlace);
                aLogger.newEntry(place.getPostalCode(), place.getLocality(), "removeplace");
                //System.out.println("\n\nPlace removed: " + objectID);
                return true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<String> getALogger() throws RemoteException {
        return aLogger.getALogger();
    }

    /**
     *
     * System Leader election - highest ID hash on sysViewAux (fresh sysView) will be considered the System Leader Candidate
     */
    private synchronized void sysLeaderElection() {
        String max = "";

        for (String entry : sysViewAux) {
            if (entry.compareTo(max) > 0) {
                max = entry;
            }
        }
        //Set highest hash as leader candidate
        placeMngrLeaderCandidate = max;

        if (placeMngrLeaderCandidate.isEmpty())
            placeMngrLeaderCandidate = placeMngrID;
    }

    /**
     *
     * Send multicast message using given socket and message
     *
     * @param multicastSocket      Multicast socket previously created
     * @param message              String message to send
     */
    private synchronized void sysSendMsg(MulticastSocket multicastSocket, String message) {
        DatagramPacket msgDatagram = new DatagramPacket(message.getBytes(), message.getBytes().length, sysAddr, placeMngrPort);
        try {
            //send the datagram packet
            multicastSocket.send(msgDatagram);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Compare sysView with sysViewAux which is cleared every 2 cycles to check if servers stopped responding or new connections occurred
     * Update sysView or sysViewAux accordingly
     */
    private synchronized void sysViewSync() {
        //check for new servers
        if (sysViewAux.size() > sysView.size()) {
            sysView.clear();
            sysView.addAll(sysViewAux);
        }
        //check if servers exited
        else {
            for (String sysServerId : sysView) {
                if (!sysViewAux.contains(sysServerId)) {
                    sysViewAux.clear();
                    sysViewAux.addAll(sysView);
                }
            }
        }
    }

    /**
     *
     * Fast method to check if new servers connected
     *
     * @return      true if sizes changed, false if not
     */
    private synchronized boolean sysViewChange() {
        return sysViewAux.size() != sysView.size();
    }

    /**
     *
     * Synchronized method change placeManager Leader
     *
     * @param placeMngrLeaderID     New placeManager ID
     */
    private synchronized void setPlaceMngrLeader(String placeMngrLeaderID) {
        placeMngrLeader = placeMngrLeaderID.trim();
    }

    /**
     *
     * Synchronized method to add new server id to sysViewAux
     *
     * @param id    Server ID
     */
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
     * @param params     String with all params split [addplace$3500,Viseu]
     */
    private synchronized void callMethodByName(String methodName, String params) {
        HashMap<String, String> hashParams = Utils.messageDecompressor(params, ";", ",");
        Place newPlace;
        switch (methodName) {
            case "addplace":
                newPlace = new Place(hashParams.get("postalcode").trim(), hashParams.get("locality").trim());
                for (Place place : places) {
                    if (place.getPostalCode().equals(hashParams.get("postalcode").trim())) {
                        return;
                    }
                }
                places.add(newPlace);
                aLogger.newEntry(newPlace.getPostalCode(), newPlace.getLocality(), "addplace");
                //System.out.println("\n\nNew Place Added: " + newPlace.getLocality() + " : " + newPlace.getPostalCode() + "\nPlacemanager: " + placeMngrID);
                break;
            case "removeplace":
                CopyOnWriteArrayList<Place> placesAux = new CopyOnWriteArrayList<>(places);
                for (Place place : placesAux) {
                    if (place.getPostalCode().equals(hashParams.get("postalcode"))) {
                        placesAux.remove(place);
                        places.clear();
                        places.addAll(placesAux);
                        aLogger.newEntry(place.getPostalCode(), place.getLocality(), "removeplace");
                        //System.out.println("\n\nPlace removed: " + hashParams.get("postalcode"));
                    }
                }
                break;
            case "setallplaces":
                /*if (places.size() == 0) {
                    for (String postCode : hashParams.keySet()) {
                        newPlace = new Place(postCode, hashParams.get(postCode));
                        places.add(newPlace);
                        aLogger.newEntry(newPlace.getPostalCode(), newPlace.getLocality(), "addplace");
                    }
                }*/
                break;
            default:
                //code block
        }
    }

    /**
     * RMI placeManager naming lookup
     * @param remotePlaceMngrID     Server ID Hash
     * @return                      RMI address
     */
    private synchronized PlacesListInterface getPlaceMngrRMI(String remotePlaceMngrID) {
        try {
            return (PlacesListInterface) Naming.lookup("rmi://" + sysIPAddr + ":" + sysRMIPort + "/" + remotePlaceMngrID);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

}