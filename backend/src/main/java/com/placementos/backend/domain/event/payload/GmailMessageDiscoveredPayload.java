package com.placementos.backend.domain.event.payload;

public class GmailMessageDiscoveredPayload {

    private String messageId;
    private String threadId;
    private String sourceEmail;

    public GmailMessageDiscoveredPayload() {}

    public GmailMessageDiscoveredPayload(String messageId, String threadId, String sourceEmail) {
        this.messageId = messageId;
        this.threadId = threadId;
        this.sourceEmail = sourceEmail;
    }

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

    public String getSourceEmail() {
        return sourceEmail;
    }

    public void setSourceEmail(String sourceEmail) {
        this.sourceEmail = sourceEmail;
    }
}
