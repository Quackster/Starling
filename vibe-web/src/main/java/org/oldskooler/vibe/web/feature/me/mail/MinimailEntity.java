package org.oldskooler.vibe.web.feature.me.mail;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "minimail")
public class MinimailEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "senderid", nullable = false, defaultValue = "0")
    private int senderId;

    @Column(name = "to_id", nullable = false)
    private int recipientId;

    @Column(nullable = false, length = 100)
    private String subject = "";

    @Column(nullable = false, defaultValue = "0")
    private long time;

    @Column(nullable = false, type = "LONGTEXT")
    private String message = "";

    @Column(name = "read_mail", nullable = false, defaultValue = "0")
    private int readMail;

    @Column(nullable = false, defaultValue = "0")
    private int deleted;

    @Column(name = "conversationid", nullable = false, defaultValue = "0")
    private int conversationId;

    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getRecipientId() { return recipientId; }
    public String getSubject() { return subject; }
    public long getTime() { return time; }
    public String getMessage() { return message; }
    public int getReadMail() { return readMail; }
    public int getDeleted() { return deleted; }
    public int getConversationId() { return conversationId; }

    public void setSenderId(int senderId) { this.senderId = senderId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setTime(long time) { this.time = time; }
    public void setMessage(String message) { this.message = message; }
    public void setReadMail(int readMail) { this.readMail = readMail; }
    public void setDeleted(int deleted) { this.deleted = deleted; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
}
