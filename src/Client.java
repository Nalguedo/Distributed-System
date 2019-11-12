
public class Client {
    public static void main (String[] args) throws InterruptedException {
        Thread t = (new Thread() {
            public void run() {
                try {
                    PlacesServer.main(new String[]{"1"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"3"});

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}

