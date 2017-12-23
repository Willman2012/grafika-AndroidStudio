/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.grafika;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;

/**
 * More or less straight out of TextureView's doc.
 * <p>
 * TODO: add options for different display sizes, frame rates, camera selection, etc.
 */
public class LiveCameraActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final String TAG = MainActivity.TAG;
    private static final int VIDEO_WIDTH = 320;  // dimensions for 720p video
    private static final int VIDEO_HEIGHT = 240;
    private static final int DESIRED_PREVIEW_FPS = 30;
    private int preview_frame_nums = 0;
    private long last_Timestamp= System.currentTimeMillis();
    private int mCameraPreviewThousandFps;
    private Camera mCamera;
    private TextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        setContentView(mTextureView);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        Log.d(TAG, "numCameras "+numCameras);
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            Log.d(TAG, "Camera.getCameraInfo(i, info) "+i);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        //mCamera = Camera.open();
        if (mCamera == null) {
            // Seeing this on Nexus 7 2012 -- I guess it wants a rear-facing camera, but
            // there isn't one.  TODO: fix
            throw new RuntimeException("Default camera not available");
        }

        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, VIDEO_WIDTH, VIDEO_HEIGHT);
        CameraUtils.choosePictureSize(parms, VIDEO_WIDTH, VIDEO_HEIGHT);
        // Try to set the frame rate to a constant value.
        mCameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(parms, DESIRED_PREVIEW_FPS * 1000);
        parms.setRotation(270);
        mCamera.setParameters(parms);

        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        Camera.Size cameraPictureSize = parms.getPictureSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height +
                " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps"+" PreviewFormat: "+parms.getPreviewFormat();
        String pictureFacts = cameraPictureSize.width + "x" + cameraPictureSize.height +
                " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps"+" PictureFormat: "+parms.getPictureFormat();
        Log.i(TAG, "Camera config previewFacts: " + previewFacts+ " pictureFacts: "+pictureFacts);

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
        preview_frame_nums++;
        if((System.currentTimeMillis()-last_Timestamp)>5000)
        {
            double fps = preview_frame_nums / 5;
            preview_frame_nums = 0;
            last_Timestamp = System.currentTimeMillis();
            Log.d(TAG, "updated onSurfaceTextureUpdated for 5 seconds, fps=" + fps);
        }
        Log.d(TAG, "updated, ts=" + System.currentTimeMillis());
    }
}
