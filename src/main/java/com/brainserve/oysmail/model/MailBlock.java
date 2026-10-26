package com.brainserve.oysmail.model;

/** A user-arranged body block. Text is escaped, never accepted as raw HTML. */
public record MailBlock(String type, String heading, String text, Integer imageIndex,
                        String label, String url) {}
