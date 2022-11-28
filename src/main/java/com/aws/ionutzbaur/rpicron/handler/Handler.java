package com.aws.ionutzbaur.rpicron.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.datasource.ScanNotification;
import com.aws.ionutzbaur.rpicron.model.Notification;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import com.aws.ionutzbaur.rpicron.service.impl.MailNotificationServiceImpl;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
    private static final String ROMANIA_ZONE_ID = "Europe/Bucharest";

    public void handleRequest(Context context) {
        LambdaLogger logger = context.getLogger();

        ScanNotification scanNotification = new ScanNotification();
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
                notification.setLastSentOn(LocalDateTime.now().atZone(ZoneId.of(ROMANIA_ZONE_ID)).toString());
                notification.setNextNotificationNeeded(true);

                isUpdateNeeded = true;
            }
        } catch (IOException e) {
            if (Boolean.TRUE.equals(notification.getNextNotificationNeeded())) {
                logger.log("Cannot connect to host due to " + e.getMessage());
                logger.log(DEVICE_NAME + " down. " + NOTIFY_TRY_MESSAGE);
                messageToSend = NOT_REACHABLE_MESSAGE;

                notification.setAlive(false);
                notification.setLastSentOn(LocalDateTime.now().atZone(ZoneId.of(ROMANIA_ZONE_ID)).toString());
                notification.setNextNotificationNeeded(false);

                isUpdateNeeded = true;
            } else {
                logger.log(DEVICE_NAME + " still down. No further notification needed.");
            }
        } finally {
            if (isUpdateNeeded) {
                DynamoDbTable<Notification> notificationTable = scanNotification.getTable();
                notificationTable.updateItem(notification);

                final NotificationService notificationService = new MailNotificationServiceImpl();
                boolean isNotificationSent = notificationService.sendNotification(messageToSend, context);
                logger.log(isNotificationSent ? "Successfully sent notification!" : "Notification not sent due to error!");
            }
        }
    }

}