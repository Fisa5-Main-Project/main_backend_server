package com.know_who_how.main_server.mydata.service;

public class MydataCallDeferredException extends RuntimeException {

    public MydataCallDeferredException(String message) {
        super(message);
    }

    public MydataCallDeferredException(String message, Throwable cause) {
        super(message, cause);
    }
}
