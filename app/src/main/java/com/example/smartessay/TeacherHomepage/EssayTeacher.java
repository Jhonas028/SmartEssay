package com.example.smartessay.TeacherHomepage;

public class EssayTeacher {
    private String student_id;
    private String classroom_id;
    private String converted_text;
    private String essay_feedback;
    private int score;
    private String status;

    private long created_at;
    private long updated_at;
    private String fullname;

    private String essay_id;

    public String getEssayId() {
        return essay_id;
    }

    public void setEssayId(String essay_id) {
        this.essay_id = essay_id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    // Required empty constructor for Firebase
    public EssayTeacher() {}

    // Getters & Setters
    public String getStudentId() {
        return student_id;
    }

    public void setStudentId(String student_id) {
        this.student_id = student_id;
    }

    public String getClassroomId() {
        return classroom_id;
    }

    public void setClassroomId(String classroom_id) {
        this.classroom_id = classroom_id;
    }

    public String getConvertedText() {
        return converted_text;
    }

    public void setConvertedText(String converted_text) {
        this.converted_text = converted_text;
    }

    public String getEssayFeedback() {
        return essay_feedback;
    }

    public void setEssayFeedback(String essay_feedback) {
        this.essay_feedback = essay_feedback;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(long created_at) {
        this.created_at = created_at;
    }

    public long getUpdatedAt() {
        return updated_at;
    }

    public void setUpdatedAt(long updated_at) {
        this.updated_at = updated_at;
    }
}
