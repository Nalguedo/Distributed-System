import utils.CLogger;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;

public class FrontendServer {

    public static void main(String[] args) {
        Registry clientRegistry;
        Registry sysRegistry;
        Frontend frontend;

        try{
            InetAddress address = InetAddress.getByName("230.0.0.0");
            int multicastPort = 6789;
            int RMIPortClient = 2000;
            int RMIPortSystem = 3000;
            CLogger LogFile= new CLogger("Frontend");

            frontend = new Frontend(address, multicastPort, RMIPortSystem, LogFile);

            //Create RMI reachable by clients
            clientRegistry = LocateRegistry.createRegistry(RMIPortClient);
            clientRegistry.rebind("frontend", frontend );

            //Create RMI system registry
            sysRegistry = LocateRegistry.createRegistry(RMIPortSystem);
            sysRegistry.rebind("frontend", frontend );
        }
        catch(RemoteException | UnknownHostException e) {
            System.out.println("Frontend server " + args[0] + " main " + e.getMessage());
        }
    }
}
