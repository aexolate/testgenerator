import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TestGenerator {
    static Random r = new Random();

    // Generate a random string that is used to represent an instrument
    public static String generateString(int length) {
        String buf = "";
        for(int i=0; i<length; i++) {
            char c = (char)(r.nextInt(26) + 'A');
            buf += c;
        }
        return buf;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Customizable Parameters
        int NUM_INSTRUMENTS = 200;                           // Up to 500
        int NUM_CLIENTTHREADS = 10;                          // Basic = 1, Medium = 4. Hard = 40
        int AVG_CMDS_PER_INSTR = 20000;                      // Expect 10k-50k per instrument
        int MIN_PRICE = 100;
        int MAX_PRICE = 500;
        int MIN_COUNT = 1;
        int MAX_COUNT = 500;
        
        // Less important customisable parameter
        int STARTING_ORDER_ID = 100;
        boolean INCLUDE_CANCEL_ORDERS = true;
        boolean INCLUDE_BARRIERS = false;
        double CANCEL_CHANCE = 0.35;                                // Chance a cancel order will be generated
        double BARRIER_CHANCE = 0.02;                               // Chance a barrier will be generated
        int NUM_CMDS = AVG_CMDS_PER_INSTR * NUM_INSTRUMENTS;        // Set maximum number of commands
        int INSTRUMENT_LEN = 3;                                     // Length of the instrument string
        String OUTPUT_FILE = "test.in";

        // Setup variables
        int _incl_cancel = INCLUDE_CANCEL_ORDERS ? 1 : 0;
        List<String> instruments = new ArrayList<>();               //Contains all the instruments available in the run
        char[] ORDERTYPES = {'S', 'B', 'C'};
        HashMap<Integer, Integer> order_thread = new HashMap<>();   //Maps the orderID to the thread number
        int order_id = STARTING_ORDER_ID;
        String buf = "";
        FileWriter writer = new FileWriter(OUTPUT_FILE);

        // Generate Instruments
        for(int i=0; i<NUM_INSTRUMENTS; i++) {
            String instr = generateString(INSTRUMENT_LEN);
            instruments.add(instr);
        }

        System.out.println("Generating " + NUM_CMDS + " orders");
        if (NUM_CMDS > 1000000) {
            System.out.println("Warning: Large file will be generated, proceeding in 3s.");
            Thread.sleep(3000);
        }

        // Setup Complete
        // =======================================================
        // Start generating output

        writer.write(Integer.toString(NUM_CLIENTTHREADS) + "\n");

        // Connect Threads
        writer.write("o\n");

        // Generate Orders
        for(int i=0; i<NUM_CMDS; i++) {
            buf = "";       //clear output buffer

            // Generate random barriers
            if (INCLUDE_BARRIERS && r.nextDouble() < BARRIER_CHANCE) {
                writer.write(".\n");
                i--;    continue;
            }

            //  Generate random parameters
            char ordertype = ORDERTYPES[r.nextInt(2 + _incl_cancel)];
            if (ordertype == 'C') {
                //Rig cancel orders to make it less frequent & check bounds
                if (r.nextDouble() > CANCEL_CHANCE || (STARTING_ORDER_ID == order_id)) {
                    i--;    continue;
                }


                //Cancel Order
                int target_id = ThreadLocalRandom.current().nextInt(STARTING_ORDER_ID, order_id);


                //Get which thread created the order
                if (NUM_CLIENTTHREADS > 1) {
                    buf += Integer.toString(order_thread.get(target_id)) + " ";
                }

                buf += "C " + Integer.toString(target_id) + "\n";
                writer.write(buf);
            } else {
                //Buy/Sell Orders

                String instrument = instruments.get(r.nextInt(instruments.size()));
                int price = ThreadLocalRandom.current().nextInt(MIN_PRICE, MAX_PRICE);
                int count = ThreadLocalRandom.current().nextInt(MIN_COUNT, MAX_COUNT);


                //Choose a random thread if client threads > 1
                if (NUM_CLIENTTHREADS > 1) {
                    int thread_num = r.nextInt(NUM_CLIENTTHREADS);
                    buf += Integer.toString(thread_num) + " ";
                    order_thread.put(order_id, thread_num);
                }
                buf += ordertype + " " + order_id + " " + instrument + " " 
                + Integer.toString(price) + " " + Integer.toString(count) + "\n";

                writer.write(buf);
                order_id++;
            }
        }

        // Disconnect Threads
        writer.write("x\n");
        writer.close();

        File file = new File(OUTPUT_FILE);
        System.out.println("Finished generating " + file.length()/(1024*1024) + "MB");
    }
}