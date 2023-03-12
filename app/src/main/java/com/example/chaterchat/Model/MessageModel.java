package com.example.chaterchat.Model;

public class MessageModel {
    String message,uId,messageId,type;
    Long timestamp;

    public MessageModel(String uId, String message, String type, Long timestamp) {
        this.message = message;
        this.uId = uId;
        this.type = type;
        this.timestamp = timestamp;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
