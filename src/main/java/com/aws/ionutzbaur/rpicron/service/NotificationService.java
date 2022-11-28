package com.aws.ionutzbaur.rpicron.service;

import com.amazonaws.services.lambda.runtime.Context;

public interface NotificationService {

    boolean sendNotification(String message, Context context);
}
