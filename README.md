# soen423_a2

## To generate proto files:

> protoc -I=. --java_out=. udpRequest.proto

> protoc -I=. --java_out=. udpResponse.proto

## To run IDLJ
> /usr/lib/jvm/jdk1.7.0_80/bin/idlj -td src/com/roomreservation -fall src/com/roomreservation/RoomReservation.idl

## To start ORBD server
> /usr/lib/jvm/jdk1.7.0_80/bin/orbd -ORBInitialPort 8050 -ORBInitialHost localhost