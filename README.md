# soen423_a2

## To generate proto files:

> protoc -I=. --java_out=. requestObject.proto

> protoc -I=. --java_out=. responseObject.proto

> protoc -I=. --java_out=. centralRepository.proto

## To run IDLJ
> /usr/lib/jvm/jdk1.7.0_80/bin/idlj -fall RoomReservation.idl

## To start ORBD server
> /usr/lib/jvm/jdk1.7.0_80/bin/orbd -ORBInitialPort 8050 -ORBInitialHost localhost