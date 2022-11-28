package com.aws.ionutzbaur.rpicron.service.impl;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

// TODO: investigate why is not working
public class SmsNotificationServiceImpl implements NotificationService {

    private static final String PHONE = "+40747781383";

    @Override
    public boolean sendNotification(String message, Context context) {
        SnsClient snsClient = SnsClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(PHONE)
                    .build();

            snsClient.publish(request);
            return true;
        } catch (SnsException e) {
            LambdaLogger logger = context.getLogger();
            logger.log("Cannot send sms due to " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }

}
