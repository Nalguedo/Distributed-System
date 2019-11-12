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
            //multicast address used
            InetAddress address = InetAddress.getByName("230.0.0.0");
            //port used
            int port = 6789;

            try {
                //bind socket to the port
                MulticastSocket multicastSocket = new MulticastSocket(port);
                //join the group in the specified address
                multicastSocket.joinGroup(address);
                //create a new datagram packet
                DatagramPacket msg = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
                try {
                    //send the datagram packet
                    multicastSocket.send(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //create a new thread to listen to other server's messages
                Thread t = (new Thread() {
                    public void run() {
                        while(true) {
                            byte[] buffer = new byte[1000];
                            //create a packet to receive the message
                            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                            try {
                                //receive the message and print it on the screen
                                multicastSocket.receive(reply);
                                String received = reply.getData().toString();
                                System.out.println("Reply: " + received);

                                if(received.equals(message)){
                                    break;
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            //leave the group and close the socket
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
