package com.placementos.backend.domain.event.payload;

public class GmailMessageRetrievedPayload {

    private String messageId;
    private String threadId;
    private String sourceEmail;
    private String subject;
    private String sender;
    private String snippet;

    public GmailMessageRetrievedPayload() {}

    public GmailMessageRetrievedPayload(String messageId, String threadId, String sourceEmail,
                                       String subject, String sender, String snippet) {
        this.messageId = messageId;
        this.threadId = threadId;
        this.sourceEmail = sourceEmail;
        this.subject = subject;
        this.sender = sender;
        this.snippet = snippet;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getSourceEmail() { return sourceEmail; }
    public void setSourceEmail(String sourceEmail) { this.sourceEmail = sourceEmail; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
}
