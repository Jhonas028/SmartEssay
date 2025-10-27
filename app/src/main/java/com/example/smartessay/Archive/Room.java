package com.example.smartessay.Archive;

public class Room {
    private String roomId;
    private String classroom_name;
    private String classroom_owner;
    private String created_at;
    private String room_code;
    private String status;

    // ✅ Required empty constructor for Firebase
    public Room() {}

    // ✅ Getters

    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public String getClassroom_name() {
        return classroom_name;
    }

    public String getClassroom_owner() {
        return classroom_owner;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getRoom_code() {
        return room_code;
    }

    public String getStatus() {
        return status;
    }
}
