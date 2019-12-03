import utils.CLogger;
import utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class Frontend extends UnicastRemoteObject implements PlacesListInterface {
    //Flags
    private boolean terminateFlag = false;
    private boolean sysChangeFlag = false;
    //List of places
    private ArrayList<Place> places = new ArrayList<>();
    //System View HashMap - <hashID, Leader>
    private ArrayList<String> sysView = new ArrayList<>();
    //Auxiliary HashMap - <hashID, Leader>
    private ArrayList<String> sysViewAux = new ArrayList<>();
    //Candidate ID
    private String placeMngrLeaderCandidate;
    //Leader ID
    private String placeMngrLeader = "noleader";
    //Frontend ID
    private String frontendID;
    //PlaceManager Multicast Port
    private int frontendPort;
    //System RMI Port
    private int sysRMIPort;
    //System IP Address
    private InetAddress sysAddr;

    Frontend(InetAddress addr, int multicastPort, int rmiPort, CLogger LogFile) throws RemoteException {
        //Thread ID
        Thread threadID = Thread.currentThread();
        sysAddr = addr;
        frontendPort = multicastPort;
        sysRMIPort = rmiPort;
        frontendID = Utils.hashString(frontendPort, threadID).trim();

        //Create multicast socket
        try {
            //bind socket to the port
            MulticastSocket multicastSocket = new MulticastSocket(frontendPort);
            //join the group in the specified address
            multicastSocket.joinGroup(addr);

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
                                    //Store received Placemanager ID
                                    addSysViewAux(messagesAux.get(type));
                                    break;
                                case "setleader":
                                    setPlaceMngrLeader(messagesAux.get("setleader"));
                                    break;
                                default:
                                    // code block
                            }
                        }

                        System.out.println("S: " + sysView.size() +  " Reply: " + received.trim() + " Who Received: Frontend");
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
                    System.out.println("FRONTEND EXIT!!!!!");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            threadListen.start();

            //Only main thread will start thread
            if(threadID == Thread.currentThread()) {

                //Create a new thread to keep sysView updated
                Thread threadSend = (new Thread(() -> {
                    while (!terminateFlag) {
                        try {
                            //Setup time wait
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //check leader candidate each iteration
                        sysLeaderElection();
                        sysChangeFlag = !sysChangeFlag;

                        if (!sysChangeFlag) {
                            sysViewSync();
                        }
                        else {
                            sysViewAux.clear();
                        }

                        System.out.println("\n\nFrontend:" + frontendID + "\nSelected Lider:" + placeMngrLeader);
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
    public void addPlace(Place p) throws RemoteException {
        PlacesListInterface placesListInterface = getRemotePlaceMngr(placeMngrLeader);
        if (placesListInterface != null) {
            placesListInterface.addPlace(p);
        }
    }

    @Override
    public ArrayList<Place> allPlaces() throws RemoteException {
        PlacesListInterface placesListInterface = getRemotePlaceMngr(placeMngrLeader);
        assert placesListInterface != null;
        return placesListInterface.allPlaces();
    }

    @Override
    public Place getPlace(String objectID) throws RemoteException {
        //TODO ask random server for place
        PlacesListInterface placesListInterface = getRemotePlaceMngr(placeMngrLeader);
        assert placesListInterface != null;
        return placesListInterface.getPlace(objectID);
    }

    @Override
    public boolean removePlace(String objectID) throws RemoteException {
        PlacesListInterface placesListInterface = getRemotePlaceMngr(placeMngrLeader);
        assert placesListInterface != null;
        return placesListInterface.removePlace(objectID);
    }

    private synchronized void sysLeaderElection() {
        String max = "";

        for(String entry : sysViewAux) {
            if (entry.compareTo(max) > 0) {
                max = entry ;
            }
        }
        //Set highest hash as leader candidate
        if (placeMngrLeader.equals("noleader"))
            setPlaceMngrLeader(max);
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

    private synchronized void setPlaceMngrLeader(String placeMngrLeaderID) {
        placeMngrLeader = placeMngrLeaderID.trim();
    }

    private synchronized void addSysViewAux(String id) {
        String idTrimmed = id.trim();
        if (!sysViewAux.contains(idTrimmed)) {
            sysViewAux.add(idTrimmed);
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
