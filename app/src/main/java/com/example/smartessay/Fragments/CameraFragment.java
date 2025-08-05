package com.example.smartessay.Fragments;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.smartessay.R;

public class CameraFragment extends Fragment {

    private ImageView imageView;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraCropper();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    Uri croppedUri = result.getUriContent();
                    imageView.setImageURI(croppedUri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        imageView = view.findViewById(R.id.imageView);

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCropper();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }

        return view;
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

        CropImageContractOptions contractOptions = new CropImageContractOptions(null, options);
        cropImageLauncher.launch(contractOptions);
    }
}

