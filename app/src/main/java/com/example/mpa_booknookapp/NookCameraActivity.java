package com.example.mpa_booknookapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class NookCameraActivity extends AppCompatActivity {

    //variables for class
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;  //app will access camera hardware at some point
    private PreviewView previewView;     //ui element for camera feed to appear in
    private Button bTakePicture;   //camerax use case that will snap the pic
    private ImageCapture imageCapture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nook_camera);

        //find previewview and button
        bTakePicture = findViewById(R.id.bCapture);
        previewView = findViewById(R.id.previewView);

        //from permissions stuff
        //check for if user already gave permission. if not ask for it. if yes crack on
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && //if permission to access camer and permission to write to gallery not granted:
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //ask perission now
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        } else {
            //if already granted go to this method
            startCameraProcess();
        }

        //if button clicked, run the capturePhoto() method
        bTakePicture.setOnClickListener(v -> capturePhoto());
    }


    //ask system for camera manager
    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        //get rid of any older camer connections first
        cameraProvider.unbindAll();
       //pick back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        //tell camera to use the previewview to display what it sees
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //speedy mode. try it and maybe change to prioritise img quality? test later.
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        //link camera to activity. stops camera if app minimised.
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }


    //helps to run background taks
    //returns an executor to direct tasks to main thresd. when camera ready/ig saved the success msg can appear without it all crashing.
    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    //starts camera process
    private void startCameraProcess() {
        //give camera when its ready
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    //when its ready:
        cameraProviderFuture.addListener(() -> {
            try {
                //get the camera manager to pass to camerax to show the preview
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                //call method to start camera
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    //capture photo and saves to gallery on phone
    private void capturePhoto() {
        //for giving unique ohoto name
        long timeStamp = System.currentTimeMillis();

        //to store details about img file before storing
        ContentValues contentValues = new ContentValues();

        //add timestamp to filename
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp);
        //itll be jpeg
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        //command to take pic
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(), //save to phones main gallery and label it
                getExecutor(), //deal with big processes like img saving so camera can keep going

                //deal with success of saving photo
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(NookCameraActivity.this, "Image saved to Gallery!", Toast.LENGTH_SHORT).show();

                        //get the digital drress/uri of the photo jus taken and saved
                        Uri savedUri = outputFileResults.getSavedUri();

                        //put uri into an intent to send back to the add nook activity
                        android.content.Intent resultIntent = new android.content.Intent();
                        if (savedUri != null) {
                            resultIntent.putExtra("imageUri", savedUri.toString());
                        }

                        //mark delivery as successful, hand it over, close the camera
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }

                    //didnt save. handle it
                    @Override
                    public void onError(@androidx.annotation.NonNull androidx.camera.core.ImageCaptureException exception) {
                        android.widget.Toast.makeText(NookCameraActivity.this, "Error: " + exception.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                    }
                });
    }


    //catches answer of permission popup as soon as its clicked by user
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
       //do normal ackground permission stuff
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //id code check. permission request went with code 101. checking for amtch.
        if (requestCode == 101) {
            // check if theres a result, and if its allow
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted. start camera process to show the camera.
                startCameraProcess();
            } else {
                //denied
                Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_SHORT).show();
                finish(); //close the screen asthey cant take a photo
            }
        }
    }

}