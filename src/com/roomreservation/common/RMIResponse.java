package com.roomreservation.common;

import com.roomreservation.protobuf.protos.ResponseObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RMIResponse implements Serializable {
    private String message;
    private Date datetime;
    private String requestType;
    private String requestParameters;
    private boolean status;

    public RMIResponse(){
        this.message = "";
        this.datetime = new Date();
        this.requestType = "";
        this.requestParameters = "";
        this.status = false;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(String requestParameters) {
        this.requestParameters = requestParameters;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus(){
        return this.status;
    }

    public RMIResponse fromResponseObject(ResponseObject responseObject) throws ParseException {
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        RMIResponse rmiResponse = new RMIResponse();
        rmiResponse.setMessage(responseObject.getMessage());
        rmiResponse.setDatetime(dateTimeFormat.parse(responseObject.getDateTime()));
        rmiResponse.setRequestType(responseObject.getRequestType());
        rmiResponse.setRequestParameters(responseObject.getRequestParameters());
        rmiResponse.setStatus(responseObject.getStatus());
        return rmiResponse;
    }

    public ResponseObject toResponseObject(RMIResponse rmiResponse){
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        ResponseObject.Builder responseObject = ResponseObject.newBuilder();
        responseObject.setMessage(rmiResponse.message);
        responseObject.setDateTime(dateTimeFormat.format(rmiResponse.getDatetime()));
        responseObject.setRequestType(rmiResponse.getRequestType());
        responseObject.setRequestParameters(rmiResponse.getRequestParameters());
        responseObject.setStatus(rmiResponse.status);
        return responseObject.build();
    }

    public String toString(){
        return this.getDatetime() + "," + this.getMessage() + "," + this.getRequestType() + "," + this.getRequestParameters() + "," + this.getStatus();
    }
}
