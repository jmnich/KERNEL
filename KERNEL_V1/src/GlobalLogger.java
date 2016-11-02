import FFDENetwork.FFDEEvent;
import FFDENetwork.FFDEObserver;
import FFDENetwork.FFDEServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jakub on 20.10.2016.
 *
 * Provides logging services through FFDE.
 */
public class GlobalLogger implements FFDEObserver{

    private FFDEServer ffdeServer;
    private ConcurrentHashMap<String, List<String>> logStorage;

    private static String   logID           = "mainLog";
    private static String   gateID          = "communicationGate";

    public GlobalLogger() {
        ffdeServer = new FFDEServer(logID, 6666, this);
        logStorage = new ConcurrentHashMap<>();

        ffdeServer.openPipeline(gateID);
    }

    public List<String> getLogStorage() {
        List<String> data = new LinkedList<>();

        Enumeration<String> keys = logStorage.keys();

        while(keys.hasMoreElements()) {
            String nextKey = keys.nextElement();
            String nextLine = "";

            for(String singleEntry: logStorage.get(nextKey)) {
                nextLine += nextKey + "  -  " + singleEntry +"\t|\t";
            }

            data.add(nextLine);
        }

        return  data;
    }

    @Override
    public void notifyFFDE(FFDEEvent event) {
        String identifier = event.getIdentifier();

        if(identifier.startsWith("pipe_")) {        //< rx event from any pipeline
            String senderName = identifier.substring(5);

            if(logStorage.containsKey(senderName)) {
                logStorage.get(senderName).add(String.valueOf(event.getMessage()));
            }
            else {
                logStorage.put(senderName, new LinkedList<>());
                logStorage.get(senderName).add(String.valueOf(event.getMessage()));
            }
            //System.out.println("Log got: " + senderName + "  " + event.getMessage().toString());
        }
        else if(identifier.equals("crx")) {
            List<String> message = event.getMessage();
            String firstLine = message.get(0);

            switch(firstLine) {
                case "flushToGate":
                    // send everything from log to global communication gate (first line ID = "systemLogFlush"
                    List<String> logStorageAsMessage = new LinkedList<>();
                    logStorageAsMessage.add("systemLogFlush");

                    Enumeration<String> logUsers = logStorage.keys();

                    while(logUsers.hasMoreElements()) {
                        String user = logUsers.nextElement();

                        for(String record : logStorage.get(user)) {
                            logStorageAsMessage.add(user + "\t|\t" + record);
                        }
                    }

                    ffdeServer.sendThroughPipeline(gateID, logStorageAsMessage);
                    break;

                case "clearLog":
                    logStorage.clear();
                    break;

                default:
                    break;
            }
        }
    }
}
