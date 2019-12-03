import utils.CLogger;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;

public class FrontendServer {

    public static void main(String[] args) {
        Registry r;
        Frontend frontend;

        try{
            //multicast address used
            InetAddress address = InetAddress.getByName("230.0.0.0");
            //port used
            int port = 6789;
            //create Frontend
            CLogger LogFile= new CLogger(args[0]);
            frontend = new Frontend(address, port, LogFile);

            r = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
            r.rebind("frontend", frontend );
        }
        catch(RemoteException | UnknownHostException e) {
            System.out.println("Frontend server " + args[0] + " main " + e.getMessage());
        }
    }
}
