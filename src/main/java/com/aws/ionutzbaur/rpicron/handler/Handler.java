package com.aws.ionutzbaur.rpicron.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.datasource.ScanNotification;
import com.aws.ionutzbaur.rpicron.model.Notification;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import com.aws.ionutzbaur.rpicron.service.impl.MailNotificationServiceImpl;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.net.Socket;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * This is the entry point for the Lambda function
 */

public class Handler {

    private static final String HOST = "ionutbaur11.go.ro";
    private static final int SSH_PORT = 22;

    private static final String DEVICE_NAME = "Raspberry Pi";
    private static final String REACHABLE_MESSAGE = DEVICE_NAME + " is now reachable!";
    private static final String NOT_REACHABLE_MESSAGE = DEVICE_NAME + " not reachable! Most probably a power outage.";
    private static final String NOTIFY_TRY_MESSAGE = "Will try to notify user.";
    private static final String BUCHAREST_ZONE_ID = "Europe/Bucharest";

    private static final int BEGIN_IGNORED_HOUR = 3;
    private static final int BEGIN_IGNORED_MINUTE = 0;
    private static final int END_IGNORED_HOUR = 3;
    private static final int END_IGNORED_MINUTE = 4;

    private final ScanNotification scanNotification;
    private final NotificationService notificationService;

    public Handler() {
        this(new ScanNotification(), new MailNotificationServiceImpl());
    }

    Handler(ScanNotification scanNotification, NotificationService notificationService) {
        this.scanNotification = scanNotification;
        this.notificationService = notificationService;
    }

    public void handleRequest(Context context) {
        LambdaLogger logger = context.getLogger();

        if (isIgnoredInterval()) {
            // in this period the router has nightly restarts.
            logger.log("Interval ignored. Will do nothing.");
        } else {
            ping(logger);
        }
    }

    private void ping(LambdaLogger logger) {
        Notification notification = scanNotification.getNotification(logger);

        String messageToSend = "Status unknown!";   //should not happen
        boolean isUpdateNeeded = false;
        boolean isAlive = Boolean.TRUE.equals(notification.getAlive());
        try (Socket ignored = new Socket(HOST, SSH_PORT)) {
            if (isAlive) {
                logger.log(DEVICE_NAME + " is alive. No action needed.");
            } else {
                logger.log(DEVICE_NAME + " back alive! " + NOTIFY_TRY_MESSAGE);
                messageToSend = REACHABLE_MESSAGE;

                notification.setAlive(true);
                notification.setLastSentOn(ZonedDateTime.now().withZoneSameInstant(ZoneId.of(BUCHAREST_ZONE_ID)).toString());
                notification.setNextNotificationNeeded(true);

                isUpdateNeeded = true;
            }
        } catch (Exception e) {
            if (Boolean.TRUE.equals(notification.getNextNotificationNeeded())) {
                logger.log("Cannot connect to host due to " + Arrays.toString(e.getStackTrace()));
                logger.log(DEVICE_NAME + " down. " + NOTIFY_TRY_MESSAGE);
                messageToSend = NOT_REACHABLE_MESSAGE;

                notification.setAlive(false);
                notification.setLastSentOn(ZonedDateTime.now().withZoneSameInstant(ZoneId.of(BUCHAREST_ZONE_ID)).toString());
                notification.setNextNotificationNeeded(false);

                isUpdateNeeded = true;
            } else {
                logger.log(DEVICE_NAME + " still down. No further notification needed.");
            }
        } finally {
            if (isUpdateNeeded) {
                DynamoDbTable<Notification> notificationTable = scanNotification.getTable();
                notificationTable.updateItem(notification);

                boolean isNotificationSent = notificationService.sendNotification(messageToSend, logger);
                logger.log(isNotificationSent ? "Successfully sent notification!" : "Notification not sent due to error!");
            }
        }
    }

    private boolean isIgnoredInterval() {
        List<Predicate<ZonedDateTime>> predicateList = List.of( // add here other conditions if needed
                zonedDateTime -> zonedDateTime.getHour() >= BEGIN_IGNORED_HOUR,
                zonedDateTime -> zonedDateTime.getHour() <= END_IGNORED_HOUR,
                zonedDateTime -> zonedDateTime.getMinute() >= BEGIN_IGNORED_MINUTE,
                zonedDateTime -> zonedDateTime.getMinute() <= END_IGNORED_MINUTE);

        return predicateList.stream()
                .reduce(predicate -> true, Predicate::and)
                .test(ZonedDateTime.now()
                        .withZoneSameInstant(ZoneId.of(BUCHAREST_ZONE_ID)));
    }

}