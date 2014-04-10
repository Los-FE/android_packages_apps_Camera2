/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera.app;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceHolder;

/**
 * An interface which provides possible camera device operations.
 *
 * The client should call {@code CameraManager.cameraOpen} to get an instance
 * of {@link CameraManager.CameraProxy} to control the camera. Classes
 * implementing this interface should have its own one unique {@code Thread}
 * other than the main thread for camera operations. Camera device callbacks
 * are wrapped since the client should not deal with
 * {@code android.hardware.Camera} directly.
 *
 * TODO: provide callback interfaces for:
 * {@code android.hardware.Camera.ErrorCallback},
 * {@code android.hardware.Camera.OnZoomChangeListener}, and
 * {@code android.hardware.Camera.Parameters}.
 */
public interface CameraManager {

    /**
     * A handler for all camera api runtime exceptions.
     * The default behavior is to throw the runtime exception.
     */
    public interface CameraExceptionCallback {
        public void onCameraException(RuntimeException e);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.ErrorCallback}
     */
    public interface CameraErrorCallback {
        public void onError(int error, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusCallback}.
     */
    public interface CameraAFCallback {
        public void onAutoFocus(boolean focused, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusMoveCallback}.
     */
    public interface CameraAFMoveCallback {
        public void onAutoFocusMoving(boolean moving, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.ShutterCallback}.
     */
    public interface CameraShutterCallback {
        public void onShutter(CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.PictureCallback}.
     */
    public interface CameraPictureCallback {
        public void onPictureTaken(byte[] data, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.PreviewCallback}.
     */
    public interface CameraPreviewDataCallback {
        public void onPreviewFrame(byte[] data, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.FaceDetectionListener}.
     */
    public interface CameraFaceDetectionCallback {
        /**
         * Callback for face detection.
         *
         * @param faces   Recognized face in the preview.
         * @param camera  The camera which the preview image comes from.
         */
        public void onFaceDetection(Camera.Face[] faces, CameraProxy camera);
    }

    /**
     * An interface to be called for any exception caught when opening the
     * camera device. This error callback is different from the one defined
     * in the framework, {@link android.hardware.Camera.ErrorCallback}, which
     * is used after the camera is opened.
     */
    public interface CameraOpenCallback {
        /**
         * Callback when camera open succeeds.
         */
        public void onCameraOpened(CameraProxy camera);

        /**
         * Callback when {@link com.android.camera.CameraDisabledException} is
         * caught.
         *
         * @param cameraId The disabled camera.
         */
        public void onCameraDisabled(int cameraId);

        /**
         * Callback when {@link com.android.camera.CameraHardwareException} is
         * caught.
         *
         * @param cameraId The camera with the hardware failure.
         */
        public void onDeviceOpenFailure(int cameraId);

        /**
         * Callback when trying to open the camera which is already opened.
         *
         * @param cameraId The camera which is causing the open error.
         */
        public void onDeviceOpenedAlready(int cameraId);

        /**
         * Callback when {@link java.io.IOException} is caught during
         * {@link android.hardware.Camera#reconnect()}.
         *
         * @param mgr The {@link CameraManager}
         *            with the reconnect failure.
         */
        public void onReconnectionFailure(CameraManager mgr);
    }

    /**
     * Opens the camera of the specified ID asynchronously. The camera device
     * will be opened in the camera handler thread and will be returned through
     * the {@link CameraManager.CameraOpenCallback#
     * onCameraOpened(com.android.camera.app.CameraManager.CameraProxy)}.
     *
     * @param handler The {@link android.os.Handler} in which the callback
     *                was handled.
     * @param callback The callback when any error happens.
     * @param cameraId The camera ID to open.
     */
    public void cameraOpen(Handler handler, int cameraId, CameraOpenCallback callback);

    /**
     * An interface that takes camera operation requests and post messages to the
     * camera handler thread. All camera operations made through this interface is
     * asynchronous by default except those mentioned specifically.
     */
    public interface CameraProxy {

        /**
         * Returns the underlying {@link android.hardware.Camera} object used
         * by this proxy. This method should only be used when handing the
         * camera device over to {@link android.media.MediaRecorder} for
         * recording.
         */
        @Deprecated
        public android.hardware.Camera getCamera();

        /**
         * Returns the camera ID associated to by this
         * {@link CameraManager.CameraProxy}.
         * @return
         */
        public int getCameraId();

        /**
         * Releases the camera device synchronously.
         * This function must be synchronous so the caller knows exactly when the camera
         * is released and can continue on.
         * TODO: make this package-private after this interface is refactored under app.
         *
         * @param synchronous Whether this call should be synchronous.
         */
        public void release(boolean synchronous);

        /**
         * Reconnects to the camera device. On success, the camera device will
         * be returned through {@link CameraManager
         * .CameraOpenCallback#onCameraOpened(com.android.camera.app.CameraManager
         * .CameraProxy)}.
         * @see android.hardware.Camera#reconnect()
         *
         * @param handler The {@link android.os.Handler} in which the callback
         *                was handled.
         * @param cb The callback when any error happens.
         */
        public void reconnect(Handler handler, CameraOpenCallback cb);

        /**
         * Unlocks the camera device.
         *
         * @see android.hardware.Camera#unlock()
         */
        public void unlock();

        /**
         * Locks the camera device.
         * @see android.hardware.Camera#lock()
         */
        public void lock();

        /**
         * Sets the {@link android.graphics.SurfaceTexture} for preview.
         *
         * @param surfaceTexture The {@link SurfaceTexture} for preview.
         */
        public void setPreviewTexture(final SurfaceTexture surfaceTexture);

