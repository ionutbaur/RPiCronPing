package com.aws.ionutzbaur.rpicron.util;

import com.aws.ionutzbaur.rpicron.exception.RPiCronPingException;
import software.amazon.awssdk.regions.Region;

import static com.aws.ionutzbaur.rpicron.util.CommonConstants.AWS_REGION_VAR;

public class Utils {

    private Utils() {
        // not meant to be instantiated
    }

    public static String getEnv(String varName) {
        return System.getenv(varName);
    }

    public static String getEnv(String varName, String defaultValue) {
        return System.getenv().getOrDefault(varName, defaultValue);
    }

    public static Region getRegion() {
        return Region.of(getEnv(AWS_REGION_VAR, Region.EU_CENTRAL_1.id()));
    }

    public static int convertStringToInt(String number, String envVar) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            throw new RPiCronPingException(String.format("%s must be a number!", envVar));
        }
    }

}
