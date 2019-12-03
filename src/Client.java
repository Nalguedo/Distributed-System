import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        PlacesListInterface frontendInterface;

        System.out.println("Localizar Frontend...");

        try {
            frontendInterface = (PlacesListInterface) Naming.lookup("rmi://localhost:2000/frontend");

            Place p1 = new Place("3510", "Viseu");
            frontendInterface.addPlace(p1);

            Place p2 = frontendInterface.getPlace("3510");
            System.out.println("Result getPlace: " + p2.getPostalCode()  + " : " + p2.getLocality());
        } catch (NotBoundException | RemoteException | MalformedURLException e) {
            //e.printStackTrace();
            System.out.println("System down!\n\nTry again...");
        }

    }
}

