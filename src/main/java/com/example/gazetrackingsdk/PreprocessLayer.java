package com.example.gazetrackingsdk;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import android.content.Context;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Executor;

public class PreprocessLayer {
    private final int width = 224;
    private final int height = 224;
    private int count;


    PreprocessLayer() {
        this.count = 0;
    }



    public ByteBuffer Normalise(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[width * height];
        bitmap.getPixels(
                intValues,   // destination array
                0,           // offset in array
                width,       // stride (how many elements per row)
                0, 0,        // x, y start position
                width, height // region size
        );

        int pixel = 0;

        for (int i = 0; i < height; i++) {
            // loops through the matrix to normalise each one
            for (int j = 0; j < width; j++) {
                int val = intValues[pixel];
                pixel = pixel + 1;

                //val = 0x00RRGGBB
                //       ^^^^^^^^
                //       00 = unused (alpha sometimes here)
                //         RR = Red   (bits 16-23)
                //           GG = Green (bits 8-15)
                //             BB = Blue  (bits 0-7)
                // Binary: 00000000 11111111 10001100 00000000
                //                 ^^^^^^^^ ^^^^^^^^ ^^^^^^^^
                //                 Red=255  Green=140 Blue=0
                // 00000000 11111111 10001100 00000000   // original
                //>> 16
                //00000000 00000000 00000000 11111111   // shift right 16 bits
                //& 0xFF (= 00000000 00000000 00000000 11111111)
                //00000000 00000000 00000000 11111111   // = 255 ✅

                // >> n right shift
                // 0xFF -->  zeros out everything except the lowest 8 bits
                // Extract RGB channels
                int r = (val >> 16) & 0xFF;
                int g = (val >> 8) & 0xFF;
                int b = val & 0xFF;

                // Normalize to [0,1]
                byteBuffer.putFloat(r / 255.0f);
                byteBuffer.putFloat(g / 255.0f);
                byteBuffer.putFloat(b / 255.0f);
            }
        }

        return byteBuffer;
    }

    // bitmap its like 00000000 11111111 10001100 00000000 for one pixel
    // bytebuffer its like R=[0,1], G=[0,1], B=[0,1]
    ByteBuffer convertBitmapToByteBuffer(Bitmap img) {
        Bitmap resizedImage = Bitmap.createScaledBitmap(
                img,
                width,
                height,
                true
        );
        return Normalise(resizedImage);
    }


}
