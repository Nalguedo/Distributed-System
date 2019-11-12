import java.io.Serializable;

public class Place implements Serializable {
    private String postalCode;
    private String locality;

    private static final long serialVersionUID = 1L;

    public Place(String postalCode, String locality) {
        this.postalCode = postalCode;
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }
}

