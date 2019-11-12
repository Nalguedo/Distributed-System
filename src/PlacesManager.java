import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private ArrayList<Place> places;

    public PlacesManager(ArrayList<Place> places) throws RemoteException {
        this.places = places;
    }

    PlacesManager() throws RemoteException {
        places = new ArrayList<>();
    }

    @Override
    public void addPlace(Place p) throws RemoteException {
        places.add(p);
    }

    @Override
    public ArrayList<Place> allPlaces() throws RemoteException {
        return places;
    }

    @Override
    public Place getPlace(String objectID) throws RemoteException {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getPostalCode().equals(objectID)){
                return places.get(i);
            }
        }
        return null;
    }
}
