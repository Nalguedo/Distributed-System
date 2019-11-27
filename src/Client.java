
public class Client {
    public static void main (String[] args) throws InterruptedException {
        //Run 3 instances of the place manager
        Thread t = (new Thread() {
            public void run() {
                try {
                    PlacesServer.main(new String[]{"1"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"3"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"4"});

                    Thread.sleep(1000);

                    /*PlacesServer.main(new String[]{"5"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"6"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"7"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"8"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"9"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"10"});*/

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}

