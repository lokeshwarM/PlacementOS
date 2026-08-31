package com.placementos.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "gmail_sources")
public class GmailSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_address", nullable = false, unique = true)
    private String emailAddress;

    @Column(nullable = false)
    private String provider;

    @Column(columnDefinition = "TEXT")
    private String credential;

    @Column(nullable = false)
    private String status;

    /**
     * Opaque mailbox synchronization cursor, stored as a String to match the Gmail API contract.
     * The Gmail Java client library returns historyId as {@code String} from
     * {@code WatchResponse.getHistoryId()}. This is a cursor position only — it does NOT
     * indicate that Gmail messages have been fetched or processed. Actual Gmail History API
     * synchronization will be performed in the next milestone.
     */
    @Column(name = "last_history_id")
    private String lastHistoryId;

    /**
     * When the current Gmail push watch registration expires.
     * Gmail watches expire within approximately 7 days. A scheduled renewal
     * workflow (deferred to a future milestone) must renew before this time.
     */
    @Column(name = "watch_expiration")
    private ZonedDateTime watchExpiration;

    /**
     * Lifecycle state of the Gmail push watch.
     * Values: NONE, ACTIVE, EXPIRED
     */
    @Column(name = "watch_status", nullable = false)
    private String watchStatus = "NONE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // Default constructor for JPA
    public GmailSource() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastHistoryId() {
        return lastHistoryId;
    }

    public void setLastHistoryId(String lastHistoryId) {
        this.lastHistoryId = lastHistoryId;
    }

    public ZonedDateTime getWatchExpiration() {
        return watchExpiration;
    }

    public void setWatchExpiration(ZonedDateTime watchExpiration) {
        this.watchExpiration = watchExpiration;
    }

    public String getWatchStatus() {
        return watchStatus;
    }

    public void setWatchStatus(String watchStatus) {
        this.watchStatus = watchStatus;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
