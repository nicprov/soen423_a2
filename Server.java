import common.Campus;
import RoomReservationApp.RMIResponse;
import common.CentralRepositoryUtils;
import protobuf.protos.*;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static common.Campus.KKL;
import static common.ConsoleColours.*;

public class Server {

    private static RoomReservationImpl roomReservation;

    public static void main(String[] args) {
        try {
            if (args.length <= 1) {
                Campus campus = getCampus(args[0]);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startCorbaServer(campus);
                    }
                }).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startUDPServer(campus); // For internal communication between servers
                    }
                }).start();
            } else {
                System.err.println("Please only specify one parameter");
                System.exit(1);
            }
        }
        catch (Exception e){
            System.err.println("Usage: java Server [CAMPUS]");
            System.exit(1);
        }
    }

    private static void startCorbaServer(Campus campus){
        try {
            // Lookup server to see if it is already registered
            int remotePort = 0;
            CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus.toString(), "corba");
            if (centralRepository != null && centralRepository.getStatus()) {
                remotePort = centralRepository.getPort();
            } else {
                remotePort = getRemotePort(campus);
                if (!CentralRepositoryUtils.registerServer(campus.toString(), "corba", remotePort)){
                    System.out.println(ANSI_RED + "Unable to register server, central repository may be down" + RESET);
                    System.exit(1);
                }
            }
            Properties props = new Properties();
            props.put("org.omg.CORBA.ORBInitialHost", "localhost");
            props.put("org.omg.CORBA.ORBInitialPort", remotePort);
            String[] newArgs = new String[0];
            ORB orb = ORB.init(newArgs, props);
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();
            roomReservation = new RoomReservationImpl(campus);
            roomReservation.setORB(orb);
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(roomReservation);
            RoomReservationApp.RoomReservation href = RoomReservationApp.RoomReservationHelper.narrow(ref);
            org.omg.CORBA.Object objRef = orb.string_to_object("corbaloc::localhost:" + remotePort + "/NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            NameComponent path[] = ncRef.to_name("RoomReservation");
            ncRef.rebind(path, href);
            System.out.println("Corba Server ready (port: " + remotePort + ")");

            // wait for invocations from clients
            orb.run();
        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
            System.out.println("HelloServer Exiting ...");
    }

    /**
     * Starts UDP server to start accepting UDP requests
     * @param campus Campus name (dvl, wst, kkl)
     */
    private static void startUDPServer(Campus campus){
        DatagramSocket datagramSocket = null;
        try {
            // Lookup server to see if it is already registered
            int remotePort;
            CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus.toString(), "udp");
            if (centralRepository != null && centralRepository.getStatus()) {
                remotePort = centralRepository.getPort();
            } else {
                remotePort = CentralRepositoryUtils.getServerPort();
                if (remotePort == -1){
                    System.out.println(ANSI_RED + "Unable to get available port, central repository may be down" + RESET);
                    System.exit(1);
                }
                if (!CentralRepositoryUtils.registerServer(campus.toString(), "udp", remotePort)){
                    System.out.println(ANSI_RED + "Unable to register server, central repository may be down" + RESET);
                    System.exit(1);
                }
            }
            datagramSocket = new DatagramSocket(remotePort);
            System.out.println("UDP Server ready (port: " + remotePort + ")");
            byte[] buffer = new byte[1000];

            while (true){
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);

                // Launch a new thread for each request
                DatagramSocket finalDatagramSocket = datagramSocket;
                Thread thread = new Thread(() -> {
                    try {
                        handleUDPRequest(finalDatagramSocket, datagramPacket);
                    } catch (IOException | ParseException e) {
                        System.out.println(ANSI_RED + "Exception: " + e.getMessage() + RESET);
                    }
                });
                thread.start();
            }
        }
        catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
            System.exit(1);
        }
        catch (IOException e){
            System.out.println("IO Exception: " + e.getMessage());
            System.exit(1);
        }
        catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            System.exit(1);
        }
        finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
    }

    /**
     * Thread method to handle incoming UDP request
     * @param datagramSocket Datagram Socket
     * @param datagramPacket Datagram Packet
     * @throws IOException Exception
     * @throws ParseException Exception
     */
    private static void handleUDPRequest(DatagramSocket datagramSocket, DatagramPacket datagramPacket) throws IOException, ParseException {
        // Decode request object
        RequestObject requestObject = RequestObject.parseFrom(CentralRepositoryUtils.trim(datagramPacket));

        // Build response object
        ResponseObject responseObject;
        ResponseObject.Builder tempObject;

        // Perform action
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        switch (RequestObjectAction.valueOf(requestObject.getAction())){
            case GetAvailableTimeslots:
                responseObject = toResponseObject(roomReservation.getAvailableTimeSlotOnCampus(requestObject.getDate()));
                break;
            case BookRoom:
                responseObject = toResponseObject(roomReservation.bookRoom(requestObject.getIdentifier(), requestObject.getCampusName(), (short) requestObject.getRoomNumber(), requestObject.getDate(), requestObject.getTimeslot()));
                break;
            case CancelBooking:
                responseObject = toResponseObject(roomReservation.cancelBooking(requestObject.getIdentifier(), requestObject.getBookingId()));
                break;
            case GetBookingCount:
                responseObject = toResponseObject(roomReservation.getBookingCount(requestObject.getIdentifier(), dateFormat.parse(requestObject.getDate())));
                break;
            case CreateRoom:
                tempObject = ResponseObject.newBuilder();
                tempObject.setMessage("Create Room not supported through UDP");
                tempObject.setDateTime(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
                tempObject.setRequestType(RequestObjectAction.CreateRoom.toString());
                tempObject.setRequestParameters("None");
                tempObject.setStatus(false);
                responseObject = tempObject.build();
                break;
            case DeleteRoom:
            default:
                tempObject = ResponseObject.newBuilder();
                tempObject.setMessage("Delete Room not supported through UDP");
                tempObject.setDateTime(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
                tempObject.setRequestType(RequestObjectAction.DeleteRoom.toString());
                tempObject.setRequestParameters("None");
                tempObject.setStatus(false);
                responseObject = tempObject.build();
                break;
        }
        // Encode response object
        byte[] response = responseObject.toByteArray();
        DatagramPacket reply = new DatagramPacket(response, response.length, datagramPacket.getAddress(), datagramPacket.getPort());
        datagramSocket.send(reply);
    }

    /**
     * Parses campus name
     * @param campus Campus name (dvl, wst, kkl)
     * @return Campus enum
     */
    private static Campus getCampus(String campus) {
        Pattern pattern = Pattern.compile("(dvl|kkl|wst)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(campus);
        if (!matcher.find()) {
            System.out.print(ANSI_RED + "Invalid campus! Campus must be (DVL/KKL/WST)");
            System.exit(1);
        }
        switch (campus){
            case "dvl":
                return Campus.DVL;
            case "kkl":
                return KKL;
            case "wst":
            default:
                return Campus.WST;
        }
    }

    private static ResponseObject toResponseObject(RMIResponse rmiResponse){
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        responseObject.setStatus(rmiResponse.status);
        responseObject.setDateTime(rmiResponse.date);
        responseObject.setMessage(rmiResponse.message);
        responseObject.setRequestParameters(rmiResponse.requestParameters);
        responseObject.setRequestType(rmiResponse.requestType);
        return responseObject.build();
    }

    private static int getRemotePort(Campus campus){
        switch (campus){
            case DVL:
                return 1050;
            case KKL:
                return 1052;
            case WST:
            default:
                return 1054;
        }
    }
}
