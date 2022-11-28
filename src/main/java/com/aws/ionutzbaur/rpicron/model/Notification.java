package com.aws.ionutzbaur.rpicron.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class Notification {

    private Integer id;
    private Boolean alive;
    private Boolean nextNotificationNeeded;
    private String lastSentOn;

    @DynamoDbPartitionKey
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @DynamoDbAttribute("alive")
    public Boolean getAlive() {
        return alive;
    }

    @DynamoDbAttribute("alive")
    public void setAlive(Boolean alive) {
        this.alive = alive;
    }

    @DynamoDbAttribute("nextNotificationNeeded")
    public Boolean getNextNotificationNeeded() {
        return nextNotificationNeeded;
    }

    @DynamoDbAttribute("nextNotificationNeeded")
    public void setNextNotificationNeeded(Boolean nextNotificationNeeded) {
        this.nextNotificationNeeded = nextNotificationNeeded;
    }

    @DynamoDbAttribute("lastSentOn")
    public String getLastSentOn() {
        return lastSentOn;
    }

    @DynamoDbAttribute("lastSentOn")
    public void setLastSentOn(String lastSentOn) {
        this.lastSentOn = lastSentOn;
    }
}
