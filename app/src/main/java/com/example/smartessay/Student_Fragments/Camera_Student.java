package com.example.smartessay.Student_Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.smartessay.API.DUMMY_OpenAiAPI;
import com.example.smartessay.API.PenToPrintAPI;
import com.example.smartessay.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Camera_Student extends AppCompatActivity {

    private ImageView imageView;
    private TextView ocrResultTextView;
    private Button submitBtn, cancelBtn;

    private String studentId;
    private String classroomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_student);

        imageView = findViewById(R.id.imageView);
        ocrResultTextView = findViewById(R.id.ocr_result);
        submitBtn = findViewById(R.id.submitBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        // Get student and classroom info from intent
        studentId = getIntent().getStringExtra("studentId");
        classroomId = getIntent().getStringExtra("classroomId");

        if (studentId == null || classroomId == null) {
            Toast.makeText(this, "Student or classroom info missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Launch cropper when root view is clicked
        findViewById(R.id.mainLayout).setOnClickListener(v -> checkPermissionAndLaunch());

        // Auto-launch on activity start
        checkPermissionAndLaunch();

        submitBtn.setOnClickListener(v -> {
            String essayText = ocrResultTextView.getText().toString().trim();
            if (essayText.isEmpty()) {
                Toast.makeText(this, "No essay detected!", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference db = FirebaseDatabase.getInstance(
                    "https://smartessay-79d91-default-rtdb.firebaseio.com/"
            ).getReference();

            // ✅ Validate BEFORE AI grading
            db.child("essay")
                    .orderByChild("student_id")
                    .equalTo(studentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            boolean alreadySubmitted = false;
                            for (DataSnapshot essaySnap : snapshot.getChildren()) {
                                String cid = essaySnap.child("classroom_id").getValue(String.class);
                                if (cid != null && cid.equals(classroomId)) {
                                    alreadySubmitted = true;
                                    break;
                                }
                            }

                            if (alreadySubmitted) {
                                Toast.makeText(Camera_Student.this,
                                        "You already submitted an essay for this classroom!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                new androidx.appcompat.app.AlertDialog.Builder(Camera_Student.this)
                                        .setTitle("Submit Essay")
                                        .setMessage("Are you sure you want to submit this essay? You can only submit once.")
                                        .setPositiveButton("Yes", (dialog, which) -> {
                                            // ✅ Run AI grading first
                                            getRubricsFromTeacherAndGradeEssay(classroomId, essayText);
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(Camera_Student.this,
                                    "Firebase error: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        cancelBtn.setOnClickListener(v -> {
            imageView.setImageDrawable(null);
            ocrResultTextView.setText("");
        });
    }

    private void uploadEssay(String convertedText, int score, String feedback) {
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference();

        long timestamp = System.currentTimeMillis();

        // ✅ Double-check validation before saving
        db.child("essay")
                .orderByChild("student_id")
                .equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        boolean alreadySubmitted = false;
                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String cid = essaySnap.child("classroom_id").getValue(String.class);
                            if (cid != null && cid.equals(classroomId)) {
                                alreadySubmitted = true;
                                break;
                            }
                        }

                        if (alreadySubmitted) {
                            Toast.makeText(Camera_Student.this,
                                    "Essay already exists. Submission blocked.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // ✅ Fetch student info
                        db.child("users").child("students").child(studentId)
                                .get()
                                .addOnSuccessListener(stuSnap -> {
                                    String firstName = stuSnap.child("first_name").getValue(String.class);
                                    String lastName = stuSnap.child("last_name").getValue(String.class);
                                    String fullname = lastName + ", " + firstName;

                                    // ✅ Fetch classroom_name
                                    db.child("classrooms").child(classroomId).child("classroom_name")
                                            .get()
                                            .addOnSuccessListener(classSnap -> {
                                                String classroomName = classSnap.getValue(String.class);

                                                // ✅ Generate essayId before creating object
                                                String essayId = db.child("essay").push().getKey();
                                                String status =  "pending";
                                                if (essayId != null) {
                                                    Essay essay = new Essay(
                                                            essayId,
                                                            studentId,
                                                            classroomId,
                                                            classroomName,
                                                            convertedText,
                                                            score,
                                                            feedback,
                                                            status,
                                                            timestamp,
                                                            timestamp,
                                                            fullname
                                                    );

                                                    db.child("essay").child(essayId).setValue(essay)
                                                            .addOnSuccessListener(aVoid -> {
                                                                DatabaseReference memberRef = db.child("classrooms")
                                                                        .child(classroomId)
                                                                        .child("classroom_members")
                                                                        .child(studentId);

                                                                memberRef.child("joined_at").setValue(timestamp);
                                                                memberRef.child("fullname").setValue(fullname);
                                                                memberRef.child("status").setValue(status);

                                                                Toast.makeText(Camera_Student.this, "Essay uploaded successfully!", Toast.LENGTH_SHORT).show();
                                                            })
                                                            .addOnFailureListener(e ->
                                                                    Toast.makeText(Camera_Student.this, "Failed to upload essay: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                                            );
                                                }
                                            });
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(Camera_Student.this, "Failed to fetch student info: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(Camera_Student.this,
                                "Database error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ✅ Essay model class
    public static class Essay {
        public String essay_id;
        public String student_id;
        public String classroom_id;
        public String classroom_name;
        public String converted_text;
        public int score;
        public String essay_feedback;
        public String status;
        public long created_at;
        public long updated_at;
        public String fullname;

        public Essay() {} // Required empty constructor

        public Essay(String essay_id, String student_id, String classroom_id, String classroom_name,
                     String converted_text, int score, String essay_feedback,
                     String status, long created_at, long updated_at,
                     String fullname) {
            this.essay_id = essay_id;
            this.student_id = student_id;
            this.classroom_id = classroom_id;
            this.classroom_name = classroom_name;
            this.converted_text = converted_text;
            this.score = score;
            this.essay_feedback = essay_feedback;
            this.status = status;
            this.created_at = created_at;
            this.updated_at = updated_at;
            this.fullname = fullname;
        }
    }

    // ✅ Camera + Cropper code (unchanged)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraCropper();
                } else {
                    Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    Uri croppedUri = result.getUriContent();
                    imageView.setImageURI(croppedUri);

                    File imageFile = new File(getCacheDir(), "cropped_image.jpg");
                    try (InputStream in = getContentResolver().openInputStream(croppedUri);
                         OutputStream out = new FileOutputStream(imageFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Error saving cropped image", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // TODO: send to OCR API DO NOT REMOVE THIS
                    //PenToPrintAPI.sendImage(imageFile, ocrResultTextView);

                    // For testing
                    ocrResultTextView.setText(R.string.sample_essay);

                } else {
                    Toast.makeText(this, "Image cropping failed", Toast.LENGTH_SHORT).show();
                }
            });

    private void checkPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCropper();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraCropper() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeCamera = true;
        options.imageSourceIncludeGallery = false;
        options.allowFlipping = true;
        options.allowRotation = true;
        options.fixAspectRatio = false;
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.guidelines = CropImageView.Guidelines.ON;
        options.activityMenuIconColor = ContextCompat.getColor(this, R.color.white);
        options.toolbarColor = ContextCompat.getColor(this, R.color.black);
        options.toolbarBackButtonColor = ContextCompat.getColor(this, R.color.black);

        CropImageContractOptions contractOptions = new CropImageContractOptions(null, options);
        cropImageLauncher.launch(contractOptions);
    }

    private void getRubricsFromTeacherAndGradeEssay(String classroomId, String essay) {
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference();

        db.child("classrooms").child(classroomId).child("rubrics")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String contentIdeas = snapshot.child("Content and Ideas").getValue(String.class);
                        String developmentSupport = snapshot.child("Development and Support").getValue(String.class);
                        String grammarMechanics = snapshot.child("Grammar, Mechanics, and Formatting").getValue(String.class);
                        String languageStyle = snapshot.child("Language Use and Style").getValue(String.class);
                        String organizationStructure = snapshot.child("Organization and Structure").getValue(String.class);
                        String notes = snapshot.child("Notes").getValue(String.class);

                        DUMMY_OpenAiAPI.gradeEssay(
                                essay,
                                contentIdeas,
                                organizationStructure,
                                developmentSupport,
                                grammarMechanics,
                                languageStyle,
                                notes,
                                new DUMMY_OpenAiAPI.GradeCallback() {
                                    @Override
                                    public void onSuccess(String result) {
                                        Log.i("AI_RESULT", "Grading result: " + result);

                                        int score = 0;
                                        String feedback = "";

                                        try {
                                            JSONObject obj = new JSONObject(result);
                                            String rawResult = obj.optString("result", "");

                                            if (!rawResult.isEmpty()) {
                                                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                                        .compile("Score:\\s*([0-9.]+)%")
                                                        .matcher(rawResult);

                                                if (matcher.find()) {
                                                    float scoreFloat = Float.parseFloat(matcher.group(1));
                                                    score = Math.round(scoreFloat);
                                                }

                                                int firstLineBreak = rawResult.indexOf("\n");
                                                if (firstLineBreak != -1 && firstLineBreak + 1 < rawResult.length()) {
                                                    feedback = rawResult.substring(firstLineBreak + 1).trim();
                                                } else {
                                                    feedback = rawResult;
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e("AI_PARSE", "Failed to parse grading result: " + e.getMessage());
                                            feedback = "Parsing error: " + e.getMessage();
                                        }

                                        uploadEssay(essay, score, feedback);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e("AI_RESULT", "Error: " + error);
                                        uploadEssay(essay, 0, "No feedback generated (AI failed)");
                                    }
                                }
                        );
                    } else {
                        Toast.makeText(this, "No rubrics found for this classroom.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching rubrics: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
