package com.example.yolov5tfliteandroid;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;

import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

public class Screen extends AppCompatActivity {
    TextToSpeech mTTS;
    Button button2;
    SpeechRecognizer speechRecognizer;
    Intent specIntent;
    TextToSpeech textToSpeech;
    public String getText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        button2 = findViewById(R.id.button2);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i==TextToSpeech.SUCCESS){
                    int language = textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });
        startPageInfo();
    }

    public void speak(View view){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Start Speaking");
        startActivityForResult(intent,100);

    }
    public void info(View view){
        //String infoString = "You can login by saying 'Start'. Also, tap the bottom of the screen once to learn about application usage.";
        play();
    }
    public void startPageInfo(){
        String infoString = "Click 2 times above the screen to say the commands, and 2 times below the screen to get information about the commands and the application.";
        int speech = textToSpeech.speak(infoString, TextToSpeech.QUEUE_FLUSH,null);
    }
    public void play(){
        String infoString = "You can start application by saying for example 'Start School'.";
        int speech = textToSpeech.speak(infoString, TextToSpeech.QUEUE_FLUSH,null);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==100 && resultCode== RESULT_OK){
            getText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            if(getText.equals("Start School") || getText.equals("start School") || getText.equals("Start school") || getText.equals("start school")){
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.putExtra("School",getText);
                startActivity(intent);
            }
        }
    }
}