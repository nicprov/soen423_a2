# soen423_a2

## Launch ORBD

> /usr/lib/jvm/jdk1.7.0_80/bin/orbd -port 1049 -ORBInitialPort 1050 -ORBInitialHost localhost& /usr/lib/jvm/jdk1.7.0_80/bin/orbd -port 1051 -ORBInitialPort 1052 -ORBInitialHost localhost& /usr/lib/jvm/jdk1.7.0_80/bin/orbd -port 1053 -ORBInitialPort 1054 -ORBInitialHost localhost&

## Generate stub and skeleton files

> java -jar lib/idlj.jar -fall RoomReservation.idl