package com.example.semaphoretranslator;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    Interpreter tfLite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        if (OpenCVLoader.initDebug()) {
            Toast.makeText(this, "FUCK YEAHHH", Toast.LENGTH_SHORT).show();
        }
        try {
            tfLite = new Interpreter(loadModelFile());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("pose_estimation.tflite");
        FileInputStream fis = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long length = fileDescriptor.getLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
    }
}
