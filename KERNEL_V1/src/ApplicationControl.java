import FFDENetwork.FFDEEvent;
import FFDENetwork.FFDEKernel;
import FFDENetwork.FFDEObserver;
import FFDENetwork.FFDEServer;
import jdk.nashorn.internal.objects.Global;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Jakub on 15.10.2016.
 *
 * The very heart of the drone. Provides kernel for local FFDE network, system logging and commands distribution.
 */
public class ApplicationControl implements Runnable {

    private FFDEKernel                              kernel;
//    private FFDEServer                              ffdeLogServer;
//    private HashMap<String, List<List<String>>>     logStorage;
    private GlobalCommunicationGate                 globalCommunicationGate;
    private GlobalCommandServer                     globalCommandServer;
    private GlobalLogger                            globalLogger;

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
        globalLogger = new GlobalLogger();

        // prepare global communication gate for the machine
        globalCommunicationGate = new GlobalCommunicationGate(6667);

        // prepare global command server responsible fot redirecting commands to right recipients
        globalCommandServer = new GlobalCommandServer();

//        Thread th = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true) {
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    System.out.println(logStorage.toString());
//                    System.out.println(ffdeLogServer.getActivePipelines().toString());
//
//                    ffdeLogServer.sendThroughPipeline("globalCommandServer", Arrays.asList("KURWA JEGO JEBANA MAC"));
//                }
//            }
//        });
//        th.start();


        Thread logThread = new Thread(this);
        logThread.setName("main log and application control thread");
        logThread.start();
    }

    @Override
    public void run() {
//        while(true) {
//            try {
//                Thread.sleep(500);
//                System.out.println(globalLogger.getLogStorage().toString());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

    }
}
