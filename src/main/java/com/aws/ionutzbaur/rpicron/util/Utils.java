package com.aws.ionutzbaur.rpicron.util;

import com.aws.ionutzbaur.rpicron.exception.RPiCronPingException;
import software.amazon.awssdk.regions.Region;

import static com.aws.ionutzbaur.rpicron.util.CommonConstants.AWS_REGION;

public class Utils {

    private Utils() {
        // not meant to be instantiated
    }

    public static Region getRegion() {
        return Region.of(System.getenv().getOrDefault(AWS_REGION, Region.EU_CENTRAL_1.id()));
    }

    public static int convertStringToInt(String number, String envVar) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            throw new RPiCronPingException(String.format("%s must be a number!", envVar));
        }
    }

}
