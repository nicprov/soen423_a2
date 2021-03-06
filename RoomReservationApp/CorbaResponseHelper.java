package RoomReservationApp;


/**
* RoomReservationApp/CorbaResponseHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "4.1"
* from RoomReservation.idl
* Thursday, October 21, 2021 at 3:43:22 p.m. Eastern Daylight Time
*/

abstract public class CorbaResponseHelper
{
  private static String  _id = "IDL:RoomReservationApp/CorbaResponse:1.0";

  public static void insert (org.omg.CORBA.Any a, RoomReservationApp.CorbaResponse that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static RoomReservationApp.CorbaResponse extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [5];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[0] = new org.omg.CORBA.StructMember (
            "message",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[1] = new org.omg.CORBA.StructMember (
            "date",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[2] = new org.omg.CORBA.StructMember (
            "requestType",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[3] = new org.omg.CORBA.StructMember (
            "requestParameters",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_boolean);
          _members0[4] = new org.omg.CORBA.StructMember (
            "status",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (RoomReservationApp.CorbaResponseHelper.id (), "CorbaResponse", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static RoomReservationApp.CorbaResponse read (org.omg.CORBA.portable.InputStream istream)
  {
    RoomReservationApp.CorbaResponse value = new RoomReservationApp.CorbaResponse ();
    value.message = istream.read_string ();
    value.date = istream.read_string ();
    value.requestType = istream.read_string ();
    value.requestParameters = istream.read_string ();
    value.status = istream.read_boolean ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, RoomReservationApp.CorbaResponse value)
  {
    ostream.write_string (value.message);
    ostream.write_string (value.date);
    ostream.write_string (value.requestType);
    ostream.write_string (value.requestParameters);
    ostream.write_boolean (value.status);
  }

}
