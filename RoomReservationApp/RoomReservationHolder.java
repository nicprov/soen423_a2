package RoomReservationApp;

/**
* RoomReservationApp/RoomReservationHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "4.1"
* from RoomReservation.idl
* Thursday, October 21, 2021 at 3:43:22 p.m. Eastern Daylight Time
*/

public final class RoomReservationHolder implements org.omg.CORBA.portable.Streamable
{
  public RoomReservationApp.RoomReservation value = null;

  public RoomReservationHolder ()
  {
  }

  public RoomReservationHolder (RoomReservationApp.RoomReservation initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = RoomReservationApp.RoomReservationHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    RoomReservationApp.RoomReservationHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return RoomReservationApp.RoomReservationHelper.type ();
  }

}
