import RoomReservationApp.CorbaResponse;
import common.Corba;

public class TestConcurrency {
    private static final int NUM_BOOKINGS = 3;

    public static void main(String[] args) {
        // Setup
        String[] bookingIds = setup();
        for (String bookingId: bookingIds){
            System.out.println("SETUP: Booking (" + bookingId + ") created");
        }

        // Get prepared threads
        Thread[] threadGroup1 = changeReservation(bookingIds);
        Thread[] threadGroup2 = bookRoom();

        // Start all threads at once
        for (Thread thread: threadGroup1)
            thread.start();
        for (Thread thread: threadGroup2)
          thread.start();
    }

    private static String[] setup(){
        RoomReservationApp.RoomReservation roomReservation = Corba.connectCorba("dvl");
        String[] bookingIds = new String[NUM_BOOKINGS];
        for (int roomNum=201; roomNum<201+(NUM_BOOKINGS); roomNum++){
            CorbaResponse corbaResponse = roomReservation.bookRoom("dvls1234", "dvl", (short) roomNum, "2021-01-01", "9:30-10:00");
            bookingIds[roomNum-201] = corbaResponse.message.split(":")[3].trim() + ":" + corbaResponse.message.split(":")[4];
        }
        return bookingIds;
    }

    private static Thread[] changeReservation(String[] bookingIds){
        RoomReservationApp.RoomReservation roomReservation = Corba.connectCorba("dvl");
        int roomNumber = 201;
        int counter = 0;
        Thread[] threads = new Thread[NUM_BOOKINGS];
        for (String bookingId: bookingIds){
            int finalRoomNumber = roomNumber;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CorbaResponse corbaResponse = roomReservation.changeReservation("dvls1234", bookingId, "dvl", (short) finalRoomNumber, "2021-01-02", "9:30-10:00");
                    System.out.println("Student (dvls1234): changeReservation (" + corbaResponse.status + ") in room (" + finalRoomNumber + ")");
                }
            });
            threads[counter++] = thread;
            roomNumber++;
        }
        return threads;
    }

    private static Thread[] bookRoom(){
        RoomReservationApp.RoomReservation roomReservation = Corba.connectCorba("dvl");
        int counter = 0;
        Thread[] threads = new Thread[NUM_BOOKINGS];
        for (int roomNumber = 201; roomNumber<(201+NUM_BOOKINGS); roomNumber++){
            int finalRoomNumber = roomNumber;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CorbaResponse corbaResponse = roomReservation.bookRoom("dvls1235", "dvl", (short) finalRoomNumber, "2021-01-02", "9:30-10:00");
                    System.out.println("Student (dvls1235): bookRoom ("  + corbaResponse.status + ") in room (" + finalRoomNumber + ")");
                }
            });
            threads[counter++] = thread;
        }
        return threads;
    }
}
