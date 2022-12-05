package com.aws.ionutzbaur.rpicron.factory;

import com.aws.ionutzbaur.rpicron.exception.RPiCronPingException;
import com.aws.ionutzbaur.rpicron.model.enums.NotificationType;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import com.aws.ionutzbaur.rpicron.service.impl.MailNotificationServiceImpl;
import com.aws.ionutzbaur.rpicron.service.impl.SmsNotificationServiceImpl;

public class NotificationServiceFactory {

    public NotificationService getNotificationService(String type) {
        final NotificationType notificationType = NotificationType.valueOf(type);

        if (notificationType == NotificationType.MAIL) {
            return new MailNotificationServiceImpl();
        } else if (notificationType == NotificationType.PHONE) {
            return new SmsNotificationServiceImpl();
        }

        throw new RPiCronPingException("Notification of type " + notificationType + " currently not supported.");
    }
}
