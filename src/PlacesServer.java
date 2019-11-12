import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class PlacesServer {

    public static void main (String[] args) {
        try{
            PlacesManager placeList = new PlacesManager();
            String message = "Place server " + args[0] + " ready";
            InetAddress address = InetAddress.getByName("230.0.0.0");
            int port = 6789;

            try {
                MulticastSocket multicastSocket = new MulticastSocket(port);
                multicastSocket.joinGroup(address);
                DatagramPacket msg = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
                try {
                    multicastSocket.send(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Thread t = (new Thread() {
                    public void run() {
                        while(true) {
                            byte[] buffer = new byte[1000];
                            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                            try {
                                multicastSocket.receive(reply);
                                System.out.println("Reply: " + new String(reply.getData()));
                                if(new String(reply.getData()).equals(message)){
                                    break;
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            multicastSocket.leaveGroup(address);
                            multicastSocket.close();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                });
                t.start();

            }
            catch(IOException e){
                e.printStackTrace();
            }

        }
        catch(Exception e) {
            System.out.println("Place server " + args[0] + " main " + e.getMessage());
        }
    }
}
