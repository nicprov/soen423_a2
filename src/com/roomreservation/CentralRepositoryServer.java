package com.roomreservation;

import com.roomreservation.collection.Entry;
import com.roomreservation.collection.Node;
import com.roomreservation.collection.Position;
import com.roomreservation.collection.LinkedPositionalList;
import com.roomreservation.protobuf.protos.CentralRepository;
import com.roomreservation.protobuf.protos.CentralRepositoryAction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import static com.roomreservation.common.ConsoleColours.ANSI_RED;
import static com.roomreservation.common.ConsoleColours.RESET;

public class CentralRepositoryServer {

    private static final int MIN_PORT = 1025;
    private static final int MAX_PORT = 65000;

    private static LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> repository;
    private static ArrayList<Integer> usedPorts;

    public static void main(String[] args){
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket(1024);
            System.out.println("Central repository ready");
            byte[] buffer = new byte[1000];

            // Initialize with udp and rmi as types
            usedPorts = new ArrayList<>();
            repository = new LinkedPositionalList<>();
            repository.addFirst(new Node<>("udp", new LinkedPositionalList<>()));
            repository.addFirst(new Node<>("rmi", new LinkedPositionalList<>()));

            while (true){
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);

                // Launch a new thread for each request
                DatagramSocket finalDatagramSocket = datagramSocket;
                Thread thread = new Thread(() -> {
                    try {
                        handleUDPRequest(finalDatagramSocket, datagramPacket);
                    } catch (IOException e) {
                        System.out.println(ANSI_RED + "Exception: " + e.getMessage() + RESET);
                    }
                });
                thread.start();
            }
        } catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
            System.exit(1);
        } catch (IOException e){
            System.out.println("IO Exception: " + e.getMessage());
            System.exit(1);
        }  catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            System.exit(1);
        }
        finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
    }

    /**
     * Thread method to processed incoming UDP request
     * @param datagramSocket Datagram socket
     * @param datagramPacket Datagram packet
     * @throws IOException Exception
     */
    private static void handleUDPRequest(DatagramSocket datagramSocket, DatagramPacket datagramPacket) throws IOException {
        // Decode request object
        CentralRepository requestCentralRepository = CentralRepository.parseFrom(trim(datagramPacket));

        // Build response object
        CentralRepository responseCentralRepository;

        // Perform action
        switch (CentralRepositoryAction.valueOf(requestCentralRepository.getAction())){
            case Lookup:
                responseCentralRepository = getServer(requestCentralRepository.getCampus(), requestCentralRepository.getType());
                break;
            case GetAvailablePort:
                responseCentralRepository = getAvailablePort();
                break;
            case Register:
            default:
                responseCentralRepository = addServer(requestCentralRepository);
                break;
        }

        // Encode response object
        byte[] response = responseCentralRepository.toByteArray();
        DatagramPacket reply = new DatagramPacket(response, response.length, datagramPacket.getAddress(), datagramPacket.getPort());
        datagramSocket.send(reply);
    }

    /**
     * Trims incoming packet to strip out 0s so that Protobuf can parse it
     * @param packet Datagram packet
     * @return Trimmed byte array
     */
    private static byte[] trim(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    /**
     * Processes add server action which adds item to the repository
     * @param requestCentralRepository Central Repository Request object
     * @return Central Repository Response object
     */
    private static CentralRepository addServer(CentralRepository requestCentralRepository){
        CentralRepository.Builder responseCentralRepository = CentralRepository.newBuilder();
        boolean status = false;
        for (Position<Entry<String, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> typePosition: repository.positions()){
            // Register server
            if (requestCentralRepository.getType().equals(typePosition.getElement().getKey())){
                // Check if campus exist, add if it doesn't
                boolean campusExist = false;
                for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> campusPosition: typePosition.getElement().getValue().positions()){
                    if (campusPosition.getElement().getKey().equals(requestCentralRepository.getCampus()))
                        campusExist = true;
                }
                if (!campusExist) {
                    LinkedPositionalList<Entry<String, String>> server = new LinkedPositionalList<>();
                    server.addFirst(new Node<>("port", String.valueOf(requestCentralRepository.getPort())));
                    server.addFirst(new Node<>("host", requestCentralRepository.getHost()));
                    server.addFirst(new Node<>("path", requestCentralRepository.getPath()));
                    typePosition.getElement().getValue().addFirst(new Node<>(requestCentralRepository.getCampus(), server));
                    usedPorts.add(requestCentralRepository.getPort());
                    status = true;
                }
            }
        }
        responseCentralRepository.setAction(requestCentralRepository.getAction());
        responseCentralRepository.setStatus(status);
        return responseCentralRepository.build();
    }

    /**
     * Processes lookup server action which checks returns server details for campus if it exists
     * @param campus Campus name (dvl, kkl, wst)
     * @param type Server type (rmi, udp)
     * @return Central repository response object
     */
    private static CentralRepository getServer(String campus, String type){
        CentralRepository.Builder responseCentralRepository = CentralRepository.newBuilder();
        boolean found = false;
        for (Position<Entry<String, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> typePosition: repository.positions()){
            if (type.equals(typePosition.getElement().getKey())){
                for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> campusPosition: typePosition.getElement().getValue().positions()){
                    if (campusPosition.getElement().getKey().equals(campus.toUpperCase())){
                        found = true;
                        for (Position<Entry<String, String>> serverInfo: campusPosition.getElement().getValue().positions()){
                            switch (serverInfo.getElement().getKey()){
                                case "port":
                                    responseCentralRepository.setPort(Integer.parseInt(serverInfo.getElement().getValue()));
                                    break;
                                case "host":
                                    responseCentralRepository.setHost(serverInfo.getElement().getValue());
                                    break;
                                case "path":
                                default:
                                    responseCentralRepository.setPath(serverInfo.getElement().getValue());
                                    break;
                            }
                        }
                    }
                }
            }
        }
        responseCentralRepository.setStatus(found);
        return responseCentralRepository.build();
    }

    /**
     * Processes get available port server action which dynamically allocates available ports
     * @return Random available port
     */
    private static CentralRepository getAvailablePort(){
        int randomPort = randomNumberGenerator();
        while (usedPorts.contains(randomPort) && !testPort(randomPort))
            randomPort = randomNumberGenerator();
        CentralRepository.Builder responseCentralRepository = CentralRepository.newBuilder();
        responseCentralRepository.setPort(randomPort);
        responseCentralRepository.setStatus(true);
        responseCentralRepository.setAction(CentralRepositoryAction.GetAvailablePort.toString());
        return responseCentralRepository.build();
    }

    /**
     * Test to see if port is taken by another process
     * @param port Port number
     * @return True if port is free, False otherwise
     */
    private static boolean testPort(int port){
        ServerSocket socket = null;
        DatagramSocket datagramSocket = null;
        try {
            socket = new ServerSocket(port);
            socket.setReuseAddress(true);
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setReuseAddress(true);
            return true;
        } catch (IOException e){
        } finally {
            if (datagramSocket != null)
                datagramSocket.close();
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    /**
     * Generates a random port between the MIN_PORT and MAX_PORT
     * @return Randomly generated port
     */
    private static int randomNumberGenerator(){
        return new Random().nextInt(MAX_PORT-MIN_PORT) + MIN_PORT;
    }
}
