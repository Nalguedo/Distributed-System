import utils.Utils;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;


/**
 * Client test class
 */
public class Client {
    public static void main(String[] args) throws InterruptedException {
        FrontendInterface frontendInterface;
        PlacesListInterface placesListInterface;
        Place place;

        System.out.println("Localizar Frontend...");

        int userChoice = 1;

        while (userChoice != 0) {

            userChoice = menu();

            switch (userChoice) {
                case 1:
                    place = clientAddPlace();
                    try {
                        frontendInterface = (FrontendInterface) Naming.lookup("rmi://localhost:2000/frontend");
                        frontendInterface.insertPlace(place.getPostalCode(), place.getLocality());
                    } catch (NotBoundException | RemoteException | MalformedURLException e) {
                        //e.printStackTrace();
                        System.out.println("System down!\n\nTry again...");
                    }

                    break;
                case 2:
                    String placeId = clientGetPlace();
                    try {
                        frontendInterface = (FrontendInterface) Naming.lookup("rmi://localhost:2000/frontend");
                        String serverURL = frontendInterface.requestServer();
                        placesListInterface = (PlacesListInterface) Naming.lookup(serverURL);

                        place = placesListInterface.getPlace(placeId);
                        if (place != null)
                            System.out.println("Result getPlace: " + place.getPostalCode() + " : " + place.getLocality());
                        else
                            System.out.println("Not found!");
                    } catch (NotBoundException | RemoteException | MalformedURLException e) {
                        //e.printStackTrace();
                        System.out.println("System down!\n\nTry again...");
                    }
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    Thread t = (new Thread(() -> {
                        try {
                            PlacesServer.main(new String[]{"3000"});
                            System.out.println("Server starting...");
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }));
                    break;
                case 0:
                    break;
                default:

            }
        }
        /*try {
            //AddPlace
            frontendInterface = (FrontendInterface) Naming.lookup("rmi://localhost:2000/frontend");
            frontendInterface.insertPlace("3510", "Viseu");
            //GetPlace
            String serverURL = frontendInterface.requestServer();
            placesListInterface = (PlacesListInterface) Naming.lookup(serverURL);

            Place place = placesListInterface.getPlace("3510");

            System.out.println("Result getPlace: " + place.getPostalCode() + " : " + place.getLocality());
        } catch (NotBoundException | RemoteException | MalformedURLException e) {
            //e.printStackTrace();
            System.out.println("System down!\n\nTry again...");
        }*/

    }

    public static int menu() {

        int selection;
        Scanner input = new Scanner(System.in);

        System.out.println("Choose option");
        System.out.println("-------------------------\n");
        System.out.println("1 - Add new place");
        System.out.println("2 - Get place");
        System.out.println("3 - Delete place");
        System.out.println("4 - Show all places");
        System.out.println("5 - Star new Server");
        System.out.println("0 - Quit");

        selection = input.nextInt();
        return selection;
    }

    public static Place clientAddPlace() {

        String postalcode = "";
        String locality = "";

        Scanner input = new Scanner(System.in);


        System.out.println("Add new place");
        System.out.println("-------------------------\n");
        System.out.println("Postal Code: ");

        postalcode = input.next();

        System.out.println("\n");
        System.out.println("Locality: ");

        locality = input.next();

        return new Place(postalcode, locality);
    }

    public static String clientGetPlace() {

        Scanner input = new Scanner(System.in);


        System.out.println("Search Place");
        System.out.println("-------------------------\n");
        System.out.println("Postal Code: ");

        return input.next();
    }

}

