import RoomReservationApp.RMIResponse;
import common.Corba;
import common.Logger;
import common.Parsing;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static common.ConsoleColours.*;
import static common.ConsoleColours.ANSI_RED;


public class AdminClient {
    private static String logFilePath;
    private static String identifier;
    private static RoomReservationApp.RoomReservation roomReservation;

    public static void main(String[] args) {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader bufferedReader = new BufferedReader(is);
        try {
            identifier = getIdentifier(bufferedReader);
            roomReservation = Corba.connectCorba(identifier.substring(0, 3));
            System.out.println("Obtained a handle on server object");
            logFilePath = "log/client/" + identifier + ".csv";
            Logger.initializeLog(logFilePath);
            startAdmin(bufferedReader);
        } catch (Exception e) {
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
        String action;
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
     * @param bufferedReader Input buffer
     * @throws IOException Exception
     */
    private static void startAdmin(BufferedReader bufferedReader) throws IOException, InterruptedException {
        while (true) {
            String action = listAndGetActions(bufferedReader);
            try {
                RMIResponse response = null;
                switch (action){
                    case "1":
                        System.out.println("\nCREATE ROOM");
                        System.out.println("-----------");
                        response = roomReservation.createRoom(Parsing.getRoomNumber(bufferedReader),
                                Parsing.getDate(bufferedReader), Parsing.getTimeslots(bufferedReader));
                        break;
                    case "2":
                        System.out.println("\nDELETE ROOM");
                        System.out.println("-----------");
                        response = roomReservation.createRoom(Parsing.getRoomNumber(bufferedReader),
                                Parsing.getDate(bufferedReader), Parsing.getTimeslots(bufferedReader));
                        break;
                    case "3":
                    default:
                        System.out.println("Goodbye!");
                        System.exit(0);
                        break;
                }
                if (response != null) {
                    if (response.status)
                        System.out.println(ANSI_GREEN + response.message + RESET);
                    else
                        System.out.println(ANSI_RED + response.message + RESET);
                    Logger.log(logFilePath, response);
                } else {
                    System.out.println(ANSI_RED + "Unable to connect to remote server" + RESET);
                }
            } catch (org.omg.CORBA.TRANSIENT exception){
                System.out.println(ANSI_RED + "Unable to connect to remote server, retrying..." + RESET);
                Thread.sleep(1000);
                roomReservation = Corba.connectCorba(identifier.substring(0, 3));
                startAdmin(bufferedReader);
            } catch (IOException e) {
                System.out.println(ANSI_RED + "Exception: " + e.getMessage() + RESET);
            }
        }
    }
}

