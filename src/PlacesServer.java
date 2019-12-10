import utils.CLogger;
import utils.Utils;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;
import java.time.Instant;

public class PlacesServer {

    public static void main(String[] args) {
        Registry sysRegistry;
        PlacesManager placeList;
        String placeMngrID;
        Thread threadID = Thread.currentThread();

        try{
            InetAddress address = InetAddress.getByName("230.0.0.0");
            int multicastPort = 6789;
            int RMIPortSystem = 3000;
            Instant instant = Instant.now();
            placeMngrID = Utils.hashString(String.valueOf(multicastPort) + threadID + instant.toEpochMilli()).trim();
            CLogger LogFile= new CLogger(placeMngrID);
            placeList = new PlacesManager(address, multicastPort, RMIPortSystem, placeMngrID,LogFile);

            //Join RMI system registry using same port and unique id
            sysRegistry = LocateRegistry.getRegistry(RMIPortSystem);
            sysRegistry.rebind(placeMngrID, placeList );
        }
        catch(RemoteException | UnknownHostException e) {
            System.out.println("Place server " + args[0] + " main " + e.getMessage());
        }
    }
}
