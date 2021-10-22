package com.jotarata.astrocam;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraFragment extends Fragment {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;
    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;

    public CameraFragment() {
        // Required empty public constructor
    }

    public View currentView;


    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        currentView = inflater.inflate(R.layout.fragment_camera, container, false);
        return currentView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PERMISSION_GRANTED)
        {
            new Handler().postDelayed(this::StartCamera, 500);

        }else
        {
            Context context = getContext();
            assert context != null;

            CreateAlert(context, R.string.cameraPermission, null);
        }
    }

    private void CreateAlert(Context context, int id, @Nullable DialogInterface.OnClickListener callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setMessage(id);
        if (callback != null)
        {
            builder.setNeutralButton(R.string.Accept, callback);
        }
        builder.show();
    }

    @SuppressLint("RestrictedApi")
    void StartCamera() {
        Context context = getContext();
        Activity activity = getActivity();

        assert context != null;
        assert activity != null;

        if (currentView == null)
        {
            CreateAlert(context, R.string.viewCreationError, (dialog, which) -> activity.finish());
        }

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        if (manager == null) {
            CreateAlert(context, R.string.camera2NotSupported, null);
            return;
        }

        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We only use a camera that supports RAW in this sample.
                int[] cap = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                boolean rawSupported = false;
                for (int c : cap)
                {
                    if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
                    {
                        rawSupported = true;

                        break;
                    }
                }
                if (!rawSupported)
                {
                    Log.wtf("RAW", "Raw camera skipped");
                    continue;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        PreviewView preview_view = (PreviewView) currentView.findViewById(R.id.texture_view);
        Preview preview = new Preview.Builder()
                .build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();


        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setTargetResolution(new Size(720, 1280))
                .setBufferFormat(ImageFormat.RAW_SENSOR)
                .build();

        preview.setSurfaceProvider(preview_view.getSurfaceProvider());

        try {
            camera = cameraProviderFuture.get().bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (ExecutionException | InterruptedException e) {

            Toast.makeText(context, R.string.cameraInterrupted, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        ImageButton capture_button = currentView.findViewById(R.id.capture_button);
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CaptureImage(context, cameraSelector);
            }
        });
    }
    void CaptureImage(Context context, CameraSelector selector){
        Executor cameraExecutor = ContextCompat.getMainExecutor(context);


        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                Log.d("Image format", String.valueOf(image.getFormat()));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
            }
        });
    }
}