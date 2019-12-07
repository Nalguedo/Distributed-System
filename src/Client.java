import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;


/**
 * Client test class
 */
public class Client {
    public static void main(String[] args) {
        FrontendInterface frontendInterface;
        PlacesListInterface placesListInterface;
        Place place;
        String placeId;
        ArrayList<Place> placeArrayList;

        System.out.println("Localizar Frontend...");

        int userChoice = 1;

        while (userChoice != 0) {

            userChoice = menu();

            switch (userChoice) {
                case 1:
                    place = clientAddPlace();
                    try {
                        frontendInterface = (FrontendInterface) Naming.lookup("rmi://localhost:2000/frontend");
                        frontendInterface.insertPlace(place.getPostalCode().trim(), place.getLocality().trim());
                    } catch (NotBoundException | RemoteException | MalformedURLException e) {
                        //e.printStackTrace();
                        System.out.println("System down!\n\nTry again...");
                    }

                    break;
                case 2:
                    placeId = clientGetPlace();
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
                    placeId = clientDeletePlace();
                    try {
                        frontendInterface = (FrontendInterface) Naming.lookup("rmi://localhost:2000/frontend");
                        frontendInterface.removePlace(placeId);
                    } catch (NotBoundException | RemoteException | MalformedURLException e) {
                        //e.printStackTrace();
                        System.out.println("System down!\n\nTry again...");
                    }
                    break;
                case 4:
                    try {
                        frontendInterface = (FrontendInterface) Naming.lookup("rmi://localhost:2000/frontend");
                        String serverURL = frontendInterface.requestServer();
                        placesListInterface = (PlacesListInterface) Naming.lookup(serverURL);

                        placeArrayList = placesListInterface.allPlaces();
                        if (placeArrayList != null) {
                            System.out.println("List of all places:");
                            for (Place p : placeArrayList) {
                                System.out.println("Postalcode: " + p.getPostalCode() + "\tLocality: " + p.getLocality());
                            }
                        }
                        else
                            System.out.println("Not found!");
                    } catch (NotBoundException | RemoteException | MalformedURLException e) {
                        //e.printStackTrace();
                        System.out.println("System down!\n\nTry again...");
                    }
                    break;
                default:
            }
        }
    }

    public static int menu() {

        int selection;
        Scanner input = new Scanner(System.in);

        System.out.println("\n\n");
        System.out.println("Choose option");
        System.out.println("-------------------------");
        System.out.println("1 - Add new place");
        System.out.println("2 - Get place");
        System.out.println("3 - Delete place");
        System.out.println("4 - Show all places");
        System.out.println("0 - Quit");

        selection = input.nextInt();
        return selection;
    }

    public static Place clientAddPlace() {

        String postalcode;
        String locality;

        Scanner input = new Scanner(System.in);


        System.out.println("Add new place");
        System.out.println("-------------------------");
        System.out.println("Postal Code: ");

        postalcode = input.next();

        System.out.println("Locality: ");

        locality = input.next();

        return new Place(postalcode, locality);
    }

    public static String clientGetPlace() {

        Scanner input = new Scanner(System.in);


        System.out.println("Search Place");
        System.out.println("-------------------------");
        System.out.println("Postal Code: ");

        return input.next();
    }

    public static String clientDeletePlace() {

        Scanner input = new Scanner(System.in);


        System.out.println("Delete Place");
        System.out.println("-------------------------");
        System.out.println("Postal Code: ");

        return input.next();
    }

}

