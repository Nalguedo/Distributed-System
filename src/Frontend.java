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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Frontend extends UnicastRemoteObject implements FrontendInterface {
    private static final long serialVersionUID = 1L;
    //Flags
    private boolean terminateFlag = false;
    private boolean sysChangeFlag = false;
    //List of places
    private ArrayList<Place> places = new ArrayList<>();
    //System View HashMap - <hashID, Leader>
    private ArrayList<String> sysView = new ArrayList<>();
    //Auxiliary HashMap - <hashID, Leader>
    private ArrayList<String> sysViewAux = new ArrayList<>();
    //Leader ID
    private String placeMngrLeader = "noleader";
    //Frontend ID
    private String frontendID;
    //System RMI Port
    private int sysRMIPort;
    //System IP Multicast Address
    private InetAddress sysAddr;
    //System IP Address
    private String sysIPAddr;

    /**
     * Frontend responsible class, assure communication between clients and system servers
     *
     * @param ipAddress         Server IP Address
     * @param mcastAddr         Network multicast address
     * @param multicastPort     Multicast Port
     * @param rmiPort           Registry port
     * @param LogFile           Server specific log
     */
    Frontend(String ipAddress, InetAddress mcastAddr, int multicastPort, int rmiPort, CLogger LogFile) throws RemoteException {
        //Thread ID
        Thread threadID = Thread.currentThread();
        Instant instant = Instant.now();
        sysAddr = mcastAddr;
        sysIPAddr = ipAddress;
        //PlaceManager Multicast Port
        sysRMIPort = rmiPort;
        frontendID = Utils.hashString(String.valueOf(multicastPort) + threadID + instant.toEpochMilli()).trim();

        //Create multicast socket
        try {
            //bind socket to the port
            MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
            //join the group in the specified address
            multicastSocket.joinGroup(mcastAddr);

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

    /**
     *
     * Remote call from clients to add new places, set new place will be called on Placemanager Leader
     *
     * @param postalCode        New place postal code
     * @param locality          New locality
     * @return                  True if success, false if no
     */
    @Override
    public boolean insertPlace(String postalCode, String locality) throws RemoteException {
        Place newPlace = new Place(postalCode, locality);
        PlacesListInterface placesListInterface = getRemotePlaceMngr(placeMngrLeader);

        if (placesListInterface == null)
            return false;

        return placesListInterface.addPlace(newPlace);
    }

    /**
     * Leader RMI removePlace call
     * @param postalCode    Postal Code
     * @return              True if remove is successful, no if it's not
     */
    @Override
    public boolean removePlace(String postalCode) throws RemoteException {
        PlacesListInterface placesListInterface = getRemotePlaceMngr(placeMngrLeader);

        if (placesListInterface == null)
            return false;

        return placesListInterface.removePlace(postalCode.trim());
    }

    /**
     * Request random server RMI address
     * @return  RMI address
     */
    @Override
    public String requestServer() {
        Random randId = new Random();
        int serverId;

        if (sysView.size() > 0) {
            serverId = randId.nextInt(sysView.size());
            return "rmi://" + sysIPAddr + ":" + sysRMIPort + "/" + sysView.get(serverId);
        }
        return null;
    }

    /**
     *
     * System Leader election - highest ID hash on sysViewAux (fresh sysView) will be considered the System Leader Candidate
     */
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
     * RMI naming lookup
     * @param remotePlaceMngrID     Server ID Hash
     * @return                      RMI address
     */
    private synchronized PlacesListInterface getRemotePlaceMngr(String remotePlaceMngrID) {
        try {
            return (PlacesListInterface) Naming.lookup("rmi://" + sysIPAddr + ":" + sysRMIPort + "/" + remotePlaceMngrID);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

}
