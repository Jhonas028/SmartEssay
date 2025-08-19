package com.example.smartessay.Student_Fragments;

public class Essay {
    private String studentId;
    private String classroomId;
    private String imageUrl;
    private String convertedText;
    private int grade;
    private String status;
    private String createdAt;
    private String updatedAt;

    // Required empty constructor
    public Essay() {}

    // Full constructor
    public Essay(String studentId, String classroomId, String imageUrl, String convertedText,
                 int grade, String status, String createdAt, String updatedAt) {
        this.studentId = studentId;
        this.classroomId = classroomId;
        this.imageUrl = imageUrl;
        this.convertedText = convertedText;
        this.grade = grade;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getConvertedText() { return convertedText; }
    public void setConvertedText(String convertedText) { this.convertedText = convertedText; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
