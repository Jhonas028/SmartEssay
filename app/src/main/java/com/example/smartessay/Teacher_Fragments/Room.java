package com.example.smartessay.Teacher_Fragments;

public class Room {
    private String id;
    private String name;
    private String roomCode;

    public Room() {} // Needed for Firebase

    public Room(String id, String name, String roomCode) {
        this.id = id;
        this.name = name;
        this.roomCode = roomCode;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoomCode() { return roomCode; }
}
