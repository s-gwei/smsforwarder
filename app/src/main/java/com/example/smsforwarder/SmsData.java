package com.example.smsforwarder;
import java.io.Serializable;
public class SmsData implements Serializable {
    public String sender, body, timestamp, displayNumber;
    public int slotId;
    public SmsData(String sender, String body, String timestamp, int slotId, String displayNumber) {
        this.sender = sender;
        this.body = body;
        this.timestamp = timestamp;
        this.slotId = slotId;
        this.displayNumber = displayNumber;
    }
}