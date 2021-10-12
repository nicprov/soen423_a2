package com.roomreservation.common;

import com.roomreservation.protobuf.protos.CentralRepository;
import com.roomreservation.protobuf.protos.CentralRepositoryAction;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static com.roomreservation.common.ConsoleColours.ANSI_RED;
import static com.roomreservation.common.ConsoleColours.RESET;

public class CentralRepositoryUtils {
    public static final String SERVER_HOST = "localhost";
    public static final String SERVER_PATH = "server";

    /**
     * Trims byte array to remove any 0 entries (or empty entries) so that the Protobuf can parse it properly
     * @param packet Datagram Packet from the UDP server
     * @return trimmed byte array
     */
    public static byte[] trim(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    /**
     * Performs a lookup request on the Central Repository
     * @param campus Campus name (dvl, kkl, wst)
     * @param type Server type (udp, rmi)
     * @return central repository object
     */
    public static CentralRepository lookupServer(String campus, String type){
        CentralRepository.Builder centralRepositoryRequest = CentralRepository.newBuilder();
        centralRepositoryRequest.setAction(CentralRepositoryAction.Lookup.toString());
        centralRepositoryRequest.setType(type);
        centralRepositoryRequest.setCampus(campus);
        return udpTransfer(centralRepositoryRequest.build());
    }

    /**
     * Performs a udp request to the central repository
     * @param centralRepositoryRequest Central repository request object
     * @return Central repository response object
     */
    public static CentralRepository udpTransfer(CentralRepository centralRepositoryRequest){
        DatagramSocket datagramSocket = null;
        try {
            int remotePort = 1024;
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(1000); // Set timeout
            InetAddress host = InetAddress.getLocalHost();
            DatagramPacket request = new DatagramPacket(centralRepositoryRequest.toByteArray(), centralRepositoryRequest.toByteArray().length, host, remotePort);
            datagramSocket.send(request);
            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(reply);
            return CentralRepository.parseFrom(trim(reply));
        }
        catch (SocketException e){
            System.out.println(ANSI_RED + "Socket: " + e.getMessage() + RESET);
        } catch (IOException e){
            System.out.println(ANSI_RED + "IO: " + e.getMessage() + RESET);
        } finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
        return null;
    }

    /**
     * Performs udp request on the central repository
     * @return available server port
     */
    public static int getServerPort(){
        CentralRepository.Builder centralRepositoryRequest = CentralRepository.newBuilder();
        centralRepositoryRequest.setAction(CentralRepositoryAction.GetAvailablePort.toString());
        CentralRepository centralRepositoryResponse = udpTransfer(centralRepositoryRequest.build());
        if (centralRepositoryResponse == null)
            return -1;
        if (!centralRepositoryResponse.getStatus())
            return -1;
        return centralRepositoryResponse.getPort();
    }

    /**
     * Registers server with the central repository
     * @param campus Campus name (dvl, kkl, wst)
     * @param type Server type (udp, rmi)
     * @param port Network port
     * @return True if server was successfully registered, false otherwise
     */
    public static boolean registerServer(String campus, String type, int port){
        CentralRepository.Builder centralRepositoryRequest = CentralRepository.newBuilder();
        centralRepositoryRequest.setAction(CentralRepositoryAction.Register.toString());
        centralRepositoryRequest.setPort(port);
        centralRepositoryRequest.setPath(SERVER_PATH);
        centralRepositoryRequest.setHost(SERVER_HOST);
        centralRepositoryRequest.setType(type);
        centralRepositoryRequest.setCampus(campus);
        CentralRepository centralRepositoryResponse = udpTransfer(centralRepositoryRequest.build());
        if (centralRepositoryResponse != null)
            return centralRepositoryResponse.getStatus();
        return false;
    }
}
