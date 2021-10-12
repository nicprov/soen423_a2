package com.roomreservation;

import com.roomreservation.common.Logger;
import com.roomreservation.common.Parsing;
import com.roomreservation.common.RMIResponse;
import com.roomreservation.protobuf.protos.CentralRepository;
import com.roomreservation.protobuf.protos.CentralRepositoryAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.roomreservation.common.CentralRepositoryUtils.lookupServer;
import static com.roomreservation.common.ConsoleColours.*;

public class AdminClient {
    private static String registryURL;
    private static String logFilePath;

    public static void main(String[] args) {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader bufferedReader = new BufferedReader(is);
        try {
            String identifier = getIdentifier(bufferedReader);
            CentralRepository centralRepository = lookupServer(identifier.substring(0, 3), "rmi");
            if (centralRepository == null || !centralRepository.getStatus()){
                System.out.println("Unable to lookup server with central repository");
                System.exit(1);
            }
            registryURL = "rmi://" + centralRepository.getHost() + ":" + centralRepository.getPort() + "/" + centralRepository.getPath();
            logFilePath = "log/client/" + identifier + ".csv";
            Logger.initializeLog(logFilePath);
            System.out.println("Lookup completed");
            RoomReservationInterface roomReservation = (RoomReservationInterface) Naming.lookup(registryURL);
            startAdmin(roomReservation, bufferedReader);
        } catch (ConnectException e) {
            System.out.println(ANSI_RED + "Unable to connect to remote server, host may be down. Please try again later!" + RESET);
        } catch (Exception e){
            System.out.println(ANSI_RED + "Unable to start client: " + e.getMessage() + RESET);
        }
    }

    /**
     * Gets and validates unique identifier using regex. Identifier must contain the campus (dvl, kkl, wst)
     * followed by the user type (a for admin or s for student) followed by exactly four digits.
     * @param br BufferedReader for console output
     * @return Validated unique identifier
     * @throws IOException Exception
     */
    private static String getIdentifier(BufferedReader br) throws IOException {
        System.out.print("Enter unique identifier: ");
        String identifier = br.readLine().trim();
        Pattern pattern = Pattern.compile("(dvl|kkl|wst)(a)[0-9]{4}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(identifier);
        while (!matcher.find()){
            System.out.print(ANSI_RED + "Invalid identifier! Please enter your unique identifier: ");
            identifier = br.readLine().trim();
            matcher = pattern.matcher(identifier);
        }
        System.out.println(ANSI_GREEN + "Valid identifier" + RESET);
        return identifier;
    }

    /**
     * List possible actions based on identifierType (either student or admin) and prompts
     * user to select an action from his specific user role
     * @param bufferedReader Input buffer
     * @return Selected action
     * @throws IOException Exception
     */
    private static String listAndGetActions(BufferedReader bufferedReader) throws IOException {
        String action = "";
        System.out.println("\n==============================");
        System.out.println("Administration section");
        System.out.println("==============================");
        System.out.println("Select an action from the list below:");
        System.out.println("1. Create room");
        System.out.println("2. Delete room");
        System.out.println("3. Quit");
        System.out.print("Selection: ");
        action = bufferedReader.readLine().trim();
        while (!action.equals("1") && !action.equals("2") && !action.equals("3")){
            System.out.println(ANSI_RED + "Invalid selection! Must select a valid action (1, 2, 3): " + RESET);
            action = bufferedReader.readLine().trim();
        }
        return action;
    }

    /**
     * Start admin action processing
     * @param roomReservation RMI object
     * @param bufferedReader Input buffer
     * @throws IOException Exception
     */
    private static void startAdmin(RoomReservationInterface roomReservation, BufferedReader bufferedReader) throws IOException, NotBoundException, InterruptedException {
        while (true) {
            String action = listAndGetActions(bufferedReader);
            switch (action){
                case "1":
                    createRoom(roomReservation, bufferedReader);
                    break;
                case "2":
                    deleteRoom(roomReservation, bufferedReader);
                    break;
                case "3":
                default:
                    System.out.println("Goodbye!");
                    System.exit(0);
                    break;
            }
        }
    }

    /**
     * Calls remote createRoom method on server
     * @param roomReservation RMI object
     * @param bufferedReader Input buffer
     * @throws RemoteException Exception
     * @throws MalformedURLException Exception
     * @throws NotBoundException Exception
     * @throws InterruptedException Exception
     */
    private static void createRoom(RoomReservationInterface roomReservation, BufferedReader bufferedReader) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException {
        System.out.println("\nCREATE ROOM");
        System.out.println("-----------");
        try {
            RMIResponse response = roomReservation.createRoom(Parsing.getRoomNumber(bufferedReader),
                    Parsing.getDate(bufferedReader), Parsing.getTimeslots(bufferedReader));
            if (response != null) {
                if (response.getStatus())
                    System.out.println(ANSI_GREEN + response.getMessage() + RESET);
                else
                    System.out.println(ANSI_RED + response.getMessage() + RESET);
                Logger.log(logFilePath, response);
            } else {
                System.out.println(ANSI_RED + "Unable to connect to remote server" + RESET);
            }
        } catch (ConnectException e){
            System.out.println(ANSI_RED + "Unable to connect to remote server, retrying..." + RESET);
            Thread.sleep(1000);
            roomReservation = (RoomReservationInterface) Naming.lookup(registryURL);
            createRoom(roomReservation, bufferedReader);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "Exception: " + e.getMessage() + RESET);
        }
    }

    /**
     * Calls remote deleteRoom method on server
     * @param roomReservation RMI object
     * @param bufferedReader Input buffer
     * @throws RemoteException Exception
     * @throws InterruptedException Exception
     * @throws MalformedURLException Exception
     * @throws NotBoundException Exception
     */
    private static void deleteRoom(RoomReservationInterface roomReservation, BufferedReader bufferedReader) throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {
        System.out.println("\nDELETE ROOM");
        System.out.println("-----------");
        try {
            RMIResponse response = roomReservation.deleteRoom(Parsing.getRoomNumber(bufferedReader),
                    Parsing.getDate(bufferedReader), Parsing.getTimeslots(bufferedReader));
            if (response != null) {
                if (response.getStatus())
                    System.out.println(ANSI_GREEN + response.getMessage() + RESET);
                else
                    System.out.println(ANSI_RED + response.getMessage() + RESET);
                Logger.log(logFilePath, response);
            } else {
                System.out.println(ANSI_RED + "Unable to connect to remote server" + RESET);
            }
        } catch (ConnectException e){
            System.out.println(ANSI_RED + "Unable to connect to remote server, retrying..." + RESET);
            Thread.sleep(1000);
            roomReservation = (RoomReservationInterface) Naming.lookup(registryURL);
            deleteRoom(roomReservation, bufferedReader);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "Exception: " + e.getMessage() + RESET);
        }
    }
}
