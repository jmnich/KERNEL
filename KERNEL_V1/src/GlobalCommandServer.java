import FFDENetwork.FFDEEvent;
import FFDENetwork.FFDEObserver;
import FFDENetwork.FFDEServer;

import java.util.*;

/**
 * Created by Jakub on 18.10.2016.
 *
 * Handles input from GlobalCommunicationGate and redirects it to proper recipients.
 */
public class GlobalCommandServer implements FFDEObserver{

    private FFDEServer                      ffdeServer;
    private HashMap<String, List<String>>   commandRecipients;

    private static String                   loggerID        = "mainLog";
    private static String                   commandServerID = "globalCommandServer";

    public GlobalCommandServer() {
        ffdeServer = new FFDEServer(commandServerID, 6666, this);
        commandRecipients = new LinkedHashMap<>();

        ffdeServer.openPipeline(loggerID);  //< open connection channel with the main logger

        // create a thread that will wait until FFDE is up and transmit hello world message to global log
        Thread reportToLogAgent = new Thread(new Runnable() {
            @Override
            public void run() {
                ffdeServer.waitUntilNetworkIsReady();

                ffdeServer.sendThroughPipeline(loggerID, Arrays.asList(String.valueOf(System.nanoTime()), "Global " +
                        "command server up"));
            }
        });
        reportToLogAgent.start();
    }

    // method called by communication gate when command is received
    public void handleCommand(List<String> message) {
        String commandIdentifier = message.get(0);

        // transmit command to all recipients who requested it's identifier
        for(String recipient : commandRecipients.keySet()) {
            for(String requestedCommandID : commandRecipients.get(recipient)) {
                if(requestedCommandID.equals(commandIdentifier)) {
                    ffdeServer.sendThroughPipeline(recipient, message);
                }
            }
        }
    }

    @Override
    public void notifyFFDE(FFDEEvent event) {
        String identifier = event.getIdentifier();

        // check who sent the command
        if(identifier.startsWith("pipe_")) {
            // if the command came from any pipeline assume that sender wants to order a certain command feed
            String senderName = identifier.substring(5);

            // check if the sender is already registered in the base of command feed recipients
            Set<String> registeredRecipients = commandRecipients.keySet();
            if(registeredRecipients.contains(senderName)) {     //< is registered
                commandRecipients.get(senderName).add(event.getMessage().get(0));   //< assign command type to recipient
            }
            else {  //< isn't registered
                commandRecipients.put(senderName, new LinkedList<>());              //< register new recipient
                commandRecipients.get(senderName).add(event.getMessage().get(0));   //< assign command type to recipient
            }
            // note 1 : command feed request contains only one line - String with requested command identifier
            // note 2: command identifier can not be mistaken for FFDEEvent source identifier
        }
        else if(identifier.equals("crx")) {
            // if the master (GlobalCommunicationGate) send data assume it is a command for retransmission
            handleCommand(event.getMessage());
        }
        else {
            ffdeServer.sendThroughPipeline(loggerID, Arrays.asList(String.valueOf(System.nanoTime()), "global command" +
                    " server got message different than command feed request"));
        }
    }
}
