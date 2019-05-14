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

class PoseEstimator {

    private static final int DIM_BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final int TENSOR_INPUT_SIZE_X = 192;
    private static final int TENSOR_INPUT_SIZE_Y = 192;
    private static final int TENSOR_OUTPUT_SIZE_X = 48;
    private static final int TENSOR_OUTPUT_SIZE_Y = 48;
    private static final int PARTS_COUNT = 14;

    private Bitmap inputPhoto;

    private Interpreter tfLite;

    private float[][][][] heatMap = new float[DIM_BATCH_SIZE][TENSOR_OUTPUT_SIZE_Y][TENSOR_OUTPUT_SIZE_X][PARTS_COUNT];

    private float[][][][] inputData;

    private ArrayList<Point> keyPoints = new ArrayList<>();
    private Paint paint = new Paint();


    void setInputData(float[][][][] inputData) {
        this.inputData = inputData;
    }

    PoseEstimator(Activity mainActivity, String model_path) {
        GpuDelegate delegate = new GpuDelegate();
        Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
        try {
            tfLite = new Interpreter(loadModelFile(mainActivity, model_path), options);
        } catch (IOException e) {
            e.printStackTrace();
        }


        String model_path1 = model_path;
        Activity mainActivity1 = mainActivity;
    }

    private MappedByteBuffer loadModelFile(Activity mainActivity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = mainActivity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private Point getPosition(int partID, float[][][][] heatMap) {
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

        paint.setStrokeWidth(scale);

        drawSkeleton(drawCanvas, scale);

        return outputPhoto;
    }

    void drawLine(Canvas drawCanvas, float scale, Point a, Point b) {
        drawCanvas.drawLine((float) a.x * scale, (float) a.y * scale, (float) b.x * scale, (float) b.y * scale, paint);
    }

    void drawSkeleton(Canvas drawCanvas, float scale) {
        for (Point part : keyPoints) {
            drawCanvas.drawCircle(part.x * scale, part.y * scale, scale, paint);
        }
        drawLine(drawCanvas, scale, keyPoints.get(0), keyPoints.get(1));

        for (int i = 2; i < 5; i++)
            drawLine(drawCanvas, scale, keyPoints.get(i - 1), keyPoints.get(i));

        drawLine(drawCanvas, scale, keyPoints.get(2), keyPoints.get(5));
        drawLine(drawCanvas, scale, keyPoints.get(1), keyPoints.get(5));

        for (int i = 6; i < 8; i++)
            drawLine(drawCanvas, scale, keyPoints.get(i - 1), keyPoints.get(i));

        drawLine(drawCanvas, scale, keyPoints.get(2), keyPoints.get(8));
        drawLine(drawCanvas, scale, keyPoints.get(5), keyPoints.get(11));

        for (int i = 9; i < 11; i ++)
            drawLine(drawCanvas, scale, keyPoints.get(i - 1), keyPoints.get(i));
        for (int i = 12; i < 14; i ++)
            drawLine(drawCanvas, scale, keyPoints.get(i - 1), keyPoints.get(i));
    }

    void createKeyPoints() {
        keyPoints.clear();
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
