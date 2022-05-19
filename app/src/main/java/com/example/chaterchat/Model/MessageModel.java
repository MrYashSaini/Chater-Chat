package com.example.chaterchat.Model;

public class MessageModel {
    String message,uId,messageId;
    Long timestamp;

    public MessageModel(String message, String uId, Long timestamp) {
        this.message = message;
        this.uId = uId;
        this.timestamp = timestamp;
    }
    public MessageModel(String uId, String message) {
        this.message = message;
        this.uId = uId;
    }
    public MessageModel() {
    }
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
