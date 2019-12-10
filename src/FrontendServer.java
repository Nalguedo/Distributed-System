import utils.CLogger;
import utils.Utils;

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
        String ipAddress = Utils.getIpAddress();
        System.setProperty("java.rmi.server.hostname", ipAddress);
        System.out.println("--------------------------------------------------------------\n\nINFORMATION: Set this IP Address on client - " + ipAddress + "\n\n--------------------------------------------------------------");

        try{
            InetAddress multicastAddress = InetAddress.getByName("230.0.0.0");
            int multicastPort = 6789;
            int RMIPortClient = 1099;
            int RMIPortSystem = 3000;
            CLogger LogFile= new CLogger("Frontend");

            frontend = new Frontend(ipAddress, multicastAddress, multicastPort, RMIPortSystem, LogFile);

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
