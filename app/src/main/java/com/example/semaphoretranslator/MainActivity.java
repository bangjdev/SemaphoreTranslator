package com.example.semaphoretranslator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;

import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG";

    ImageView imageView;
    Button buttonNext, buttonPrev;

    PoseEstimator poseEstimator;
    ArrayList<Bitmap> frames;
    int frameKey = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        init();

        performFileSearch();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        poseEstimator.close();
    }

    ArrayList<Bitmap> loadFrames(Uri path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(getApplicationContext(), path);
        ArrayList<Bitmap> bitmaps = new ArrayList<>();

        long duration = Long.parseLong(mmr.extractMetadata(METADATA_KEY_DURATION));

        for (int i = 1000000; i < duration * 1000; i += 1000000) {
            Bitmap bitmap = mmr.getFrameAtTime(i, OPTION_CLOSEST_SYNC);
            bitmaps.add(bitmap);
        }

        return bitmaps;
    }

    void startProcessing(int frameKey) {
        if (frameKey >= frames.size())
            return;
        Bitmap bmp = frames.get(frameKey);
        poseEstimator.setInputData(poseEstimator.bitmapToPixels(bmp));

        poseEstimator.run();

        poseEstimator.createKeyPoints();

        imageView.setImageBitmap(poseEstimator.generateOutputPhoto());
        imageView.invalidate();
    }

    void init() {
        poseEstimator = new PoseEstimator(this, "hourglass.tflite");
        imageView = findViewById(R.id.imageView);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrev = findViewById(R.id.buttonPrev);

        buttonNext.setOnClickListener(v -> {
            frameKey ++;
            startProcessing(frameKey);
        });
    }

    private static final int READ_REQUEST_CODE = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == READ_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            Uri videoPath = data.getData();
            Log.d(TAG, "onActivityResult: " + videoPath);
            frames = loadFrames(videoPath);
            startProcessing(0);
        }
    }
}

