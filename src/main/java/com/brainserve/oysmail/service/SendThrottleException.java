package com.brainserve.oysmail.service;

/** Prevents repeated sends and excessive SMTP usage from a single admin instance. */
public class SendThrottleException extends RuntimeException {
    public SendThrottleException(String message) { super(message); }
}
