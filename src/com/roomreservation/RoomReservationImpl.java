package com.roomreservation;

import RoomReservationApp.Campus;
import RoomReservationApp.RMIResponse;
import org.omg.CORBA.ORB;

public class RoomReservationImpl extends RoomReservationApp.RoomReservationPOA {

    private ORB orb;

    public void setORB(ORB orb) {
        this.orb = orb;
    }

    @Override
    public RMIResponse createRoom(short roomNumber, String date, String[] timeslots) {
        return new RMIResponse();
    }

    @Override
    public RMIResponse deleteRoom(short roomNumber, String date, String[] timeslots) {
        return null;
    }

    @Override
    public RMIResponse bookRoom(String identifier, Campus campusName, short roomNumber, String date, String timeslot) {
        return null;
    }

    @Override
    public RMIResponse getAvailableTimeSlot(String date) {
        return null;
    }

    @Override
    public RMIResponse cancelBooking(String identifier, String bookingId) {
        return null;
    }

    @Override
    public void shutdown() {
        orb.shutdown(true);
    }
}
