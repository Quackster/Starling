package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "messenger_messages")
public class MessengerMessageEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "receiver_id", nullable = false)
    private int receiverId;

    @Column(name = "sender_id", nullable = false)
    private int senderId;

    @Column(nullable = false, defaultValue = "1")
    private int unread = 1;

    @Column(nullable = false, type = "TEXT")
    private String body = "";

    @Column(nullable = false, defaultValue = "0")
    private long date;

    public int getId() { return id; }
    public int getReceiverId() { return receiverId; }
    public int getSenderId() { return senderId; }
    public int getUnread() { return unread; }
    public String getBody() { return body; }
    public long getDate() { return date; }

    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public void setUnread(int unread) { this.unread = unread; }
    public void setBody(String body) { this.body = body; }
    public void setDate(long date) { this.date = date; }
}
