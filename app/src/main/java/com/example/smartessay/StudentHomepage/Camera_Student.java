package com.example.smartessay.StudentHomepage;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.smartessay.API.PenToPrintAPI;
import com.example.smartessay.API.Student_OpenAiAPI;
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
    private Button submitBtn, cancelBtn, addPageBtn;

    private String studentId;
    private String classroomId;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_student);

        imageView = findViewById(R.id.imageView);
        ocrResultTextView = findViewById(R.id.ocr_result);
        submitBtn = findViewById(R.id.submitBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        // ✅ Get student and classroom info passed from previous screen
        studentId = getIntent().getStringExtra("studentId");
        classroomId = getIntent().getStringExtra("classroomId");


        addPageBtn = findViewById(R.id.addPageBtn);
        addPageBtn.setOnClickListener(v -> checkPermissionAndLaunch());

        // If missing studentId or classroomId, stop activity
        if (studentId == null || classroomId == null) {
            Toast.makeText(this, "Student or classroom info missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // When user clicks the main layout → launch camera & cropper
        findViewById(R.id.mainLayout).setOnClickListener(v -> checkPermissionAndLaunch());

        // Auto-launch cropper when activity starts
        checkPermissionAndLaunch();

        // ✅ When submit button is clicked
        submitBtn.setOnClickListener(v -> {
            String essayText = ocrResultTextView.getText().toString().trim();

            // If no essay text detected, show warning
            if (essayText.isEmpty()) {
                Toast.makeText(this, "No essay detected!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Connect to Firebase Realtime Database
            DatabaseReference db = FirebaseDatabase.getInstance(
                    "https://smartessay-79d91-default-rtdb.firebaseio.com/"
            ).getReference();

            // 🔎 Check if student already submitted an essay in this classroom
            db.child("essay")
                    .orderByChild("student_id")
                    .equalTo(studentId) // look for essays by this student
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            boolean alreadySubmitted = false;

                            // Loop through essays found for this student
                            for (DataSnapshot essaySnap : snapshot.getChildren()) {
                                String cid = essaySnap.child("classroom_id").getValue(String.class);

                                // If essay classroomId == current classroomId → student already submitted
                                if (cid != null && cid.equals(classroomId)) {
                                    alreadySubmitted = true;
                                    break;
                                }
                            }

                            // ✅ If already submitted → block submission
                            if (alreadySubmitted) {
                                Toast.makeText(Camera_Student.this,
                                        "You already submitted an essay for this classroom!",
                                        Toast.LENGTH_SHORT).show();
                            }
                            // ✅ Else → show confirmation dialog before submitting
                            else {
                                showYesNoDialog(
                                        "Submit Essay",
                                        "Are you sure you want to submit this essay? You can only submit once.",
                                        () -> {
                                            // ✅ User pressed Yes
                                            showLoadingDialog("Submitting essay...");
                                            getRubricsFromTeacherAndGradeEssay(classroomId, essayText);
                                        },
                                        () -> {
                                            // ❌ User pressed No / canceled → just dismiss
                                            Toast.makeText(Camera_Student.this, "Submission canceled", Toast.LENGTH_SHORT).show();
                                        }
                                );

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // If Firebase query fails
                            Toast.makeText(Camera_Student.this,
                                    "Firebase error: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // ✅ Cancel button clears image + essay text
        cancelBtn.setOnClickListener(v -> {
            imageView.setImageDrawable(null);
            ocrResultTextView.setText("");
        });
    }

    private void showYesNoDialog(String title, String message, Runnable onYes, Runnable onCancel) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_yes_no, null); // your custom layout XML

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        tvTitle.setText(title);
        tvMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnYes.setOnClickListener(v -> {
            if (onYes != null) onYes.run();
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> {
            if (onCancel != null) onCancel.run();
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> {
            if (onCancel != null) onCancel.run();
        });

        dialog.show();
    }


    /**
     * Upload essay to Firebase Database after grading.
     */
    private void uploadEssay(String convertedText, int score, String feedback) {
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference();

        long timestamp = System.currentTimeMillis();

        // 🔎 Double-check if essay already exists before saving
        db.child("essay")
                .orderByChild("student_id")
                .equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        boolean alreadySubmitted = false;

                        // Check if essay exists in the same classroom
                        for (DataSnapshot essaySnap : snapshot.getChildren()) {
                            String cid = essaySnap.child("classroom_id").getValue(String.class);
                            if (cid != null && cid.equals(classroomId)) {
                                alreadySubmitted = true;
                                break;
                            }
                        }

                        // If essay already submitted → block upload
                        if (alreadySubmitted) {
                            Toast.makeText(Camera_Student.this,
                                    "Essay already exists. Submission blocked.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // ✅ Fetch student info from Firebase
                        db.child("users").child("students").child(studentId)
                                .get()
                                .addOnSuccessListener(stuSnap -> {
                                    String firstName = stuSnap.child("first_name").getValue(String.class);
                                    String lastName = stuSnap.child("last_name").getValue(String.class);
                                    String fullname = lastName + ", " + firstName;

                                    // ✅ Fetch classroom name
                                    db.child("classrooms").child(classroomId).child("classroom_name")
                                            .get()
                                            .addOnSuccessListener(classSnap -> {
                                                String classroomName = classSnap.getValue(String.class);

                                                // Generate unique essayId
                                                String essayId = db.child("essay").push().getKey();
                                                String status = "pending";

                                                if (essayId != null) {
                                                    // Create Essay object
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

                                                    // Save essay object in Firebase
                                                    db.child("essay").child(essayId).setValue(essay)
                                                            .addOnSuccessListener(aVoid -> {
                                                                // Update classroom_members info
                                                                DatabaseReference memberRef = db.child("classrooms")
                                                                        .child(classroomId)
                                                                        .child("classroom_members")
                                                                        .child(studentId);

                                                                memberRef.child("joined_at").setValue(timestamp);
                                                                memberRef.child("fullname").setValue(fullname);
                                                                memberRef.child("status").setValue(status);

                                                                hideLoadingDialog();
                                                                Toast.makeText(Camera_Student.this, "Essay uploaded successfully!", Toast.LENGTH_SHORT).show();
                                                                finish();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                hideLoadingDialog();
                                                                Toast.makeText(Camera_Student.this, "Failed to upload essay: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            });
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

    // ✅ Essay model class for Firebase storage
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

        public Essay() {} // Empty constructor required by Firebase

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

    // ✅ Camera + Cropper code
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

                    // ✅ Show loading while OCR runs
                    showLoadingDialog("Analyzing handwriting...");

                    // ✅ Send image to OCR API
                    PenToPrintAPI.sendImage(imageFile, ocrResultTextView, this::hideLoadingDialog);

                    // For testing → show sample essay text
                    ocrResultTextView.setText(R.string.sample_essay);

                } else {
                    Toast.makeText(this, "Image cropping failed", Toast.LENGTH_SHORT).show();
                }
            });

    // ✅ Check camera permission
    private void checkPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCropper();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // ✅ Open camera + cropper
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

    /**
     * ✅ Get rubrics from Firebase → send essay to AI grading API → get score + feedback
     */
    private void getRubricsFromTeacherAndGradeEssay(String classroomId, String essay) {
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.firebaseio.com/"
        ).getReference();

        db.child("classrooms").child(classroomId).child("rubrics")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Get each rubric criteria from Firebase
                        String contentIdeas = snapshot.child("Content and Ideas").getValue(String.class);
                        String developmentSupport = snapshot.child("Development and Support").getValue(String.class);
                        String grammarMechanics = snapshot.child("Grammar, Mechanics, and Formatting").getValue(String.class);
                        String languageStyle = snapshot.child("Language Use and Style").getValue(String.class);
                        String organizationStructure = snapshot.child("Organization and Structure").getValue(String.class);
                        String notes = snapshot.child("Notes").getValue(String.class);

                        // Send essay + rubrics to dummy AI API for grading
                        Student_OpenAiAPI.gradeEssay(
                                essay,
                                contentIdeas,
                                organizationStructure,
                                grammarMechanics,
                                languageStyle,
                                notes,
                                new Student_OpenAiAPI.GradeCallback() {
                                    @Override
                                    public void onSuccess(String result) {
                                        Log.i("AI_RESULT", "Grading result: " + result);

                                        int score = 0;
                                        String feedback = "";

                                        try {
                                            JSONObject obj = new JSONObject(result);
                                            String rawResult = obj.optString("result", "");

                                            if (!rawResult.isEmpty()) {
                                                // Extract score percentage using regex
                                                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                                        .compile("Score:\\s*([0-9.]+)%")
                                                        .matcher(rawResult);

                                                if (matcher.find()) {
                                                    float scoreFloat = Float.parseFloat(matcher.group(1));
                                                    score = Math.round(scoreFloat);
                                                }

                                                // Extract feedback (text after first line break)
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

                                        // Upload essay with score + feedback
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

    // ✅ Show custom loading dialog
    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_creating_room_loading, null);
        ((TextView) dialogView.findViewById(R.id.tvLoadingMessage)).setText(message);

        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    // ✅ Hide loading dialog
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
