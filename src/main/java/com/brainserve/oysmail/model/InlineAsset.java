package com.brainserve.oysmail.model;

public record InlineAsset(String filename, byte[] bytes, String contentType) {}
