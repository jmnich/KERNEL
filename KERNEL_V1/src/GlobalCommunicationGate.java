import FFDENetwork.FFDEEvent;
import FFDENetwork.FFDEObserver;
import FFDENetwork.FFDEServer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetConnectedException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Jakub on 16.10.2016.
 *
 * If this gate receives anything through FFDE it immediately transmits it through the web. I accepts only commands
 * directly addressed to it.
 */
public class GlobalCommunicationGate implements Runnable, FFDEObserver{

    private Socket  socket;
    private BufferedWriter  tx;
    private Scanner     rx;
    private FFDEServer ffdeServer;
    private AtomicBoolean gateActive;       //< thread safe flag
    private List<String> rxMessageBuffer;

    private static String   loggerID        = "mainLog";
    private static String   gateID          = "communicationGate";
    private static String   commandServerID = "globalCommandServer";


    public GlobalCommunicationGate(int requestedServerPort) {
        gateActive = new AtomicBoolean();
        gateActive.set(false);    //< gate is not connected

        rxMessageBuffer = new LinkedList<>();

        // prepare server socket listener
        Thread serverSocketListener = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // wait until user connects to a socket
                    ServerSocket serverSocket = new ServerSocket(requestedServerPort);
                    socket = serverSocket.accept();
                    // after binding the socket encapsulate data streams into more user-friendly objects
                    rx = new Scanner(new InputStreamReader(socket.getInputStream()));
                    tx = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    // log successful socket binding
                    transmitInternal(loggerID, Arrays.asList(String.valueOf(System.nanoTime()), "Global " +
                            "communication gate connected"));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        serverSocketListener.start();

        // set up FFDE communication gate server
        ffdeServer = new FFDEServer(gateID, 6666, this);
        ffdeServer.openPipeline(loggerID);          //< establish communication with the main logger
        ffdeServer.takeControl(commandServerID);    //< take control over GlobalCommandServer

        // start gate's own thread
        Thread th = new Thread(this);
        th.setName("Global communications gate main thread");
        th.start();
    }

    private void transmitInternal(String destination, List<String> message) {
        ffdeServer.sendThroughPipeline(destination, message);
    }

    private void transmitExternal(List<String> message) throws NotYetConnectedException {
        if(!gateActive.get()) {
            throw new NotYetConnectedException();
        }
        else {
            // transmit message through net and add "$" at the end
            for(String messageLine : message) {
                try {
                    tx.write(messageLine);
                    tx.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                tx.write("$");
                tx.newLine();
                tx.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        ffdeServer.waitUntilNetworkIsReady();
        transmitInternal(loggerID, Arrays.asList(String.valueOf(System.nanoTime()), "Global communication gate up"));

        while(true) {
            if(rx.hasNext()) {
                String messageLine = rx.next();

                if(messageLine.equals("$")) {
                    // if whole message is already buffered pass it to GlobalCommandServer
                    ffdeServer.sendToSlave(commandServerID, rxMessageBuffer);
                    rxMessageBuffer.clear();
                }
                else
                    rxMessageBuffer.add(messageLine);
            }
            else{
                // suspend the thread to save resources
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void notifyFFDE(FFDEEvent event) {
        String firstLine = event.getMessage().get(0);

        // check if the command is addressed to the gate itself
        switch(firstLine) {

//            case "bagno":
//                break;

            default:
                // if the command was not recognized as internal transmit it through net
                try {
                    transmitExternal(event.getMessage());
                } catch(NotYetConnectedException e) {
                    transmitInternal(loggerID, Arrays.asList(String.valueOf(System.nanoTime()), "Gate error: could " +
                            "not retransmit external message due to inactive net connection"));
                }
                break;
        }
    }
}
