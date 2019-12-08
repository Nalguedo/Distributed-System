import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FrontendInterface extends Remote {
    boolean insertPlace(String postalCode, String locality) throws RemoteException;
    boolean removePlace(String postalCode) throws RemoteException;
    String requestServer() throws RemoteException;
}
