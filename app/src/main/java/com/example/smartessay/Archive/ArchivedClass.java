package com.example.smartessay.Archive;

public class ArchivedClass {
    private String classroom_name;
    private String classroom_owner;
    private String created_at;
    private String room_code;
    private String status;
    private String key; // Firebase ID

    public ArchivedClass() {
        // Default constructor required for Firebase
    }

    public String getClassroom_name() { return classroom_name; }
    public String getClassroom_owner() { return classroom_owner; }
    public String getCreated_at() { return created_at; }
    public String getRoom_code() { return room_code; }
    public String getStatus() { return status; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
