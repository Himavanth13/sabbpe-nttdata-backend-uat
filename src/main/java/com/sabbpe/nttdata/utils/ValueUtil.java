package com.sabbpe.nttdata.utils;

public class ValueUtil {

    public static String pick(String provided, String fallback) {
        return (provided != null && !provided.isBlank()) ? provided : fallback;
    }
}
