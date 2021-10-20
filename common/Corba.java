package common;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import protobuf.protos.CentralRepository;

import static common.ConsoleColours.ANSI_RED;
import static common.ConsoleColours.RESET;

public class Corba {
    public static RoomReservationApp.RoomReservation connectCorba(String campus) {
        try {
            CentralRepository centralRepository = CentralRepositoryUtils.lookupServer(campus, "corba");
            if (centralRepository == null || !centralRepository.getStatus()){
                System.out.println("Unable to lookup server with central repository");
                System.exit(1);
            }
            int port = centralRepository.getPort();
            ORB orb = ORB.init( new String[0], null);
            org.omg.CORBA.Object objRef = orb.string_to_object("corbaloc::localhost:" + port + "/NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            return RoomReservationApp.RoomReservationHelper.narrow(ncRef.resolve_str("RoomReservation"));
        } catch (Exception e){
            System.out.println(ANSI_RED + "Unable to connect to corba: " + e.getMessage() + RESET);
        }
        return null;
    }
}
