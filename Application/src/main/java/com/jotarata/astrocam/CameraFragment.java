package com.jotarata.astrocam;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    private Camera camera;

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

        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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
            StartCamera();
        }else
        {
            Context context = getContext();
            assert context != null;

            new AlertDialog.Builder(context)
                    .setMessage(R.string.cameraPermission)
                    .show();
        }
    }

    private void StartCamera() {
        Context context = getContext();
        Activity activity = getActivity();

        assert context != null;
        assert activity != null;

        if (currentView == null)
        {
           new AlertDialog.Builder(context)
                    .setMessage(R.string.viewCreationError)
                    .setNegativeButton(R.string.Accept, (dialog, which) -> activity.finish())
                    .show();
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        PreviewView view = (PreviewView) currentView.findViewById(R.id.texture_view);
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(view.getSurfaceProvider());
        try {
            camera = cameraProviderFuture.get().bindToLifecycle((LifecycleOwner) context, selector, preview);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}