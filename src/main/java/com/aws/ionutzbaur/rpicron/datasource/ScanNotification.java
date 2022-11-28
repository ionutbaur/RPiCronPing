package com.aws.ionutzbaur.rpicron.datasource;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.model.Notification;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.Iterator;

public class ScanNotification {

    private static final int UNIQ_ID = 1;

    public DynamoDbTable<Notification> getTable() {
        DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDbDatasource.getInstance().getDynamoDbEnhancedClient();

        // Create a DynamoDbTable object based on Notification class
        return dynamoDbEnhancedClient.table("Notification", TableSchema.fromBean(Notification.class));
    }

    public Notification getNotification(LambdaLogger logger) {
        AttributeValue idValue = AttributeValue.builder()
                .n(String.valueOf(UNIQ_ID))
                .build();
        Expression findExpression = Expression.builder()
                .expression("#id = :idValue")
                .putExpressionName("#id", "id")
                .putExpressionValue(":idValue", idValue)
                .build();
        ScanEnhancedRequest enhancedRequest = ScanEnhancedRequest.builder()
                .filterExpression(findExpression)
                .build();

        DynamoDbTable<Notification> notificationDynamoDbTable = getTable();
        try {
            Iterator<Notification> iterator = notificationDynamoDbTable.scan(enhancedRequest)
                    .items()
                    .iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                logger.log("Table is empty! Inserting brand new items.");
                return getBrandNewItem();
            }
        } catch (ResourceNotFoundException e) {
            logger.log("Table does not exist. Creating it...");
            notificationDynamoDbTable.createTable();
            notificationDynamoDbTable.putItem(getBrandNewItem());

            return getNotification(logger);
        }
    }

    private Notification getBrandNewItem() {
        Notification notification = new Notification();
        notification.setId(UNIQ_ID);
        notification.setAlive(false);
        notification.setNextNotificationNeeded(false);
        notification.setLastSentOn("never");

        return notification;
    }

}