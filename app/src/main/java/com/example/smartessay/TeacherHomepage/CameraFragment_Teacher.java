package com.example.smartessay.TeacherHomepage;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.smartessay.API.Teacher_OpenAiAPI;
import com.example.smartessay.API.PenToPrintAPI;
import com.example.smartessay.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CameraFragment_Teacher extends Fragment {

    private ImageView imageView;
    private TextView ocrResultTextView,scoresTV;
    EditText contentPercentage,organizationPercentage,grammarPercentage,languageStyle,otherTV;

    Button submitBtn,addPageBtn;
    private AlertDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment_teacher, container, false);
        imageView = view.findViewById(R.id.imageView);
        ocrResultTextView = view.findViewById(R.id.ocr_result);

        //Rubrics
        contentPercentage = view.findViewById(R.id.contentPercentage);
        organizationPercentage = view.findViewById(R.id.organizationPercentage);
        grammarPercentage = view.findViewById(R.id.grammarPercentage);
        languageStyle = view.findViewById(R.id.languageStyle);
        otherTV = view.findViewById(R.id.otherTV);

        //Ai
        scoresTV = view.findViewById(R.id.scoresTV);
        submitBtn = view.findViewById(R.id.submitBtn);

        addPageBtn = view.findViewById(R.id.addPageBtn);

        addPageBtn.setOnClickListener(v -> checkPermissionAndLaunch());

        // Launch cropper when root view clicked
        view.setOnClickListener(v -> checkPermissionAndLaunch());

        // Auto-launch on fragment start
        checkPermissionAndLaunch();

        submitBtn.setOnClickListener(v -> {
            String essay = ocrResultTextView.getText().toString();
            if (essay.isEmpty()) {
                Toast.makeText(getContext(), "No essay detected!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Parse rubric values
            int content = parseEditText(contentPercentage);
            int organization = parseEditText(organizationPercentage);
            int grammar = parseEditText(grammarPercentage);
            int language = parseEditText(languageStyle);

            int total = content + organization + grammar + language;

            // 🔹 Validation: must equal 100
            if (total != 100) {
                Toast.makeText(getContext(), "Rubrics must sum exactly to 100%", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoadingDialog("Grading essay...");

            // ✅ If valid, proceed with API
            Teacher_OpenAiAPI.gradeEssay(
                    essay,
                    contentPercentage.getText().toString(),
                    organizationPercentage.getText().toString(),
                    grammarPercentage.getText().toString(),
                    languageStyle.getText().toString(),
                    otherTV.getText().toString(),
                    new Teacher_OpenAiAPI.GradeCallback() {
                        @Override
                        public void onSuccess(String result) {
                            try {
                                hideLoadingDialog();
                                JSONObject jsonObject = new JSONObject(result);
                                String rawText = jsonObject.getString("result");

                                String formatted = rawText
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                        .trim();

                                scoresTV.setText(formatted);
                                Log.i("resultOpenAI", formatted);

                            } catch (JSONException e) {
                                e.printStackTrace();
                                scoresTV.setText("Error parsing result");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            hideLoadingDialog(); // ✅ Hide on error
                            scoresTV.setText("Error: " + error);
                        }
                    }
            );
        });


        return view;
    }

    private int parseEditText(EditText et) {
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return 0;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    // Camera permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraCropper();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    // Crop image result
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    Uri croppedUri = result.getUriContent();
                    imageView.setImageURI(croppedUri);

                    // Save cropped image to a file
                    File imageFile = new File(requireContext().getCacheDir(), "cropped_image.jpg");
                    try (InputStream in = requireContext().getContentResolver().openInputStream(croppedUri);
                         OutputStream out = new FileOutputStream(imageFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Error saving cropped image", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Send image to OCR API
                    //This is an API PLEASE DO NOT REMOVE THIS
                    PenToPrintAPI.sendImage(imageFile, ocrResultTextView);
                } else {
                    Toast.makeText(getContext(), "Image cropping failed", Toast.LENGTH_SHORT).show();
                }
            });

    private void checkPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
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
        options.activityMenuIconColor = ContextCompat.getColor(requireContext(), R.color.white);
        options.toolbarColor = ContextCompat.getColor(requireContext(), R.color.black);
        options.toolbarBackButtonColor = ContextCompat.getColor(requireContext(), R.color.black);

        CropImageContractOptions contractOptions = new CropImageContractOptions(null, options);
        cropImageLauncher.launch(contractOptions);
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_creating_room_loading, null);
        ((TextView) dialogView.findViewById(R.id.tvLoadingMessage)).setText(message);

        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }




}
