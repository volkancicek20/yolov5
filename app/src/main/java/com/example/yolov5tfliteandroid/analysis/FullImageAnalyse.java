package com.example.yolov5tfliteandroid.analysis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.example.yolov5tfliteandroid.MainActivity;
import com.example.yolov5tfliteandroid.R;
import com.example.yolov5tfliteandroid.detector.Yolov5TFLiteDetector;
import com.example.yolov5tfliteandroid.utils.ImageProcess;
import com.example.yolov5tfliteandroid.utils.Recognition;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FullImageAnalyse implements ImageAnalysis.Analyzer {

    public static class ObjectDistance{
        String label;
        float veryClose;
        float close;
        float farAway;
        static ArrayList<ObjectDistance> objectDistances = new ArrayList<>();
        ObjectDistance(String label, float veryClose, float close, float farAway){
            this.label = label; this.veryClose = veryClose; this.close = close; this.farAway = farAway;
            objectDistances.add(this);
        }
        ObjectDistance(){}

    }
    ObjectDistance door = new ObjectDistance("door", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance door1 = new ObjectDistance("door1", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance stairs = new ObjectDistance("stairs", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance column = new ObjectDistance("column", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance elevator = new ObjectDistance("elevator", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance automat = new ObjectDistance("automat", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance bench = new ObjectDistance("bench", 100000.0F, 10000.0F, 1000.0F);
    ObjectDistance trash = new ObjectDistance("trash", 100000.0F, 10000.0F, 1000.0F);

    //public ArrayList<String> obj = new ArrayList<>();
    public class Result{

        public Result(long costTime, Bitmap bitmap) {
            this.costTime = costTime;
            this.bitmap = bitmap;
        }
        long costTime;
        Bitmap bitmap;
    }

    ImageView boxLabelCanvas;
    PreviewView previewView;
    int rotation;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    ImageProcess imageProcess;
    private Yolov5TFLiteDetector yolov5TFLiteDetector;

    public FullImageAnalyse(Context context,
                            PreviewView previewView,
                            ImageView boxLabelCanvas,
                            int rotation,
                            TextView inferenceTimeTextView,
                            TextView frameSizeTextView,
                            Yolov5TFLiteDetector yolov5TFLiteDetector) {
        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.rotation = rotation;
        this.inferenceTimeTextView = inferenceTimeTextView;
        this.frameSizeTextView = frameSizeTextView;
        this.imageProcess = new ImageProcess();
        this.yolov5TFLiteDetector = yolov5TFLiteDetector;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        Observable.create( (ObservableEmitter<Result> emitter) -> {
            long start = System.currentTimeMillis();

            byte[][] yuvBytes = new byte[3][];
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            int imageHeight = image.getHeight();
            int imagewWidth = image.getWidth();

            imageProcess.fillBytes(planes, yuvBytes);
            int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            int[] rgbBytes = new int[imageHeight * imagewWidth];
            imageProcess.YUV420ToARGB8888(
                    yuvBytes[0],
                    yuvBytes[1],
                    yuvBytes[2],
                    imagewWidth,
                    imageHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes);

            Bitmap imageBitmap = Bitmap.createBitmap(imagewWidth, imageHeight, Bitmap.Config.ARGB_8888);
            imageBitmap.setPixels(rgbBytes, 0, imagewWidth, 0, 0, imagewWidth, imageHeight);

            double scale = Math.max(
                    previewHeight / (double) (rotation % 180 == 0 ? imagewWidth : imageHeight),
                    previewWidth / (double) (rotation % 180 == 0 ? imageHeight : imagewWidth)
            );
            Matrix fullScreenTransform = imageProcess.getTransformationMatrix(
                    imagewWidth, imageHeight,
                    (int) (scale * imageHeight), (int) (scale * imagewWidth),
                    rotation % 180 == 0 ? 90 : 0, false
            );

            Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imagewWidth, imageHeight, fullScreenTransform, false);
            Bitmap cropImageBitmap = Bitmap.createBitmap(fullImageBitmap, 0, 0, previewWidth, previewHeight);

            Matrix previewToModelTransform =
                    imageProcess.getTransformationMatrix(
                            cropImageBitmap.getWidth(), cropImageBitmap.getHeight(),
                            yolov5TFLiteDetector.getInputSize().getWidth(),
                            yolov5TFLiteDetector.getInputSize().getHeight(),
                            0, false);
            Bitmap modelInputBitmap = Bitmap.createBitmap(cropImageBitmap, 0, 0,
                    cropImageBitmap.getWidth(), cropImageBitmap.getHeight(),
                    previewToModelTransform, false);

            Matrix modelToPreviewTransform = new Matrix();
            previewToModelTransform.invert(modelToPreviewTransform);

            ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(modelInputBitmap);


            Bitmap emptyCropSizeBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
            Canvas cropCanvas = new Canvas(emptyCropSizeBitmap);
//
            Paint boxPaint = new Paint();
            boxPaint.setStrokeWidth(5);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setColor(Color.RED);
            Paint textPain = new Paint();
            textPain.setTextSize(50);
            textPain.setColor(Color.RED);
            textPain.setStyle(Paint.Style.FILL);

            float pixelArea = (float) 0;
            float maxConfidence = (float) 0;
            String distanceS ="";
            String labelParam = "";

            for (Recognition res : recognitions) {
                RectF location = res.getLocation();
                String label = res.getLabelName();
                float confidence = res.getConfidence();
                modelToPreviewTransform.mapRect(location);
                cropCanvas.drawRect(location, boxPaint);
                //obj.add(label);
                cropCanvas.drawText(label + ":" + String.format("%.2f", confidence), location.left, location.top, textPain);

                if (labelParam == "")
                    labelParam = label;
                if (maxConfidence == 0.F)
                    maxConfidence = confidence;
                if (recognitions.size()>1){
                    if(confidence > maxConfidence){
                        maxConfidence = confidence;
                        labelParam = label;
                    }
                }
                pixelArea = ((res.location.right - res.location.left)*(res.location.bottom - res.location.top));
            }

            // If any object detected, notify to the user
            if (recognitions.size()>0){
                // Algorithm for "info to give blind user"
                ObjectDistance objectDistance = new ObjectDistance();
                for (ObjectDistance obj: ObjectDistance.objectDistances) {
                    if(obj.label.equals(labelParam))
                        objectDistance.label = obj.label;
                        objectDistance.veryClose = obj.veryClose;
                        objectDistance.close = obj.close;
                        objectDistance.farAway = obj.farAway;
                        break;
                }
                if(pixelArea >= objectDistance.veryClose)
                    distanceS = "Very Close";
                else if(pixelArea >= objectDistance.close)
                    distanceS = "Close";
                else if(pixelArea >= objectDistance.farAway)
                    distanceS = "Far away";

                MainActivity.speak(labelParam, distanceS);
            }


            long end = System.currentTimeMillis();
            long costTime = (end - start);
            image.close();
            emitter.onNext(new Result(costTime, emptyCropSizeBitmap));

        }).subscribeOn(Schedulers.io())

                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Result result) -> {
                    boxLabelCanvas.setImageBitmap(result.bitmap);
                    frameSizeTextView.setText(previewHeight + "x" + previewWidth);
                    inferenceTimeTextView.setText(Long.toString(result.costTime) + "ms");
                });

    }

}
