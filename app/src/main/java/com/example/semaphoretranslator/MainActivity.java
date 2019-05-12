package com.example.semaphoretranslator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    PoseEstimator poseEstimator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);

        poseEstimator = new PoseEstimator(this, "hourglass.tflite");

        long start = SystemClock.currentThreadTimeMillis();

        // Main code


        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.human2);

        poseEstimator.setInputData(poseEstimator.bitmapToPixels(bmp));
        poseEstimator.run();

        Log.d("TIME CONSUME", (SystemClock.currentThreadTimeMillis() - start) + "");

        poseEstimator.createKeyPoints();

        Bitmap outputPhoto = poseEstimator.generateOutputPhoto();

        imageView.setImageBitmap(outputPhoto);

        poseEstimator.close();

    }


}

