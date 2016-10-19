import FFDENetwork.FFDEEvent;
import FFDENetwork.FFDEKernel;
import FFDENetwork.FFDEObserver;
import FFDENetwork.FFDEServer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Jakub on 15.10.2016.
 *
 * The very heart of the drone.
 */
public class ApplicationControl implements Runnable, FFDEObserver{

    private FFDEKernel                              kernel;
    private FFDEServer                              ffdeLogServer;
    private HashMap<String, List<List<String>>>     logStorage;
    private GlobalCommunicationGate                 globalCommunicationGate;
    private GlobalCommandServer                     globalCommandServer;

    public ApplicationControl() {
        // prepare kernel
        kernel = new FFDEKernel(6666);

        // wait until kernel is up and running - most probably not required for proper functioning
//        try {
//            Thread.sleep(20);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // prepare main log server
        ffdeLogServer = new FFDEServer("mainLog", 6666, this);


        logStorage = new HashMap<>();

        // prepare global communication gate for the machine
        globalCommunicationGate = new GlobalCommunicationGate(6667);

        // prepare global command server responsible fot redirecting commands to right recipients
        globalCommandServer = new GlobalCommandServer();


        Thread logThread = new Thread(this);
        logThread.setName("main log and application control thread");
        logThread.start();
    }

    @Override
    public void run() {
        ffdeLogServer.waitUntilNetworkIsReady();

        //System.out.println("Kernel ready, state:    " + kernel.getCurrentState());

        // TODO nie wiem czy to jest konieczne

    }

    @Override
    public void notifyFFDE(FFDEEvent event) {
        String identifier = event.getIdentifier();

        if(identifier.startsWith("pipe_")) {        //< rx event from any pipeline
            String senderName = identifier.substring(5);

            if(logStorage.containsKey(senderName)) {
                logStorage.get(senderName).add(event.getMessage());
            }
            else {
                logStorage.put(senderName, new LinkedList<>());
                logStorage.get(senderName).add(event.getMessage());
            }

            //System.out.println("Log got: " + senderName + "  " + event.getMessage().toString());
        }
    }
}
