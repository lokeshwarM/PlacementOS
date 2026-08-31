package com.placementos.backend.domain.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalized internal representation of a Gmail message.
 * Completely decouples the downstream business and persistence layer from Google API objects.
 */
public class NormalizedGmailMessage {

    private String messageId;
    private String threadId;
    private String subject;
    private String sender;
    private String recipients;
    private String plainTextBody;
    private String htmlBody;
    private String snippet;
    private Instant internalDate;
    private List<NormalizedAttachment> attachments = new ArrayList<>();

    public NormalizedGmailMessage() {}

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getPlainTextBody() {
        return plainTextBody;
    }

    public void setPlainTextBody(String plainTextBody) {
        this.plainTextBody = plainTextBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public Instant getInternalDate() {
        return internalDate;
    }

    public void setInternalDate(Instant internalDate) {
        this.internalDate = internalDate;
    }

    public List<NormalizedAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<NormalizedAttachment> attachments) {
        this.attachments = attachments;
    }
}
