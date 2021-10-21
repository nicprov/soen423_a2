package common;

import RoomReservationApp.CorbaResponse;

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
     * @param corbaResponse CorbaResponse object
     * @throws IOException Exception
     */
    public static void log(String logFilePath, CorbaResponse corbaResponse) throws IOException {
        FileWriter fileWriter = new FileWriter(logFilePath, true);
        fileWriter.append(toString(corbaResponse)).append("\n");
        fileWriter.close();
    }

    /**
     * Converts Corba RMIResponse object to an appropriate string for the logger
     * @param corbaResponse CorbaResponse object
     * @return
     */
    private static String toString(CorbaResponse corbaResponse){
        return corbaResponse.date + "," + corbaResponse.message + "," + corbaResponse.requestType + "," + corbaResponse.requestParameters + "," + corbaResponse.status;
    }
}
