package com.roomreservation;

import com.roomreservation.common.Campus;
import com.roomreservation.common.RMIResponse;

import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Date;

public interface RoomReservationInterface extends Remote {
    /* Admin role */
    RMIResponse createRoom(int roomNumber, Date date, ArrayList<String> listOfTimeSlots) throws IOException;
    RMIResponse deleteRoom(int roomNumber, Date date, ArrayList<String> listOfTimeSlots) throws IOException;

    /* Student role */
    RMIResponse bookRoom(String identifier, Campus campusName, int roomNumber, Date date, String timeslot) throws IOException;
    RMIResponse getAvailableTimeSlot(Date date) throws IOException;
    RMIResponse cancelBooking(String identifier, String bookingId) throws IOException;
}