/**
 * Initialize Frontend server and x PlaceManager instances
 *
 * @version 1.1
 */
public class Initializer {
    public static void main (String[] args) throws InterruptedException {
        Thread t = (new Thread(() -> {
            try {

                FrontendServer.main(new String[]{"2000"});

                for (int i = 0; i < 5; i++) {
                    PlacesServer.main(new String[]{"3000"});
                    Thread.sleep(1000);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        t.start();
    }
}

