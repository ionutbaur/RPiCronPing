package com.aws.ionutzbaur.rpicron.util;

import com.aws.ionutzbaur.rpicron.exception.RPiCronPingException;

import static com.aws.ionutzbaur.rpicron.util.CommonConstants.HOST_VAR;
import static com.aws.ionutzbaur.rpicron.util.CommonConstants.PORT_VAR;

public class Sanitizer {

    private Sanitizer() {
        // utility
    }

    public static void sanitizeRoute() {
        if (System.getenv(HOST_VAR) == null || System.getenv(PORT_VAR) == null) {
            throw new RPiCronPingException("Host and/or port not provided.");
        }
    }

    public static int getValidHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new RPiCronPingException("Hour must be a number between 0 and 23");
        }

        return hour;
    }

    public static int getValidMinute(int minute) {
        if (minute < 0 || minute > 59) {
            throw new RPiCronPingException("Minute must be a number between 0 and 59");
        }

        return minute;
    }

    public static void sanitizeMail(String sender, String recipient) {
        if (sender == null || recipient == null) {
            throw new RPiCronPingException("Cannot build mail without sender and/or recipient.");
        }
    }

    public static void sanitizePhone(String phone) {
        if (phone == null) {
            throw new RPiCronPingException("Phone number missing. Cannot send sms.");
        }
    }
}
