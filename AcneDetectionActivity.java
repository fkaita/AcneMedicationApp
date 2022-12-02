package com.example.acnedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.camerakit.CameraKitView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AcneDetectionActivity extends AppCompatActivity {
    private CameraKitView cameraKitView;
    private Bitmap bitmap;
    private InputImage image;
    private TextView txtMsg;
    int count = 0;
    private Handler handler = new Handler();
    private Runnable runnable;
    private Module mModule = null;
    private ArrayList<Tensor> inputTensors;
    public static Integer outputClsFinal = null;
    public static Integer outputCntFinal = null;
    private Vibrator vibrator;
    private VibrationEffect vibrationEffect;

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acne_detection);

        vibrate();
        txtMsg = findViewById(R.id.txtMsg);
        txtMsg.setText("Look Front.");
        cameraKitView = findViewById(R.id.camera);
        inputTensors = new ArrayList<Tensor>();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(params);
    }

    @Override
    protected void onResume() {
        final Handler handler = new Handler();
        final int delay = 1000; // 1000 milliseconds == 1 second
        handler.postDelayed(new Runnable() {
            public void run() {
                decode();
//                System.out.println("myHandler: here!"); // Do your work here
                handler.postDelayed(this, delay);
            }
        }, delay);
        super.onResume();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
        super.onPause();
    }

    private void decode() {
        cameraKitView.captureImage(new CameraKitView.ImageCallback() {
            @Override
            public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                Bitmap itmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                bitmap = Bitmap.createScaledBitmap(itmap, cameraKitView.getWidth(), cameraKitView.getHeight(), false);
                image = InputImage.fromBitmap(bitmap, 0);
                faceDetection();
            }
        });
    }

    private void faceDetection(){
        FaceDetectorOptions realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .build();

        FaceDetector detector = FaceDetection.getClient(realTimeOpts);

        Task<List<Face>> result = detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        if (faces.size()==0){
//                                            txtMsg.setText("No face found.");
                                            System.out.println("no face detected.");
                                        }
                                        for (Face face : faces) {
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            System.out.println("Y axis value" + rotY);
                                            if (count == 0 && rotY > -10 && rotY < 10){
                                                Tensor inputTensor = preprocess();
                                                inputTensors.add(inputTensor);
                                                count += 1;
                                            } else if (count == 1 && rotY > -10 && rotY < 10){
                                                Tensor inputTensor = preprocess();
                                                inputTensors.add(inputTensor);
                                                count += 1;
                                                vibrate();
                                                txtMsg.setText("Look Left.");
                                            } else if (count == 2 && rotY > 30 && rotY < 70){
                                                Tensor inputTensor = preprocess();
                                                inputTensors.add(inputTensor);
                                                count += 1;
                                            } else if (count == 3 && rotY > 30 && rotY < 70){
                                                Tensor inputTensor = preprocess();
                                                inputTensors.add(inputTensor);
                                                count += 1;
                                                vibrate();
                                                txtMsg.setText("Look Right.");
                                            } else if (count == 4 && rotY > -60 && rotY < -30){
                                                Tensor inputTensor = preprocess();
                                                inputTensors.add(inputTensor);
                                                count += 1;
                                            } else if (count == 5 && rotY > -60 && rotY < -30) {
                                                txtMsg.setText("Analysing...");
                                                Tensor inputTensor = preprocess();
                                                inputTensors.add(inputTensor);
                                                count += 1;
                                                vibrate();
                                                // some action before finish.
                                                try {
                                                    acneDetection();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                finish();
                                            } else {
                                                continue;
                                            }
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.e("Face Detection", "Error detection", e);
                                    }
                                });
    }

    private Tensor preprocess(){
        // for apply MEAN and STD to standardize.
        float[] NO_MEAN_RGB = new float[] {0.45815152f, 0.361242f, 0.29348266f};
        float[] NO_STD_RGB = new float[] {0.2814769f, 0.226306f, 0.20132513f};
        // model input image size
        int mInputWidth = 224;
        int mInputHeight = 224;

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, mInputWidth, mInputHeight, true);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, NO_MEAN_RGB, NO_STD_RGB);
        return inputTensor;
    }

    private void acneDetection() throws IOException {
        try {
            mModule = LiteModuleLoader.load(AcneDetectionActivity.assetFilePath(getApplicationContext(), "trained_cnn_lite_v6.ptl"));
        }catch (IOException e) {
            Log.e("Acne Detection", "Error reading assets", e);
        }
        int[][] outputs = new int[2][inputTensors.size()];
        System.out.println("Size of input tensors: "+inputTensors.size());
        int idx = 0;
        // It's better to concat tensor and calculate once. But I don't know how to concat.
        for (Tensor inputTensor: inputTensors){
            IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
            final Tensor outputClsTensor = outputTuple[0].toTensor();
            final Tensor outputCntTensor = outputTuple[1].toTensor();
            final float[] outputClsFloat = outputClsTensor.getDataAsFloatArray();
            final float[] outputCntFloat = outputCntTensor.getDataAsFloatArray();
            int outputCls = getArgmax(outputClsFloat);
            final int outputCnt = getArgmax(outputCntFloat);
            if (outputCnt < 1){
                outputCls = -1;
            }
            outputs[0][idx] = outputCls;
            outputs[1][idx] = outputCnt;
            idx += 1;
            System.out.println("index: " + idx + ", Class: " + outputCls);
            System.out.println("index: " + idx + ", Counts: " + outputCnt);
        }
        final int frontMax = Math.max(outputs[1][0],outputs[1][1]);
        final int leftMax = Math.max(outputs[1][2],outputs[1][3]);
        final int rightMax = Math.max(outputs[1][4],outputs[1][5]);
        outputCntFinal = Math.max(frontMax, leftMax+rightMax);
        outputClsFinal = -1;
        for (int i=0; i<inputTensors.size(); i++){
            if (outputs[0][i] > outputClsFinal){
                outputClsFinal = outputs[0][i];
            }
        }
        System.out.println("Final Class: " + outputClsFinal);
        System.out.println("Final Counts: " + outputCntFinal);

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String txt_date = dateFormat.format(date);
        OpenHelper helper = new OpenHelper(getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        if(db == null){
            helper.onUpgrade(db, 0, 1);
        }
        insertData(db, txt_date, outputClsFinal, outputCntFinal);
    }

    private void insertData(SQLiteDatabase db, String date, int cls, int cnt) {

        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("class", cls);
        values.put("count", cnt);

        db.insert("acne_db", null, values);
    }

    private int getArgmax(float[] x) {
        float tmp = 0;
        int amx = 0;
        for (int i = 0; i< x.length; i++) {
//            System.out.println(x[i]);
            if (tmp < x[i]){
                tmp = x[i];
                amx = i;
            }
        }
        return amx;
    }

    private void vibrate(){
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.cancel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);
            vibrator.vibrate(vibrationEffect);
        } else{
            vibrator.vibrate(100);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(params);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}