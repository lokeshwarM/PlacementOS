package com.placementos.backend.domain.entity;

import com.placementos.backend.domain.enums.GmailMessageRetrievalStatus;
import com.placementos.backend.domain.model.GmailSource;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code gmail_messages} table.
 * Stores normalized Gmail message content acquired from Gmail API.
 */
@Entity
@Table(
    name = "gmail_messages",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_gmail_messages_source_message", columnNames = {"gmail_source_id", "message_id"})
    }
)
public class GmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 255)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gmail_source_id", nullable = false)
    private GmailSource gmailSource;

    @Column(name = "thread_id", length = 255)
    private String threadId;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "sender", length = 255)
    private String sender;

    @Column(name = "recipients", columnDefinition = "TEXT")
    private String recipients;

    @Column(name = "plain_text_body", columnDefinition = "TEXT")
    private String plainTextBody;

    @Column(name = "html_body", columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "snippet", columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "gmail_internal_date")
    private Instant gmailInternalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrieval_status", nullable = false, length = 50)
    private GmailMessageRetrievalStatus retrievalStatus = GmailMessageRetrievalStatus.RETRIEVED;

    @Column(name = "retrieved_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant retrievedAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;

    @OneToMany(mappedBy = "gmailMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (retrievedAt == null) retrievedAt = now;
        if (retrievalStatus == null) retrievalStatus = GmailMessageRetrievalStatus.RETRIEVED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public GmailMessage() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public GmailSource getGmailSource() {
        return gmailSource;
    }

    public void setGmailSource(GmailSource gmailSource) {
        this.gmailSource = gmailSource;
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

    public Instant getGmailInternalDate() {
        return gmailInternalDate;
    }

    public void setGmailInternalDate(Instant gmailInternalDate) {
        this.gmailInternalDate = gmailInternalDate;
    }

    public GmailMessageRetrievalStatus getRetrievalStatus() {
        return retrievalStatus;
    }

    public void setRetrievalStatus(GmailMessageRetrievalStatus retrievalStatus) {
        this.retrievalStatus = retrievalStatus;
    }

    public Instant getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(Instant retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        attachment.setGmailMessage(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmailMessage)) return false;
        GmailMessage other = (GmailMessage) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "GmailMessage{id=" + id + ", messageId='" + messageId + "', retrievalStatus=" + retrievalStatus + "}";
    }
}
