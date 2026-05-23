package com.example.gazetrackingsdk;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public interface FrameListener {
    public void onCapture(Bitmap bitmap);
}
