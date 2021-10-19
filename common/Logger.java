package common;

import RoomReservationApp.RMIResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    /**
     * Initializes logger file
     * @param logFilePath Logger file path
     * @throws IOException Exception
     */
    public static void initializeLog(String logFilePath) throws IOException {
        if (new File(logFilePath).createNewFile()) {
            FileWriter fileWriter = new FileWriter(logFilePath, false);
            fileWriter.append("Datetime,Message,RequestType,RequestParameters,Status").append("\n");
            fileWriter.close();
        }
    }

    /**
     * Adds entry in log file
     * @param logFilePath Logger file path
     * @param rmiResponse RMI Response object
     * @throws IOException Exception
     */
    public static void log(String logFilePath, RMIResponse rmiResponse) throws IOException {
        FileWriter fileWriter = new FileWriter(logFilePath, true);
        fileWriter.append(toString(rmiResponse)).append("\n");
        fileWriter.close();
    }

    private static String toString(RMIResponse rmiResponse){
        return rmiResponse.date + "," + rmiResponse.message + "," + rmiResponse.requestType + "," + rmiResponse.requestParameters + "," + rmiResponse.status;
    }
}
