import utils.Utils;

import java.io.File;
import java.util.Arrays;

/**
 * Initialize Frontend server and x PlaceManager instances
 *
 * @version 1.1
 *
 */
public class Initializer {
    public static void main (String[] args) {

        //Test Print current project directory
        System.out.println(Utils.CurrDirectory());
        //Clear old log files
        File path = new File(Utils.CurrDirectory() + "/Logs/");
        try {
            Arrays.stream(path.listFiles()).forEach(File::delete);
        } catch (NullPointerException e) {
            //e.printStackTrace();
            if (new File("Logs").mkdirs()) {
                System.out.println("First run...\nLogs directory created!");
            }
            else {
                //File error exit
                System.out.println("System error... check filesystem permissions");
                return;
            }
        }

        Thread t = (new Thread(() -> {
            try {

                FrontendServer.main(new String[]{"2000"});

                for (int i = 0; i < 5; i++) {
                    PlacesServer.main(new String[]{"3000"});
                    Thread.sleep(1000);
                }

                Thread.sleep(60000);
                PlacesServer.main(new String[]{"3000"});

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        t.start();
    }
}

