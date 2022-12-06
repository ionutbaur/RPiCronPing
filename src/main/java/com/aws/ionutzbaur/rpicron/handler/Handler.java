package com.aws.ionutzbaur.rpicron.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.datasource.ScanNotification;
import com.aws.ionutzbaur.rpicron.factory.NotificationServiceFactory;
import com.aws.ionutzbaur.rpicron.model.Notification;
import com.aws.ionutzbaur.rpicron.model.enums.NotificationType;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.net.Socket;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.aws.ionutzbaur.rpicron.util.CommonConstants.*;
import static com.aws.ionutzbaur.rpicron.util.Sanitizer.*;
import static com.aws.ionutzbaur.rpicron.util.Utils.convertStringToInt;
import static com.aws.ionutzbaur.rpicron.util.Utils.getEnv;

/**
 * This is the entry point for the Lambda function
 */

public class Handler {

    private static final String NOTIFICATION_TYPE = getEnv("NOTIFICATION_TYPE", NotificationType.MAIL.name());
    private static final String DEVICE_NAME = getEnv("DEVICE_NAME", "Raspberry Pi");
    private static final String REACHABLE_MESSAGE = DEVICE_NAME + " is now reachable!";
    private static final String NOT_REACHABLE_MESSAGE = DEVICE_NAME + " not reachable! Most probably a power outage.";
    private static final String NOTIFY_TRY_MESSAGE = "Will try to notify user.";

    private final ScanNotification scanNotification;
    private final NotificationService notificationService;

    public Handler() {
        this(new ScanNotification(), new NotificationServiceFactory().getNotificationService(NOTIFICATION_TYPE));
    }

    Handler(ScanNotification scanNotification, NotificationService notificationService) {
        this.scanNotification = scanNotification;
        this.notificationService = notificationService;
    }

    public void handleRequest(Context context) {
        LambdaLogger logger = context.getLogger();

        if (isIgnoredInterval(logger)) {
            // in this period the router has nightly restarts.
            logger.log("Interval ignored. Will do nothing.");
        } else {
            ping(logger);
        }
    }

    private void ping(LambdaLogger logger) {
        sanitizeRoute();

        final String host = getEnv(HOST_VAR);
        final int port = getPort();
        Notification notification = scanNotification.getNotification(logger);
        String messageToSend = "Status unknown!";   //should not happen
        boolean isUpdateNeeded = false;

        try (Socket ignored = new Socket(host, port)) {
            boolean isAlive = Boolean.TRUE.equals(notification.getAlive());
            if (isAlive) {
                logger.log(DEVICE_NAME + " is alive. No action needed.");
            } else {
                logger.log(DEVICE_NAME + " back alive! " + NOTIFY_TRY_MESSAGE);
                messageToSend = REACHABLE_MESSAGE;

                notification.setAlive(true);
                notification.setNextNotificationNeeded(true);

                isUpdateNeeded = true;
            }
        } catch (Exception e) {
            if (Boolean.TRUE.equals(notification.getNextNotificationNeeded())) {
                logger.log("Cannot connect to host due to " + Arrays.toString(e.getStackTrace()));
                logger.log(DEVICE_NAME + " down. " + NOTIFY_TRY_MESSAGE);
                messageToSend = NOT_REACHABLE_MESSAGE;

                notification.setAlive(false);
                notification.setNextNotificationNeeded(false);

                isUpdateNeeded = true;
            } else {
                logger.log(DEVICE_NAME + " still down. No further notification needed.");
            }
        } finally {
            if (isUpdateNeeded) {
                boolean isNotificationSent = notificationService.sendNotification(messageToSend, logger);
                logger.log(isNotificationSent ? "Successfully sent notification!" : "Notification not sent due to error!");
                notification.setLastSentOn(ZonedDateTime.now().withZoneSameInstant(ZoneId.of(BUCHAREST_ZONE_ID)).toString());

                DynamoDbTable<Notification> notificationTable = scanNotification.getTable();
                notificationTable.updateItem(notification);
            }
        }
    }

    private boolean isIgnoredInterval(LambdaLogger logger) {
        try {
            List<Predicate<ZonedDateTime>> predicateList = List.of( // add here other conditions if needed
                    zonedDateTime -> zonedDateTime.getHour() >= getBeginIgnoredHour(),
                    zonedDateTime -> zonedDateTime.getHour() <= getEndIgnoredHour(),
                    zonedDateTime -> zonedDateTime.getMinute() >= getBeginIgnoredMinute(),
                    zonedDateTime -> zonedDateTime.getMinute() <= getEndIgnoredMinute());

            return predicateList.stream()
                    .reduce(predicate -> true, Predicate::and)
                    .test(ZonedDateTime.now()
                            .withZoneSameInstant(ZoneId.of(BUCHAREST_ZONE_ID)));
        } catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }

    private static int getBeginIgnoredHour() {
        final String beginIgnoredHourAsString = getEnv(BEGIN_IGNORED_HOUR_VAR);
        final int beginIgnoredHour = convertStringToInt(beginIgnoredHourAsString, BEGIN_IGNORED_HOUR_VAR);

        return getValidHour(beginIgnoredHour);
    }

    private static int getEndIgnoredHour() {
        final String endIgnoredHourAsString = getEnv(END_IGNORED_HOUR_VAR);
        final int endIgnoredHour = convertStringToInt(endIgnoredHourAsString, END_IGNORED_HOUR_VAR);

        return getValidHour(endIgnoredHour);
    }

    private static int getBeginIgnoredMinute() {
        final String beginIgnoredMinuteAsString = getEnv(BEGIN_IGNORED_MINUTE_VAR);
        final int beginIgnoredMinute = convertStringToInt(beginIgnoredMinuteAsString, BEGIN_IGNORED_MINUTE_VAR);

        return getValidMinute(beginIgnoredMinute);
    }

    private static int getEndIgnoredMinute() {
        final String endIgnoredMinuteAsString = getEnv(END_IGNORED_MINUTE_VAR);
        final int endIgnoredMinute = convertStringToInt(endIgnoredMinuteAsString, END_IGNORED_MINUTE_VAR);

        return getValidMinute(endIgnoredMinute);
    }

    private static int getPort() {
        final String portAsString = getEnv(PORT_VAR);
        return convertStringToInt(portAsString, PORT_VAR);
    }

}