package com.aws.ionutzbaur.rpicron.service.impl;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.ionutzbaur.rpicron.model.Email;
import com.aws.ionutzbaur.rpicron.service.NotificationService;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

public class MailNotificationServiceImpl implements NotificationService {

    private static final String FROM = "ionut.baur@gmail.com";
    private static final String TO = "ionutzgabri91@gmail.com";
    private static final String SUBJECT = "Raspberry Pi status";
    private static final String INFO_MESSAGE = "Sent from AWS Lambda.";

    @Override
    public boolean sendNotification(String message, Context context) {
        LambdaLogger lambdaLogger = context.getLogger();
        try (SesClient client = SesClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build()) {

            // The email body for non-HTML email clients.
            String bodyText = message + "\r\n" + INFO_MESSAGE;
            // The HTML body of the email.
            String bodyHTML = "<html>" + "<head></head>" + "<body>" + "<h1>" + message + "</h1>"
                    + "<p>" + INFO_MESSAGE + "</p>" + "</body>" + "</html>";

            sendMail(client, new Email(FROM, TO, SUBJECT, bodyText, bodyHTML), lambdaLogger);
            return true;
        } catch (IOException | MessagingException e) {
            lambdaLogger.log(e.getMessage());
            return false;
        }
    }

    private void sendMail(SesClient client, Email email, LambdaLogger lambdaLogger) throws MessagingException, IOException {
        lambdaLogger.log("Attempting to send an email through Amazon SES using the AWS SDK for Java...");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MimeMessage message = buildMailContent(email);
        message.writeTo(outputStream);
        ByteBuffer buf = ByteBuffer.wrap(outputStream.toByteArray());

        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);

        SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
                .rawMessage(builder -> builder.data(SdkBytes.fromByteArray(arr)))
                .overrideConfiguration(builder -> builder.credentialsProvider(DefaultCredentialsProvider.create()))
                .build();

        client.sendRawEmail(rawEmailRequest);
    }

    private MimeMessage buildMailContent(Email email) throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);

        // Add subject, from and to lines.
        message.setSubject(email.getSubject(), "UTF-8");
        message.setFrom(new InternetAddress(email.getSender()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getRecipient()));

        // Create a multipart/alternative child container.
        MimeMultipart msgBody = new MimeMultipart("alternative");

        // Create a wrapper for the HTML and text parts.
        MimeBodyPart wrap = new MimeBodyPart();

        // Define the text part.
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(email.getBodyText(), "text/plain; charset=UTF-8");

        // Define the HTML part.
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(email.getBodyHTML(), "text/html; charset=UTF-8");

        // Add the text and HTML parts to the child container.
        msgBody.addBodyPart(textPart);
        msgBody.addBodyPart(htmlPart);

        // Add the child container to the wrapper object.
        wrap.setContent(msgBody);

        // Create a multipart/mixed parent container.
        MimeMultipart msg = new MimeMultipart("mixed");

        // Add the parent container to the message.
        message.setContent(msg);

        // Add the multipart/alternative part to the message.
        msg.addBodyPart(wrap);

        return message;
    }
}
