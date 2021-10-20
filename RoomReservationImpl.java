import common.Campus;
import RoomReservationApp.RMIResponse;
import collection.Entry;
import collection.LinkedPositionalList;
import collection.Node;
import collection.Position;
import common.CentralRepositoryUtils;
import common.Logger;
import common.Parsing;
import org.omg.CORBA.ORB;
import protobuf.protos.CentralRepository;
import protobuf.protos.RequestObject;
import protobuf.protos.RequestObjectAction;
import protobuf.protos.ResponseObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static common.ConsoleColours.ANSI_RED;
import static common.ConsoleColours.RESET;

public class RoomReservationImpl extends RoomReservationApp.RoomReservationPOA {

    private static volatile LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> database;
    private static volatile LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingCount;
    private final String logFilePath;
    private final Campus campus;
    private ORB orb;
    private final ReentrantLock lock = new ReentrantLock();

    protected RoomReservationImpl(Campus campus){
        database = new LinkedPositionalList<>();
        bookingCount = new LinkedPositionalList<>();
        this.campus = campus;
        logFilePath = "log/server/" + this.campus.toString() + ".csv";
        try {
            Logger.initializeLog(logFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.generateSampleData();
    }

    public void setORB(ORB orb) {
        this.orb = orb;
    }

    @Override
    public RMIResponse createRoom(short roomNumber, String date, String[] listOfTimeSlots) {
        Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition = findDate(date);
        boolean timeSlotCreated = false;
        boolean roomExist = false;
        if (datePosition == null){
            // Date not found so create date entry
            LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslots = new LinkedPositionalList<>();
            for (String timeslot: listOfTimeSlots){
                timeslots.addFirst(new Node<>(timeslot, null));
            }
            lock.lock();
            try {
                database.addFirst(new Node<>(date, new LinkedPositionalList<>(new Node<>(roomNumber, timeslots))));
            } finally {
                lock.unlock();
            }
        } else {
            // Date exist, check if room exist
            Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition = findRoom(roomNumber, datePosition);
            if (roomPosition == null) {
                // Room not found so create room
                LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslots = new LinkedPositionalList<>();
                for (String timeslot: listOfTimeSlots){
                    timeslots.addFirst(new Node<>(timeslot, null));
                }
                lock.lock();
                try {
                    datePosition.getElement().getValue().addFirst(new Node<>(roomNumber, timeslots));
                } finally {
                    lock.unlock();
                }
            } else {
                // Room exist, so check if timeslot exist
                roomExist = true;
                for (String timeslot: listOfTimeSlots){
                    if (findTimeslot(timeslot, roomPosition) == null) {
                        lock.lock();
                        try {
                            // Timeslot does not exist, so create it, skip otherwise
                            roomPosition.getElement().getValue().addFirst(new Node<>(timeslot, null));
                            timeSlotCreated = true;
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        }
        RMIResponse rmiResponse = new RMIResponse();
        if (!roomExist) {
            rmiResponse.message = "Created room (" + roomNumber + ")";
            rmiResponse.status = true;
        } else if (!timeSlotCreated){
            rmiResponse.message = "Room already exist with specified timeslots";
            rmiResponse.status = false;
        } else {
            rmiResponse.message = "Added timeslots to room (" + roomNumber + ")";
            rmiResponse.status = true;
        }
        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.CreateRoom.toString();
        rmiResponse.requestParameters = "Room number: " + roomNumber + " | Date: " + date + " | List of Timeslots: " + arrayToString(listOfTimeSlots);
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    @Override
    public RMIResponse deleteRoom(short roomNumber, String date, String[] listOfTimeSlots) {
        Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition = findDate(date);
        boolean timeslotExist = false;
        if (datePosition != null){
            // Date exist, check if room exist
            Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition = findRoom(roomNumber, datePosition);
            if (roomPosition != null) {
                // Room found, search for timeslots
                for (String timeslot: listOfTimeSlots){
                    Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotPosition = findTimeslot(timeslot, roomPosition);
                    if (timeslotPosition != null) {
                        if (timeslotPosition.getElement().getValue() != null) {
                            for (Position<Entry<String, String>> timeslotPropertiesNext : timeslotPosition.getElement().getValue().positions()) {
                                if (timeslotPropertiesNext.getElement().getValue().equals("studentId")) {
                                    // Reduce booking count for student
                                    decreaseBookingCounter(timeslotPropertiesNext.getElement().getValue(), datePosition.getElement().getKey());
                                }
                            }
                        }
                        lock.lock();
                        try {
                            // Timeslot exists, so delete it
                            roomPosition.getElement().getValue().remove(timeslotPosition);
                            timeslotExist = true;
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        }
        RMIResponse rmiResponse = new RMIResponse();
        if (!timeslotExist){
            rmiResponse.message = "No timeslots to delete on (" + date + ")";
            rmiResponse.status = false;
        } else {
            rmiResponse.message = "Removed timeslots from room (" + roomNumber + ")";
            rmiResponse.status = true;
        }
        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.CreateRoom.toString();
        rmiResponse.requestParameters = "Room number: " + roomNumber + " | Date: " + date + " | List of Timeslots: " + arrayToString(listOfTimeSlots);
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    @Override
    public RMIResponse bookRoom(String identifier, String campusName, short roomNumber, String date, String timeslot) {
        if (campus.equals(this.campus))
            return bookRoomOnCampus(identifier, roomNumber, date, timeslot);
        else {
            // Perform action on remote server
            RequestObject.Builder requestObject = RequestObject.newBuilder();
            requestObject.setAction(RequestObjectAction.BookRoom.toString());
            requestObject.setIdentifier(identifier);
            requestObject.setRoomNumber(roomNumber);
            requestObject.setCampusName(campus.toString());
            requestObject.setDate(date);
            requestObject.setTimeslot(timeslot);
            return udpTransfer(campus, requestObject.build());
        }
    }

    @Override
    public RMIResponse getAvailableTimeSlot(String date) {
        System.out.println("Received request for CAMPUS: " + this.campus);
        // Build new proto request object
        RequestObject.Builder requestObject = RequestObject.newBuilder();
        requestObject.setAction(RequestObjectAction.GetAvailableTimeslots.toString());
        requestObject.setDate(date);

        // Get response object from each campus
        RMIResponse dvlTimeslots = udpTransfer(Campus.DVL, requestObject.build());
        RMIResponse kklTimeslots = udpTransfer(Campus.KKL, requestObject.build());
        RMIResponse wstTimeslots = udpTransfer(Campus.WST, requestObject.build());
        String message = "";
        if (dvlTimeslots.status)
            message += "DVL " + dvlTimeslots.message + " ";
        else
            message += "DVL (no response from server) ";
        if (kklTimeslots.status)
            message += "KKL " + kklTimeslots.message + " ";
        else
            message += "KKL (no response from server) ";
        if (wstTimeslots.status)
            message += "WST " + wstTimeslots.message;
        else
            message += "WST (no response from server)";

        //  Create response object for rmi
        RMIResponse rmiResponse = new RMIResponse();
        rmiResponse.message = message;
        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.GetAvailableTimeslots.toString();
        rmiResponse.requestParameters = "Date: " + date;
        rmiResponse.status = true;
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    @Override
    public RMIResponse cancelBooking(String identifier, String bookingId) {
        Campus campus = Campus.valueOf(bookingId.split(":")[0]);
        if (campus.equals(this.campus))
            return cancelBookingOnCampus(identifier, bookingId);
        else {
            // Perform action on remote server
            RequestObject.Builder requestObject = RequestObject.newBuilder();
            requestObject.setAction(RequestObjectAction.CancelBooking.toString());
            requestObject.setIdentifier(identifier);
            requestObject.setBookingId(bookingId);
            return udpTransfer(campus, requestObject.build());
        }
    }

    @Override
    public RMIResponse changeReservation(String identifier, String bookingId, String newCampusName, short newRoomNumber, String newDate, String newTimeslot) {
        // Cancel existing booking
        RMIResponse cancelBooking = cancelBooking(identifier, bookingId);
        String requestParameters = "Booking ID: " + bookingId + " | Campus Name: " + newCampusName + " | Room number: " + newRoomNumber + " | New date: " + newDate + " | Timeslot: " + newTimeslot;
        if (cancelBooking.status){
            // Create new booking
            RMIResponse createBooking = bookRoom(identifier, newCampusName, newRoomNumber, newDate, newTimeslot);
            if (createBooking.status){
                RMIResponse response = new RMIResponse();
                response.requestType = RequestObjectAction.ChangeReservation.toString();
                response.requestParameters = requestParameters;
                response.status = true;
                response.message = createBooking.message;
                response.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
                return response;
            } else {
                createBooking.requestType = RequestObjectAction.ChangeReservation.toString();
                createBooking.requestParameters = requestParameters;
                return createBooking;
            }
        } else {
            cancelBooking.requestType = RequestObjectAction.ChangeReservation.toString();
            cancelBooking.requestParameters = requestParameters;
            return cancelBooking;
        }
    }

    @Override
    public void shutdown() {
        orb.shutdown(true);
    }

    private Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> findDate(String date){
        for (Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> dateNext : database.positions()) {
            if (dateNext.getElement().getKey().equals(date))
                return dateNext;
        }
        return null;
    }

    /**
     * Counts the number of available timeslots on a given day in the given campus
     * @param date Date
     * @return RMI response object
     */
    public RMIResponse getAvailableTimeSlotOnCampus(String date) {
        int counter = 0;
        for (Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> dateNext : database.positions()) {
            for (Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomNext : dateNext.getElement().getValue().positions()) {
                for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotNext : roomNext.getElement().getValue().positions()) {
                    if (timeslotNext.getElement().getValue() == null && dateNext.getElement().getKey().equals(date))
                        counter++;
                }
            }
        }
        RMIResponse rmiResponse = new RMIResponse();
        rmiResponse.message = Integer.toString(counter);
        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.GetAvailableTimeslots.toString();
        rmiResponse.requestParameters = "Date: " + date;
        rmiResponse.status = true;
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    /**
     * Counts the number of bookings on a specific date for a specific user
     * @param identifier User ID (ie. dvls1234)
     * @param date Date
     * @return RMI response object
     */
    public RMIResponse getBookingCount(String identifier, Date date) {
        int counter = 0;
        LocalDate tempDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        for (Position<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingIdentifier: bookingCount.positions()){
            if (bookingIdentifier.getElement().getKey().equals(identifier)) {
                for (Position<Entry<Date, Integer>> bookingDate: bookingIdentifier.getElement().getValue().positions()){
                    // Counter date is >= than provided date (-1 week) and Counter date is < provided date
                    if ((bookingDate.getElement().getKey().compareTo(Date.from(tempDate.minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant())) > 0)
                            && (bookingDate.getElement().getKey().compareTo(Date.from(tempDate.atStartOfDay(ZoneId.systemDefault()).toInstant())) <= 0)){
                        // Within 1 week so it counts
                        counter += bookingDate.getElement().getValue();
                    }
                }
            }
        }
        RMIResponse rmiResponse = new RMIResponse();
        rmiResponse.status = true;
        rmiResponse.message = Integer.toString(counter);
        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.CreateRoom.toString();
        rmiResponse.requestParameters = "Identifier: " + identifier + " | Date: " + date;
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    /**
     * Books room for a specific user in a specific room, on a specific day and timeslot
     * @param identifier User ID (ie. dvls1234)
     * @param roomNumber Room number
     * @param date Date
     * @param timeslot Timeslot
     * @return RMI response object
     */
    private RMIResponse bookRoomOnCampus(String identifier, short roomNumber, String date, String timeslot) {
        boolean isOverBookingCountLimit = false;
        boolean timeslotExist = false;
        boolean isBooked = false;
        String bookingId = "";
        Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition = findDate(date);
        if (datePosition != null) {
            // Date exist, check if room exist
            Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition = findRoom(roomNumber, datePosition);
            if (roomPosition != null) {
                // Room found, search for timeslots
                Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotPosition = findTimeslot(timeslot, roomPosition);

                // Check if timeslot exist
                if (timeslotPosition != null) {
                    timeslotExist = true;

                    // Check booking count for this week on all campuses
                    RequestObject.Builder requestBookingCount = RequestObject.newBuilder();
                    requestBookingCount.setIdentifier(identifier);
                    requestBookingCount.setDate(date);
                    requestBookingCount.setAction(RequestObjectAction.GetBookingCount.toString());
                    RMIResponse dvlBookingCount = udpTransfer(Campus.DVL, requestBookingCount.build());
                    RMIResponse kklBookingCount = udpTransfer(Campus.KKL, requestBookingCount.build());
                    RMIResponse wstBookingCount = udpTransfer(Campus.WST, requestBookingCount.build());

                    int totalBookingCount = 0;
                    if (dvlBookingCount.status)
                        totalBookingCount += Integer.parseInt(dvlBookingCount.message);
                    if (kklBookingCount.status)
                        totalBookingCount += Integer.parseInt(kklBookingCount.message);
                    if (wstBookingCount.status)
                        totalBookingCount += Integer.parseInt(wstBookingCount.message);

                    // Increase if total booking count < 3, increase
                    if (totalBookingCount < 3) {
                        //Increase booking count
                        increaseBookingCounter(identifier, date);

                        lock.lock();
                        try {
                            if (timeslotPosition.getElement().getValue() == null){
                                // Create timeslot and add attributes
                                isBooked = true;
                                bookingId = this.campus + ":" + UUID.randomUUID();
                                roomPosition.getElement().getValue().set(timeslotPosition, new Node<>(timeslot, new LinkedPositionalList<>()));
                                timeslotPosition.getElement().getValue().addFirst(new Node<>("bookingId", bookingId));
                                timeslotPosition.getElement().getValue().addFirst(new Node<>("studentId", identifier));
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else
                        isOverBookingCountLimit = true;
                }
            }
        }
        RMIResponse rmiResponse = new RMIResponse();
        if (!timeslotExist){
            rmiResponse.message = "Timeslot (" + timeslot + ") does not exist on (" + date + ")";
            rmiResponse.status = false;
        } else if (isOverBookingCountLimit) {
            rmiResponse.message = "Unable to book room, maximum booking limit is reached";
            rmiResponse.status = false;
        } else if (isBooked){
            rmiResponse.message = "Timeslot (" + timeslot + ") on (" + date + ") has been booked | Booking ID: " + bookingId;
            rmiResponse.status = true;
        } else {
            rmiResponse.message = "Unable to book room, timeslot (" + timeslot + ") has already booked";
            rmiResponse.status = false;
        }
        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.CreateRoom.toString();
        rmiResponse.requestParameters = "Identifier: " + identifier + " | Room Number: " + roomNumber + " | Date: " + date + " | Timeslot: " + timeslot;
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    /**
     * Cancels booking on campus for a specific user and booking id
     * @param identifier User
     * @param bookingId Booking id
     * @return RMI response object
     */
    private RMIResponse cancelBookingOnCampus(String identifier, String bookingId) {
        boolean bookingExist = false;
        boolean studentIdMatched = false;
        for (Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition : database.positions()) {
            for (Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomPosition : datePosition.getElement().getValue().positions()) {
                for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotPosition : roomPosition.getElement().getValue().positions()) {
                    if (timeslotPosition.getElement().getValue() != null){
                        for (Position<Entry<String, String>> timeslotPropertiesPosition : timeslotPosition.getElement().getValue().positions()) {
                            if (timeslotPropertiesPosition.getElement().getKey().equals("studentId") && timeslotPropertiesPosition.getElement().getValue().equals(identifier)){
                                studentIdMatched = true;
                            }
                            if (timeslotPropertiesPosition.getElement().getKey().equals("bookingId") && timeslotPropertiesPosition.getElement().getValue().equals(bookingId)){
                                bookingExist = true;
                            }
                        }
                        if (bookingExist && studentIdMatched){
                            // Reduce booking count
                            decreaseBookingCounter(identifier, datePosition.getElement().getKey());

                            lock.lock();
                            try {
                                // Cancel booking
                                roomPosition.getElement().getValue().set(timeslotPosition, new Node<>(timeslotPosition.getElement().getKey(), null));
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                }
            }
        }
        RMIResponse rmiResponse = new RMIResponse();
        if (!bookingExist){
            rmiResponse.message = "Booking (" + bookingId + ") does not exist";
            rmiResponse.status = false;
        } else if (!studentIdMatched) {
            rmiResponse.message = "Booking (" + bookingId + ") is reserved to another student";
            rmiResponse.status = false;
        } else {
            rmiResponse.message = "Cancelled booking (" + bookingId + ")";
            rmiResponse.status = true;
        }

        rmiResponse.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        rmiResponse.requestType = RequestObjectAction.CreateRoom.toString();
        rmiResponse.requestParameters = "Booking Id: " + bookingId;
        try {
            Logger.log(logFilePath, rmiResponse);
        } catch (IOException ignored) {}
        return rmiResponse;
    }

    /**
     * Increase booking count for specific user on specific date
     * @param identifier User ID (ie. dvls1234)
     * @param date Date
     */
    public void increaseBookingCounter(String identifier, String date) {
        try {
            Date tempDate = new SimpleDateFormat("dd-MM-yyyy").parse(date);
            boolean foundIdentifier = false;
            boolean foundDate = false;
            for (Position<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingIdentifier: bookingCount.positions()){
                if (bookingIdentifier.getElement().getKey().equals(identifier)) {
                    foundIdentifier = true;
                    for (Position<Entry<Date, Integer>> bookingDate: bookingIdentifier.getElement().getValue().positions()){
                        if (bookingDate.getElement().getKey().equals(tempDate)){
                            lock.lock();
                            try {
                                foundDate = true;
                                // Increase count
                                bookingIdentifier.getElement().getValue().set(bookingDate, new Node<>(tempDate, bookingDate.getElement().getValue() + 1));
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                    if (!foundDate){
                        lock.lock();
                        try {
                            bookingIdentifier.getElement().getValue().addFirst(new Node<>(tempDate, 1));
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
            if (!foundIdentifier) {
                lock.lock();
                try {
                    bookingCount.addFirst(new Node<>(identifier, new LinkedPositionalList<>(new Node<>(tempDate, 1))));
                } finally {
                    lock.unlock();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Decreases booking count for specific user on specific date
     * @param identifier User ID (ie. dvls1234)
     * @param date Date
     */
    public void decreaseBookingCounter(String identifier, String date) {
        try {
            Date tempDate = new SimpleDateFormat("dd-MM-yyyy").parse(date);
            for (Position<Entry<String, LinkedPositionalList<Entry<Date, Integer>>>> bookingIdentifier: bookingCount.positions()){
                if (bookingIdentifier.getElement().getKey().equals(identifier)) {
                    for (Position<Entry<Date, Integer>> bookingDate: bookingIdentifier.getElement().getValue().positions()){
                        if (bookingDate.getElement().getKey().equals(tempDate)){
                            lock.lock();
                            try {
                                // Decrease count
                                bookingIdentifier.getElement().getValue().set(bookingDate, new Node<>(tempDate, bookingDate.getElement().getValue() - 1));
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs a UDP request on a specific campus by first performing a looking with the central repository
     * @param campus Campus name (dvl, wst, kkl)
     * @param requestObject Request Object
     * @return RMI response object
     */
    private RMIResponse udpTransfer(Campus campus, RequestObject requestObject){
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(1000); // Set timeout
            InetAddress host = InetAddress.getLocalHost();

            CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus.toString(), "udp");
            if (centralRepository != null && centralRepository.getStatus()){
                DatagramPacket request = new DatagramPacket(requestObject.toByteArray(), requestObject.toByteArray().length, host, centralRepository.getPort());
                datagramSocket.send(request);
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(reply);
                return fromResponseObject(ResponseObject.parseFrom(trim(reply)));
            } else {
                System.out.println(ANSI_RED + "Unable to get server details from the central repository" + RESET);
                RMIResponse rmiResponse = new RMIResponse();
                rmiResponse.status = false;
                rmiResponse.message = "Unable to get server details from the central repository";
                return rmiResponse;
            }
        }
        catch (SocketException e){
            System.out.println(ANSI_RED + "Socket: " + e.getMessage() + RESET);
        } catch (IOException e){
            System.out.println(ANSI_RED + "IO: " + e.getMessage() + RESET);
        } finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
        RMIResponse rmiResponse = new RMIResponse();
        rmiResponse.status = false;
        rmiResponse.message = "Unable to connect to remote server";
        return rmiResponse;
    }

    /**
     * Trims byte array to strip 0s filling up unused elements
     * @param packet Datagram packet
     * @return Trimmed byte array
     */
    private static byte[] trim(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    /**
     * Searches database to find position at specific room number
     * @param roomNumber Campus room number
     * @param datePosition Date position object
     * @return Room position in database
     */
    private Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> findRoom(short roomNumber, Position<Entry<String, LinkedPositionalList<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>>>> datePosition){
        for (Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> roomNext : datePosition.getElement().getValue().positions()) {
            if (roomNext.getElement().getKey().equals(roomNumber))
                return roomNext;
        }
        return null;
    }

    /**
     * Searches database to find position at specific timeslot
     * @param timeslot Timeslot
     * @param room Room position object
     * @return Timeslot position in database
     */
    private Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> findTimeslot(String timeslot, Position<Entry<Short, LinkedPositionalList<Entry<String, LinkedPositionalList<Entry<String, String>>>>>> room){
        for (Position<Entry<String, LinkedPositionalList<Entry<String, String>>>> timeslotNext : room.getElement().getValue().positions()) {
            if (timeslotNext.getElement().getKey().equals(timeslot))
                return timeslotNext;
        }
        return null;
    }

    private RMIResponse fromResponseObject(ResponseObject responseObject){
        RMIResponse rmiResponse = new RMIResponse();
        rmiResponse.message = responseObject.getMessage();
        rmiResponse.status = responseObject.getStatus();
        rmiResponse.requestParameters = responseObject.getRequestParameters();
        rmiResponse.requestType = responseObject.getRequestType();
        rmiResponse.date = responseObject.getDateTime();
        return rmiResponse;
    }

    private String arrayToString(String[] array){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        int counter = 1;
        for (String item: array) {
            stringBuilder.append(item);
            if (counter++ < array.length)
                stringBuilder.append("_");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    /**
     * Generates sample data in campus
     */
    private void generateSampleData(){
        this.createRoom((short) 201, Parsing.tryParseDate("2021-01-01"), Parsing.tryParseTimeslotList("9:30-10:00"));
        this.createRoom((short) 201, Parsing.tryParseDate("2021-01-02"), Parsing.tryParseTimeslotList("9:30-10:00"));
        this.createRoom((short) 202, Parsing.tryParseDate("2021-01-01"), Parsing.tryParseTimeslotList("9:30-10:00"));
        this.createRoom((short) 202, Parsing.tryParseDate("2021-01-02"), Parsing.tryParseTimeslotList("9:30-10:00"));
        this.createRoom((short) 203, Parsing.tryParseDate("2021-01-01"), Parsing.tryParseTimeslotList("9:30-10:00"));
        this.createRoom((short) 203, Parsing.tryParseDate("2021-01-02"), Parsing.tryParseTimeslotList("9:30-10:00"));
    }
}
