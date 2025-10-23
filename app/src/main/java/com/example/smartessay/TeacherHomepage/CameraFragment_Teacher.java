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
    private TextView scoresTV;
    private EditText ocrResultTextView;
    EditText contentPercentage,organizationPercentage,grammarPercentage,languageStyle,otherTV,etRubricOtherScore;
    Button submitBtn,addPageBtn;
    private AlertDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate UI layout
        View view = inflater.inflate(R.layout.camera_fragment_teacher, container, false);

        // UI elements
        imageView = view.findViewById(R.id.imageView);
        ocrResultTextView = view.findViewById(R.id.ocr_result);

        // Rubrics inputs
        contentPercentage = view.findViewById(R.id.contentPercentage);
        organizationPercentage = view.findViewById(R.id.organizationPercentage);
        grammarPercentage = view.findViewById(R.id.grammarPercentage);
        languageStyle = view.findViewById(R.id.languageStyle);
        otherTV = view.findViewById(R.id.otherTV);
        etRubricOtherScore = view.findViewById(R.id.etRubricOtherScore);

        //++>> this method allows this field scrollable
        ocrResultTextView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        ocrResultTextView.setVerticalScrollBarEnabled(true);
        ocrResultTextView.setOnTouchListener((v, event) -> {
            // Allow the EditText to handle its own scroll
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        //disable copy paste
        disableCopyPaste(ocrResultTextView);
        //Allows double clicking to highlight and edit it.
        secureEssayEditText(ocrResultTextView);

        setupOtherFieldVisibility();

        // AI output
        scoresTV = view.findViewById(R.id.scoresTV);
        submitBtn = view.findViewById(R.id.submitBtn);

        addPageBtn = view.findViewById(R.id.addPageBtn);

        // Click to add page → check camera permission
        addPageBtn.setOnClickListener(v -> checkPermissionAndLaunch());

        // Click anywhere in view → also open camera
        view.setOnClickListener(v -> checkPermissionAndLaunch());

        // Auto-launch when fragment starts
        checkPermissionAndLaunch();

        // Submit essay for grading
        submitBtn.setOnClickListener(v -> {
            String essay = ocrResultTextView.getText().toString();
            if (essay.isEmpty()) { // IF OCR result is empty
                Toast.makeText(getContext(), "No essay detected!", Toast.LENGTH_SHORT).show();
                return; // stop execution
            }

            // Parse rubric values from EditText
            int content = parseEditText(contentPercentage);
            int organization = parseEditText(organizationPercentage);
            int grammar = parseEditText(grammarPercentage);
            int language = parseEditText(languageStyle);
            int other = parseEditText(etRubricOtherScore);

            int total = content + organization + grammar + language + other;

            // IF rubrics don’t add up to 100 → show error
            if (total != 100) {
                Toast.makeText(getContext(), "Rubrics must sum exactly to 100%", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading screen while grading
            showLoadingDialog("Grading essay...");

            // Call the OpenAI grading API

            Teacher_OpenAiAPI.gradeEssay(
                    essay,
                    contentPercentage.getText().toString(),         // Content %
                    organizationPercentage.getText().toString(),    // Organization %
                    grammarPercentage.getText().toString(),         // Grammar %
                    languageStyle.getText().toString(),             // Critical thinking / Language style %
                    etRubricOtherScore.getText().toString(),        // ✅ Other Criteria %
                    otherTV.getText().toString(),                   // Teacher notes
                    new Teacher_OpenAiAPI.GradeCallback() {
                        @Override
                        public void onSuccess(String result) {
                            try {
                                hideLoadingDialog(); // hide spinner
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
                                scoresTV.setText("Connection error. Please try again.");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            hideLoadingDialog();
                            scoresTV.setText("Error: " + error);
                        }
                    }
            );

        });

        return view;
    }

    // Converts EditText input into an integer (if empty/invalid → 0)
    private int parseEditText(EditText et) {
        String text = et.getText().toString().trim();
        if (text.isEmpty()) return 0; // IF no input → return 0
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0; // IF not a number → return 0
        }
    }

    // Request camera permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) { // IF user allows camera
                    launchCameraCropper();
                } else { // ELSE user denies camera
                    Toast.makeText(getContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    // Handle cropped image result
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) { // IF cropping success
                    Uri croppedUri = result.getUriContent();
                    imageView.setImageURI(croppedUri);

                    // Save cropped image to cache
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

                    // Send image to OCR API → convert handwriting to text
                    showLoadingDialog("Analyzing handwriting...");

                    PenToPrintAPI.sendImage(imageFile, ocrResultTextView, this::hideLoadingDialog);
                } else { // ELSE cropping failed
                    Toast.makeText(getContext(), "Image cropping failed", Toast.LENGTH_SHORT).show();
                }
            });

    // Check camera permission before launching cropper
    private void checkPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCropper();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // Launch the camera cropper tool
    private void launchCameraCropper() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeCamera = true;   // only camera
        options.imageSourceIncludeGallery = false; // no gallery
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

    // Show a loading dialog with a message
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

    // Hide the loading dialog if visible
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void setupOtherFieldVisibility() {
        // Initially hide "Other" EditText
        otherTV.setVisibility(View.GONE);

        // Listen for changes in "OtherScore"
        etRubricOtherScore.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    otherTV.setVisibility(View.GONE);
                } else {
                    otherTV.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }

    private void disableCopyPaste(EditText editText) {
        // Allow word selection (for editing)
        editText.setLongClickable(true);
        editText.setTextIsSelectable(true);

        // Disable copy/paste/cut actions
        editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.clear(); // Remove copy/paste options
                return false;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {}
        });

        // Disable insertion (the little clipboard popup)
        editText.setCustomInsertionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) { return false; }
            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {}
        });


    }

    private void secureEssayEditText(EditText editText) {
        // ✅ Allow editing and selection
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setCursorVisible(true);
        editText.setTextIsSelectable(true);

        // ✅ Keep highlight visible but remove copy/paste menu
        editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // Remove all menu options (Copy, Cut, Paste)
                menu.clear();
                return true; // ✅ Keep selection highlight visible
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {}
        });

        // ✅ Block long-press context menu (safety)
        editText.setLongClickable(false);
        editText.setOnCreateContextMenuListener((menu, v, menuInfo) -> menu.clear());

        // ✅ (Optional) Block hardware keyboard paste (Ctrl + V)
        editText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.isCtrlPressed() && keyCode == android.view.KeyEvent.KEYCODE_V)) {
                return true; // Block paste shortcut
            }
            return false;
        });
    }

}
