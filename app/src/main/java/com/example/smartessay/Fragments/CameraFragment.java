package com.example.smartessay.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.smartessay.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraFragment extends Fragment {

    private ImageView imageView;
    private TextView ocrResultTextView;

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
                    new OCRRequestTask(imageFile, ocrResultTextView).execute();
                } else {
                    Toast.makeText(getContext(), "Image cropping failed", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        imageView = view.findViewById(R.id.imageView);
        ocrResultTextView = view.findViewById(R.id.ocr_result);

        // Launch cropper when root view clicked
        view.setOnClickListener(v -> checkPermissionAndLaunch());

        // Auto-launch on fragment start
        checkPermissionAndLaunch();

        return view;
    }

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

    // AsyncTask for OCR API call
    private static class OCRRequestTask extends AsyncTask<Void, Void, String> {
        private final File imageFile;
        private final TextView resultView;

        public OCRRequestTask(File imageFile, TextView resultView) {
            this.imageFile = imageFile;
            this.resultView = resultView;
        }

        @Override
        protected String doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("srcImg", imageFile.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), imageFile))
                    .addFormDataPart("includeSubScan", "0")
                    .addFormDataPart("Session", "string")
                    .build();

            Request request = new Request.Builder()
                    .url("https://pen-to-print-handwriting-ocr.p.rapidapi.com/recognize/")
                    .post(body)
                    .addHeader("x-rapidapi-key", "5d6b0c84c3msh8935cfeb2995b5fp15496djsnac76059af8ce") // <-- put working key here
                    .addHeader("x-rapidapi-host", "pen-to-print-handwriting-ocr.p.rapidapi.com")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return (response.isSuccessful() && response.body() != null)
                        ? response.body().string()
                        : "Error: " + response.code() + " - " + response.message();
            } catch (IOException e) {
                return "Exception: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);

                if (jsonObject.has("value")) {
                    String value = jsonObject.getString("value")
                            .replaceAll("\\n+", " ")
                            .replaceAll("\\s{2,}", " ")
                            .trim();

                    resultView.setText(value);
                } else {
                    resultView.setText("No 'value' in response: " + result);
                }
            } catch (JSONException e) {
                resultView.setText("Invalid OCR response: " + result);
            }
        }
    }
}