        /**
         * Blocks until a {@link android.graphics.SurfaceTexture} has been set
         * for preview.
         *
         * @param surfaceTexture The {@link SurfaceTexture} for preview.
         */
        public void setPreviewTextureSync(final SurfaceTexture surfaceTexture);

        /**
         * Sets the {@link android.view.SurfaceHolder} for preview.
         *
         * @param surfaceHolder The {@link SurfaceHolder} for preview.
         */
        public void setPreviewDisplay(final SurfaceHolder surfaceHolder);

        /**
         * Starts the camera preview.
         */
        public void startPreview();

        /**
         * Starts the camera preview and executes a callback on a handler once
         * the preview starts.
         */
        public void startPreviewWithCallback(Handler h, CameraStartPreviewCallback cb);

        /**
         * Stops the camera preview synchronously.
         * {@code stopPreview()} must be synchronous to ensure that the caller can
         * continues to release resources related to camera preview.
         */
        public void stopPreview();

        /**
         * Sets the callback for preview data.
         *
         * @param handler    The {@link android.os.Handler} in which the callback was handled.
         * @param cb         The callback to be invoked when the preview data is available.
         * @see  android.hardware.Camera#setPreviewCallback(android.hardware.Camera.PreviewCallback)
         */
        public void setPreviewDataCallback(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Sets the one-time callback for preview data.
         *
         * @param handler    The {@link android.os.Handler} in which the callback was handled.
         * @param cb         The callback to be invoked when the preview data for
         *                   next frame is available.
         * @see  android.hardware.Camera#setPreviewCallback(android.hardware.Camera.PreviewCallback)
         */
        public void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Sets the callback for preview data.
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The callback to be invoked when the preview data is available.
         * @see android.hardware.Camera#setPreviewCallbackWithBuffer(android.hardware.Camera.PreviewCallback)
         */
        public void setPreviewDataCallbackWithBuffer(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Adds buffer for the preview callback.
         *
         * @param callbackBuffer The buffer allocated for the preview data.
         */
        public void addCallbackBuffer(byte[] callbackBuffer);

        /**
         * Starts the auto-focus process. The result will be returned through the callback.
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The auto-focus callback.
         */
        public void autoFocus(Handler handler, CameraAFCallback cb);

        /**
         * Cancels the auto-focus process.
         */
        public void cancelAutoFocus();

        /**
         * Sets the auto-focus callback
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The callback to be invoked when the preview data is available.
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void setAutoFocusMoveCallback(Handler handler, CameraAFMoveCallback cb);

        /**
         * Instrument the camera to take a picture.
         *
         * @param handler   The handler in which the callback will be invoked.
         * @param shutter   The callback for shutter action, may be null.
         * @param raw       The callback for uncompressed data, may be null.
         * @param postview  The callback for postview image data, may be null.
         * @param jpeg      The callback for jpeg image data, may be null.
         * @see android.hardware.Camera#takePicture(
         *         android.hardware.Camera.ShutterCallback,
         *         android.hardware.Camera.PictureCallback,
         *         android.hardware.Camera.PictureCallback)
         */
        public void takePicture(
                Handler handler,
                CameraShutterCallback shutter,
                CameraPictureCallback raw,
                CameraPictureCallback postview,
                CameraPictureCallback jpeg);

        /**
         * Sets the display orientation for camera to adjust the preview orientation.
         *
         * @param degrees The rotation in degrees. Should be 0, 90, 180 or 270.
         */
        public void setDisplayOrientation(int degrees);

        /**
         * Sets the listener for zoom change.
         *
         * @param listener The listener.
         */
        public void setZoomChangeListener(OnZoomChangeListener listener);

        /**
         * Sets the face detection listener.
         *
         * @param handler  The handler in which the callback will be invoked.
         * @param callback The callback for face detection results.
         */
        public void setFaceDetectionCallback(Handler handler, CameraFaceDetectionCallback callback);

        /**
         * Starts the face detection.
         */
        public void startFaceDetection();

        /**
         * Stops the face detection.
         */
        public void stopFaceDetection();

        /**
         * Registers an error callback.
         *
         * @param handler  The handler on which the callback will be invoked.
         * @param cb The error callback.
         * @see android.hardware.Camera#setErrorCallback(android.hardware.Camera.ErrorCallback)
         */
        public void setErrorCallback(Handler handler, CameraErrorCallback cb);

        /**
         * Sets the camera parameters.
         *
         * @param params The camera parameters to use.
         */
        public void setParameters(Parameters params);

        /**
         * Gets the current camera parameters synchronously. This method is
         * synchronous since the caller has to wait for the camera to return
         * the parameters. If the parameters are already cached, it returns
         * immediately.
         */
        public Parameters getParameters();

        /**
         * Forces {@code CameraProxy} to update the cached version of the camera
         * parameters regardless of the dirty bit.
         */
        public void refreshParameters();

        /**
         * Enables/Disables the camera shutter sound.
         *
         * @param enable   {@code true} to enable the shutter sound,
         *                 {@code false} to disable it.
         */
        public void enableShutterSound(boolean enable);
    }

    /**
     * An interface to be called when the camera preview has started.
     */
    public interface CameraStartPreviewCallback {
        /**
         * Callback when the preview starts.
         */
        public void onPreviewStarted();
    }

    /**
     * Sets a callback for handling camera api runtime exceptions on
     * a handler.
     */
    public void setCameraDefaultExceptionCallback(CameraExceptionCallback callback,
            Handler handler);
}