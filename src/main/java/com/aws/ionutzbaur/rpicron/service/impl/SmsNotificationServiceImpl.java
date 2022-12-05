package com.aws.ionutzbaur.rpicron.service.impl;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import com.aws.ionutzbaur.rpicron.util.Utils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import static com.aws.ionutzbaur.rpicron.util.Sanitizer.sanitizePhone;

// TODO: investigate why sms is not received
public class SmsNotificationServiceImpl implements NotificationService {

    private static final String PHONE_VAR = "PHONE";

    @Override
    public boolean sendNotification(String message, LambdaLogger logger) {
        final String phone = System.getenv(PHONE_VAR);
        sanitizePhone(phone);

        SnsClient snsClient = SnsClient.builder()
                .region(Utils.getRegion())
                // TODO: check credentials
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phone)
                    .build();

            snsClient.publish(request);
            return true;
        } catch (SnsException e) {
            logger.log("Cannot send sms due to " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }

}
