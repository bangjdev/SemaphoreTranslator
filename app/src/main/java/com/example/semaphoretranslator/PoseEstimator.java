package com.example.semaphoretranslator;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PoseEstimator {

    private static final int DIM_BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final int TENSOR_INPUT_SIZE_X = 192;
    private static final int TENSOR_INPUT_SIZE_Y = 192;
    private static final int TENSOR_OUTPUT_SIZE_X = 48;
    private static final int TENSOR_OUTPUT_SIZE_Y = 48;
    public static final int PARTS_COUNT = 14;

    private static String model_path;

    private static Map<String, Integer> PARTS_ID = new HashMap<>();

    Bitmap inputPhoto;

    Interpreter tfLite;

    float[][][][] heatMap = new float[DIM_BATCH_SIZE][TENSOR_OUTPUT_SIZE_Y][TENSOR_OUTPUT_SIZE_X][PARTS_COUNT];

    float[][][][] inputData;

    ArrayList<Point> keyPoints = new ArrayList<>();
    Paint paint = new Paint();

    Activity mainActivity;


    public void setInputData(float[][][][] inputData) {
        this.inputData = inputData;
    }

    public PoseEstimator(Activity mainActivity, String model_path) {
        GpuDelegate delegate = new GpuDelegate();
        Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
        try {
            tfLite = new Interpreter(loadModelFile(mainActivity, model_path), options);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.model_path = model_path;
        this.mainActivity = mainActivity;
    }

    private MappedByteBuffer loadModelFile(Activity mainActivity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = mainActivity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    Point getPosition(int partID, float[][][][] heatMap) {
        Point res = new Point();
        int maxX = 0, maxY = 0;

        for (int y = 0; y < TENSOR_OUTPUT_SIZE_Y; y++) {
            for (int x = 0; x < TENSOR_OUTPUT_SIZE_X; x++) {
                if (heatMap[0][y][x][partID] > heatMap[0][maxY][maxX][partID]) {
                    maxX = x;
                    maxY = y;
                }
            }
        }

        res.set(maxX, maxY);

        return res;
    }

    float[][][][] bitmapToPixels(Bitmap inputPhoto) {
        this.inputPhoto = inputPhoto;
        inputPhoto = Bitmap.createScaledBitmap(inputPhoto, TENSOR_INPUT_SIZE_X, TENSOR_INPUT_SIZE_Y, false);

        float[][][][] inputData = new float[DIM_BATCH_SIZE][TENSOR_INPUT_SIZE_X][TENSOR_INPUT_SIZE_Y][PIXEL_SIZE];
        int[] pixels = new int[TENSOR_INPUT_SIZE_X * TENSOR_INPUT_SIZE_Y];

        inputPhoto.getPixels(pixels, 0, TENSOR_INPUT_SIZE_X, 0, 0, TENSOR_INPUT_SIZE_X, TENSOR_INPUT_SIZE_Y);

        int pixelsIndex = 0;

        for (int i = 0; i < TENSOR_INPUT_SIZE_X; i++) {
            for (int j = 0; j < TENSOR_INPUT_SIZE_Y; j++) {
                int p = pixels[pixelsIndex];
                inputData[0][i][j][0] = (p >> 16) & 0xff;
                inputData[0][i][j][1] = (p >> 8) & 0xff;
                inputData[0][i][j][2] = (p) & 0xff;
                pixelsIndex++;
            }
        }

        return inputData;
    }

    Bitmap generateOutputPhoto() {
        int outputSize = Math.min(inputPhoto.getWidth(), inputPhoto.getHeight());
        Bitmap outputPhoto = Bitmap.createScaledBitmap(inputPhoto, outputSize, outputSize, false);
        outputPhoto = outputPhoto.copy(Bitmap.Config.ARGB_8888, true);
        Canvas drawCanvas = new Canvas(outputPhoto);
        paint.setColor(Color.RED);

        float scale = outputSize / ((float) TENSOR_OUTPUT_SIZE_X);

        for (Point part : keyPoints) {
            drawCanvas.drawCircle(part.x * scale, part.y * scale, 50, paint);
        }

        return outputPhoto;
    }

    void createKeyPoints() {
        for (int part = 0; part < PARTS_COUNT; part++) {
            keyPoints.add(getPosition(part, heatMap));
        }
    }

    void run() {
        tfLite.run(inputData, heatMap);
    }

    void close() {
        tfLite.close();
    }
}
