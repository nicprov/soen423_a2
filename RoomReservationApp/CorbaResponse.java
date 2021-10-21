package RoomReservationApp;


/**
* RoomReservationApp/CorbaResponse.java .
* Generated by the IDL-to-Java compiler (portable), version "4.1"
* from RoomReservation.idl
* Thursday, October 21, 2021 at 3:43:22 p.m. Eastern Daylight Time
*/

public final class CorbaResponse implements org.omg.CORBA.portable.IDLEntity
{
  public String message = null;
  public String date = null;
  public String requestType = null;
  public String requestParameters = null;
  public boolean status = false;

  public CorbaResponse ()
  {
  } // ctor

  public CorbaResponse (String _message, String _date, String _requestType, String _requestParameters, boolean _status)
  {
    message = _message;
    date = _date;
    requestType = _requestType;
    requestParameters = _requestParameters;
    status = _status;
  } // ctor

} // class CorbaResponse