package RoomReservationApp;


/**
* RoomReservationApp/RoomReservationHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from src/com/roomreservation/RoomReservation.idl
* Wednesday, October 13, 2021 6:41:41 o'clock PM EDT
*/

abstract public class RoomReservationHelper
{
  private static String  _id = "IDL:RoomReservationApp/RoomReservation:1.0";

  public static void insert (org.omg.CORBA.Any a, RoomReservationApp.RoomReservation that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static RoomReservationApp.RoomReservation extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (RoomReservationApp.RoomReservationHelper.id (), "RoomReservation");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static RoomReservationApp.RoomReservation read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_RoomReservationStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, RoomReservationApp.RoomReservation value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static RoomReservationApp.RoomReservation narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof RoomReservationApp.RoomReservation)
      return (RoomReservationApp.RoomReservation)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      RoomReservationApp._RoomReservationStub stub = new RoomReservationApp._RoomReservationStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static RoomReservationApp.RoomReservation unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof RoomReservationApp.RoomReservation)
      return (RoomReservationApp.RoomReservation)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      RoomReservationApp._RoomReservationStub stub = new RoomReservationApp._RoomReservationStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
