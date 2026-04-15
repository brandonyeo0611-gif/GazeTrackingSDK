package com.example.gazetrackingsdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

public class CaptureLayer {
    private Context context;
    private int count;
    private final LifecycleOwner  lifecycleOwner;
    // would be the app
    private final FrameListener frameListener;

    CaptureLayer(Context context, LifecycleOwner lifecycleOwner, FrameListener frameListener) {
        this.context = context;
        this.count = 0;
        this.lifecycleOwner = lifecycleOwner;
        this.frameListener = frameListener;
    }

    public void captureImage() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);
        // retrieve camera provider from the context

        cameraProviderFuture.addListener(() -> {
            try {
                // 2. Initialize the provider
                // a cameraprovider allows access to camera
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 3. Set up ImageAnalysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                // stream of image and allows processing on each one

                Executor executor = ContextCompat.getMainExecutor(context);

                imageAnalysis.setAnalyzer(executor, image -> {
                    try {
                        count += 1;
                        Bitmap bitmap = image.toBitmap();
                        // pass to preprocess layer through a listener
                        // get inference

                        // not doing anything currently.
                        // should just pass the bitmap to the preprocess layer --> model --> post --> api?
                        // maybe just return a bytebuffer then the orchestrator layer / API layer will do it
                        // gpt suggest to use a listener.. idk what that need learn
                        File file = new File(context.getCacheDir(), "frame_" + count + ".jpg");
                        // saved in internal cached directory first
                        FileOutputStream outputStream = new FileOutputStream(file);
                        frameListener.onCapture(bitmap);
                        // sends it to orchestrator and orchestrator will save the initial logits

                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e("ImageProcessor", "Save failed", e);
                    } finally {
                        image.close();
                    }
                });

                // 4. Select camera and bind
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll(); // Best practice to unbind before rebinding
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis);
                // need to learn more about lifecycle owner
                // each game is like a lifecycleOwner , settled in the game

            } catch (Exception e) {
                Log.e("ImageProcessor", "Binding failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
}
