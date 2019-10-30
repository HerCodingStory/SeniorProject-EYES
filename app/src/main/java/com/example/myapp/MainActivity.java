package com.example.myapp;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.wonderkiln.camerakit.CameraView;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import com.google.gson.Gson;
import android.graphics.Bitmap;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.wonderkiln.camerakit.CameraView;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraView;
import com.wonderkiln.camerakit.CameraKitEventListener;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import edmt.dev.edmtdevcognitivevision.Contract.AnalysisResult;
import edmt.dev.edmtdevcognitivevision.Contract.Caption;
import edmt.dev.edmtdevcognitivevision.Rest.VisionServiceException;
import edmt.dev.edmtdevcognitivevision.VisionServiceClient;
import edmt.dev.edmtdevcognitivevision.VisionServiceRestClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.camView) CameraView mCameraView;
    @BindView(R.id.cameraBtn) Button mCameraButton;

    //API Key for Microsoft Computer Vision API (Scene Recogntion)
    private final String API_KEY = "e142c9270906485a9dc505c268ffa409";
    private final String API_LINK = "https://visuallyimpairedapp.cognitiveservices.azure.com/vision/v1.0";

    // Creates connection with API
    VisionServiceClient visionServiceClient = new VisionServiceRestClient(API_KEY,API_LINK);

    // Initializing Components
    private TextToSpeech textToSpeech;
    private TextView txtResult;

    //
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        txtResult = (TextView)findViewById(R.id.txt_result);

        // Bottom navigation Bar
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Button Listener
        mCameraButton.setOnClickListener(new View. OnClickListener() {
            // When button is pressed, image is capture. It also stops text to speech if available
            @Override
            public void onClick(View v)
            {
                // Image is taken
                mCameraView.start();
                mCameraView.captureImage();

                // Text to speech stops if it is working
                if (textToSpeech != null)
                {
                    textToSpeech.stop();
                    textToSpeech.shutdown();
                }
            }
        });

        // Camera Listener
        mCameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {}

            @Override
            public void onError(CameraKitError cameraKitError) {}

            // Once image is taken and received, convert it to a bitmap and run the corresponding API
            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                // Create bitmap of image taken
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, mCameraView.getWidth(), mCameraView.getHeight(), false);

                // stop camera meanwhile recognition is happening
                mCameraView.stop();

                // Get selected API
                int number = navView.getSelectedItemId();

                // Depending on selected feature, we run the separate API
                switch(number)
                {
                    case R.id.navigation_home: // if item selected is home, then run text recognition
                        runTextRecognition(bitmap);
                        break;
                    case R.id.navigation_dashboard: // if item selected is dashboard, then run scene recognition
                        runSceneRecognition(bitmap);
                        break;
                    case R.id.navigation_notifications: // if item selected is notifications, then run object recognition
                        runObjectRecognition(bitmap);
                        break;
                }
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {}
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mCameraView.start();
    }

    @Override
    public void onPause()
    {
        mCameraView.stop();
        if(textToSpeech != null)
        {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    // Google Image Labels API
    private void runObjectRecognition(Bitmap bitmap)
    {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler();

        labeler.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        // Task completed successfully
                        // ...
                        StringBuilder stringResult = new StringBuilder();

                        for (FirebaseVisionImageLabel label: labels) {
                            if (label.getConfidence() > 0.8)
                                stringResult.append(label.getText());
                        }
                        String displayText = stringResult.toString();
                        processTextToSpeechResults(displayText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
    }

    // Microsoft Computer Vision API
    private void runSceneRecognition(Bitmap bitmap) {
        // Convert Bitmap to ByteArray
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Use Async to request API
        AsyncTask<InputStream,String,String> visionTask = new AsyncTask<InputStream, String, String>() {
            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

            @Override
            protected void onPreExecute() {progressDialog.show(); }

            @Override
            protected String doInBackground(InputStream... inputStreams) {
                try {
                    publishProgress("Recognizing");
                    String[] features = {"Description"}; // Get Description from API return result
                    String[] details = {};

                    AnalysisResult result = visionServiceClient.analyzeImage(inputStreams[0], features, details);

                    String JSONResult = new Gson().toJson(result);
                    return JSONResult;

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (VisionServiceException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (TextUtils.isEmpty(s)) {
                    Toast.makeText(MainActivity.this, "API return empty result", Toast.LENGTH_SHORT);
                } else {
                    progressDialog.dismiss();

                    AnalysisResult result = new Gson().fromJson(s, AnalysisResult.class);
                    StringBuilder stringResult = new StringBuilder();

                    for (Caption caption : result.description.captions) {
                        stringResult.append(caption.text);
                    }

                    String displayText = stringResult.toString();
                    txtResult.setText(displayText);

                    processTextToSpeechResults(displayText);

                }
            }

            @Override
            protected void onProgressUpdate(String... values)
            {
                progressDialog.setMessage(values[0]);
            }
        };

        visionTask.execute(inputStream);
    }

    private void runTextRecognition(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText texts) {
                        String resultText = texts.getText();
                        txtResult.setText(resultText);
                        processTextToSpeechResults(resultText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        e.printStackTrace();
                    }
                });
    }

    // Convert text to speech
    private void processTextToSpeechResults(String displayText)
    {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    int lanResult = textToSpeech.setLanguage(Locale.US);
                    if (lanResult == TextToSpeech.LANG_MISSING_DATA ||
                            lanResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    } else {
                        if(textToSpeech == null || "".equals(textToSpeech)) {
                            textToSpeech.speak("No Text Recognized", TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                        }
                        else {
                            textToSpeech.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                        }
                    }
                } else {
                    Log.e("error", "Initilization Failed!");
                }
            }
        });
    }
}