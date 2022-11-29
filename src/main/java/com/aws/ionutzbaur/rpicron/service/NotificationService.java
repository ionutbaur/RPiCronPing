package com.aws.ionutzbaur.rpicron.service;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public interface NotificationService {

    boolean sendNotification(String message, LambdaLogger logger);
}
