package com.example.smartessay.Student_Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.smartessay.API.PenToPrintAPI;
import com.example.smartessay.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        studentId = getIntent().getStringExtra("studentId");
        classroomId = getIntent().getStringExtra("roomCode");

        // Launch cropper when root view is clicked
        findViewById(R.id.mainLayout).setOnClickListener(v -> checkPermissionAndLaunch());

        // Auto-launch on activity start
        checkPermissionAndLaunch();

        submitBtn.setOnClickListener(v -> {
            String essayText = ocrResultTextView.getText().toString();
            if (essayText.isEmpty()) {
                Toast.makeText(this, "No essay detected!", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri imageUri = null;
            if (imageView.getDrawable() != null && imageView.getTag() != null) {
                imageUri = (Uri) imageView.getTag(); // store URI in setTag() when loading cropped image
            }

            uploadEssay(ocrResultTextView.getText().toString(), imageUri);


            uploadEssay(essayText, imageUri);
        });



        cancelBtn.setOnClickListener(v -> {
            imageView.setImageDrawable(null);
            ocrResultTextView.setText("");
        });
    }

    private void uploadEssay(String convertedText, Uri imageUri) {



        if (studentId == null || classroomId == null) {
            Toast.makeText(this, "Student or classroom info missing", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://smartessay-79d91-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference();

        // Create new essay ID
        String essayId = db.child("essay").push().getKey();
        if (essayId == null) {
            Toast.makeText(this, "Failed to generate essay ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Essay object
        long timestamp = System.currentTimeMillis();
        Essay essay = new Essay(
                studentId,
                classroomId,
                imageUri != null ? imageUri.toString() : "",
                convertedText,
                0,                  // grade default 0
                "uploaded",         // status
                timestamp,
                timestamp
        );

        // Save essay
        db.child("essay").child(essayId).setValue(essay)
                .addOnSuccessListener(aVoid -> {
                    // Link essay to classroom members
                    db.child("classrooms").child(classroomId)
                            .child("classroom_members")
                            .child(studentId)
                            .setValue(essayId)
                            .addOnSuccessListener(v -> Toast.makeText(this, "Essay uploaded!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update classroom members", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload essay", Toast.LENGTH_SHORT).show());
    }

    // Essay model class
    public static class Essay {
        public String student_id;
        public String classroom_id;
        public String image_url;
        public String converted_text;
        public int grade;
        public String status;
        public long created_at;
        public long updated_at;

        public Essay() {} // Required empty constructor

        public Essay(String student_id, String classroom_id, String image_url,
                     String converted_text, int grade, String status,
                     long created_at, long updated_at) {
            this.student_id = student_id;
            this.classroom_id = classroom_id;
            this.image_url = image_url;
            this.converted_text = converted_text;
            this.grade = grade;
            this.status = status;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }
    }


    // Permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraCropper();
                } else {
                    Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    // Cropper result
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    Uri croppedUri = result.getUriContent();
                    imageView.setImageURI(croppedUri);

                    // Save cropped image
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

                    // Send to OCR API
                    PenToPrintAPI.sendImage(imageFile, ocrResultTextView);
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
}
