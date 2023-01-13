package com.example.yolov5tfliteandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.view.PreviewView;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.lifecycle.ProcessCameraProvider;

import com.example.yolov5tfliteandroid.analysis.FullImageAnalyse;
import com.example.yolov5tfliteandroid.analysis.FullScreenAnalyse;
import com.example.yolov5tfliteandroid.detector.Yolov5TFLiteDetector;
import com.example.yolov5tfliteandroid.utils.CameraProcess;
import com.google.common.util.concurrent.ListenableFuture;


import java.util.ArrayList;
import java.util.Locale;
import android.os.Vibrator;

public class MainActivity extends AppCompatActivity {


    private static TextToSpeech mTTS;
    static Vibrator v;
    static ArrayList<String> objRecord = new ArrayList<>();

    static int counter = 0;

    public static void vibratePhone(int ms) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(ms);
        }
    }

    public static void speak(String text, String distance) {
        if(objRecord.size() != 0) {
            if(objRecord.get(objRecord.size()-1) == text){
                System.out.println("Same object detected. Count: " + (counter + 1));
                counter++;
                if(counter > 4){
                    counter = 0;
                    mTTS.speak((text + " " + distance), TextToSpeech.QUEUE_FLUSH, null);
                    vibratePhone(150);
                }
                return;
            }
        }
        vibratePhone(150);
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        objRecord.add(text);
    }

    FullImageAnalyse fullImageAnalyse;
    MediaPlayer player = null;
    private boolean IS_FULL_SCREEN = false;

    private PreviewView cameraPreviewMatch;
    private PreviewView cameraPreviewWrap;
    private ImageView boxLabelCanvas;
    private Spinner modelSpinner;
    private Switch immersive;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Yolov5TFLiteDetector yolov5TFLiteDetector;

    private CameraProcess cameraProcess = new CameraProcess();



    //ekran yönünü aliniyor
    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    // model baslatma fonksiyonu
    private void initModel(String modelName) {

        try {
            this.yolov5TFLiteDetector = new Yolov5TFLiteDetector();
            this.yolov5TFLiteDetector.setModelFile(modelName);
//           this.yolov5TFLiteDetector.addNNApiDelegate();
            this.yolov5TFLiteDetector.addGPUDelegate();
            this.yolov5TFLiteDetector.initialModel(this);
            Log.i("model", "Success loading model" + this.yolov5TFLiteDetector.getModelFile());
        } catch (Exception e) {
            Log.e("image", "load model error: " + e.getMessage() + e.toString());
        }
//deneme
    }


    //main fonksiyonu
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Vibrator object
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Text to Speech object
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Uygulamayı açarken üst durum çubuğunu gizleyin
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // Tam ekran
        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);

        // Tam ekran
        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);
//      cameraPreviewWrap.setScaleType(PreviewView.ScaleType.FILL_START);

        // box/label画面
        boxLabelCanvas = findViewById(R.id.box_label_canvas);

        // Spinner
        modelSpinner = findViewById(R.id.model);

        // immersive
        immersive = findViewById(R.id.immersive);

        // Bazı görünümler gerçek zamanlı olarak güncellenir
        inferenceTimeTextView = findViewById(R.id.inference_time);
        frameSizeTextView = findViewById(R.id.frame_size);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // Kamera izni için başvur
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);
        }

        // Cep telefonu kamerasının kamera dönüş parametrelerini edinin
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image", "rotation: " + rotation);

        cameraProcess.showCameraSupportSize(MainActivity.this);

        // default model olarak yolov5s baslatiliyor
        initModel("bizimModel");

        if(IS_FULL_SCREEN){
            cameraPreviewWrap.removeAllViews();
            FullScreenAnalyse fullScreenAnalyse = new FullScreenAnalyse(MainActivity.this,
                    cameraPreviewMatch,
                    boxLabelCanvas,
                    rotation,
                    inferenceTimeTextView,
                    frameSizeTextView,
                    yolov5TFLiteDetector);
            cameraProcess.startCamera(MainActivity.this, fullScreenAnalyse, cameraPreviewMatch);
        }else{
            cameraPreviewMatch.removeAllViews();
            FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                    MainActivity.this,
                    cameraPreviewWrap,
                    boxLabelCanvas,
                    rotation,
                    inferenceTimeTextView,
                    frameSizeTextView,
                    yolov5TFLiteDetector);
            cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);
        }
    }
}