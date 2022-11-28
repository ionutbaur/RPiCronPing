package com.aws.ionutzbaur.rpicron.model;

public class Email {

    private final String sender;
    private final String recipient;
    private final String subject;
    private final String bodyText;
    private final String bodyHTML;

    public Email(String sender, String recipient, String subject, String bodyText, String bodyHTML) {
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.bodyText = bodyText;
        this.bodyHTML = bodyHTML;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyText() {
        return bodyText;
    }

    public String getBodyHTML() {
        return bodyHTML;
    }
}
