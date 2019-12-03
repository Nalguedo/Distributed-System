
public class Initializer {
    public static void main (String[] args) throws InterruptedException {
        //Run 3 instances of the place manager
        Thread t = (new Thread() {
            public void run() {
                try {



                    PlacesServer.main(new String[]{"2001"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2002"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2003"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2004"});

                    /*Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2005"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2006"});

                    Thread.sleep(5000);

                    PlacesServer.main(new String[]{"2007"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2008"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2009"});

                    Thread.sleep(1000);

                    PlacesServer.main(new String[]{"2010"});*/

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}

