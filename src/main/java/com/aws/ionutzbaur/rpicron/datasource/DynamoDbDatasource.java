package com.aws.ionutzbaur.rpicron.datasource;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbDatasource {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbDatasource() {
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        // Create a DynamoDbEnhancedClient and use the DynamoDbClient object
        dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
    }

    public static DynamoDbDatasource getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final DynamoDbDatasource INSTANCE = new DynamoDbDatasource();
    }

    public DynamoDbEnhancedClient getDynamoDbEnhancedClient() {
        return dynamoDbEnhancedClient;
    }
}
