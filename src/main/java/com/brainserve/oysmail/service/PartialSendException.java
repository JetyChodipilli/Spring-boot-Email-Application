package com.brainserve.oysmail.service;

/** A batch can be partially accepted by SMTP; callers must not retry the full list blindly. */
public class PartialSendException extends RuntimeException {
    private final int accepted;
    private final int total;

    public PartialSendException(int accepted, int total, Throwable cause) {
        super("Email batch did not complete.", cause);
        this.accepted = accepted;
        this.total = total;
    }

    public int accepted() { return accepted; }
    public int total() { return total; }
}
