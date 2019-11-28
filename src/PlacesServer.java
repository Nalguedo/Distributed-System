import utils.CLogger;

import java.net.InetAddress;

public class PlacesServer {

    public static void main(String[] args) {
        try{
            //multicast address used
            InetAddress address = InetAddress.getByName("230.0.0.0");
            //port used
            int port = 6789;
            //create PlacesManager
            CLogger LogFile= new CLogger(args[0]);
            PlacesManager placeList = new PlacesManager(address, port);
        }
        catch(Exception e) {
            System.out.println("Place server " + args[0] + " main " + e.getMessage());
        }
    }
}
