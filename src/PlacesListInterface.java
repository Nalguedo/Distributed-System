import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PlacesListInterface extends Remote {
    boolean addPlace(Place p) throws RemoteException;
    ArrayList<Place> allPlaces() throws RemoteException;
    Place getPlace(String objectID) throws RemoteException;
    boolean removePlace(String objectID) throws RemoteException;
}
