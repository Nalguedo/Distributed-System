import utils.CLogger;

import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;

public class PlacesServer {

    public static void main(String[] args) {
        Registry r;
        PlacesManager placeList;

        try{
            //multicast address used
            InetAddress address = InetAddress.getByName("230.0.0.0");
            //port used
            int port = 6789;
            //create PlacesManager
            CLogger LogFile= new CLogger(args[0]);
            placeList = new PlacesManager(address, port, LogFile);

            r = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
            r.rebind("placelist", placeList );
        }
        catch(RemoteException | UnknownHostException e) {
            System.out.println("Place server " + args[0] + " main " + e.getMessage());
        }
    }
}
