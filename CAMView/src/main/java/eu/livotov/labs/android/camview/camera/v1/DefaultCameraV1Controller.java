package eu.livotov.labs.android.camview.camera.v1;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.livotov.labs.android.camview.camera.AbstractController;
import eu.livotov.labs.android.camview.camera.CAMViewAsyncTask;
import eu.livotov.labs.android.camview.camera.CameraDelayedOperationResult;
import eu.livotov.labs.android.camview.camera.CameraInfo;
import eu.livotov.labs.android.camview.camera.LiveDataProcessingCallback;
import eu.livotov.labs.android.camview.camera.PictureProcessingCallback;

/**
 * Created by dlivotov on 02/09/2015.
 */
public class DefaultCameraV1Controller extends AbstractController implements Camera.PreviewCallback
{
    private CameraInfo camera;
    private Camera rawCameraObject;
    private AtomicBoolean isOpen = new AtomicBoolean(false);
    private AtomicBoolean isInInitState = new AtomicBoolean(false);
    private byte[] previewBuffer;
    private SurfaceHolder surfaceHolder;
    private int previewFormat = ImageFormat.NV21;

    public DefaultCameraV1Controller(CameraInfo camera, CameraDelayedOperationResult callback)
    {
        this.camera = camera;
        open(callback);
    }

    @Override
    public boolean isReady()
    {
        return isOpen.get();
    }

    @Override
    public void close()
    {
        close(null);
    }

    @Override
    public void close(final CameraDelayedOperationResult callback)
    {
        if (isOpen.get() && isInInitState.compareAndSet(false, true))
        {
            new CAMViewAsyncTask()
            {
                int cameraErrorCode = 0;

                @Override
                protected void onPostExecute(Object o)
                {
                    isOpen.set(false);
                    isInInitState.set(false);
                    rawCameraObject = null;

                    if (callback != null)
                    {
                        callback.onOperationCompleted(DefaultCameraV1Controller.this);
                    }
                }

                @Override
                protected void onError(Throwable t)
                {
                    isInInitState.set(false);

                    if (callback != null)
                    {
                        callback.onOperationFailed(t, cameraErrorCode);
                    }
                }

                @Override
                protected Object doInBackground(Object[] args) throws Throwable
                {
                    if (rawCameraObject != null)
                    {
                        rawCameraObject.setPreviewCallbackWithBuffer(null);
                        rawCameraObject.setErrorCallback(null);
                        rawCameraObject.stopPreview();
                        rawCameraObject.release();
                    }
                    return null;
                }
            }.execSerial();
        }
    }

    @Override
    public void startPreview(SurfaceView surfaceView) throws IOException
    {
        if (isCameraReadyForUserOperations())
        {
            try
            {
                android.hardware.Camera.Parameters parameters = CameraUtilsV1.getMainCameraParameters(rawCameraObject);
                parameters.setPreviewFormat(previewFormat);
                rawCameraObject.setParameters(parameters);
            }
            catch (Throwable err)
            {
                Log.e(getClass().getSimpleName(), "Master parameters set was rejected by a camera, trying failsafe one.", err);

                try
                {
                    android.hardware.Camera.Parameters parameters = CameraUtilsV1.getFailsafeCameraParameters(rawCameraObject);
                    parameters.setPreviewFormat(previewFormat);
                    rawCameraObject.setParameters(parameters);
                }
                catch (Throwable err2)
                {
                    Log.e(getClass().getSimpleName(), "Failsafe parameters set was rejected by a camera, trying to use it as is.", err2);
                }
            }

            if (surfaceHolder == null || surfaceHolder != surfaceView.getHolder())
            {
                surfaceHolder = surfaceView.getHolder();
                rawCameraObject.setPreviewDisplay(surfaceHolder);
            }

            //CameraUtilsV1.computeAspectRatiosForSurface(Integer.parseInt(camera.getCameraId()), rawCameraObject, surfaceView);
            CameraUtilsV1.setupSurfaceAndCameraForPreview(Integer.parseInt(camera.getCameraId()), rawCameraObject, surfaceView);
            rawCameraObject.startPreview();
            rechargePreviewBuffer();
        }
    }

    @Override
    public void stopPreview()
    {
        if (isCameraReadyForUserOperations() && surfaceHolder != null)
        {
            stopLiveDataCapture();
            rawCameraObject.stopPreview();
            surfaceHolder = null;
        }
    }

    @Override
    public void requestLiveData(LiveDataProcessingCallback callback)
    {
        if (isCameraReadyForUserOperations())
        {
            if (surfaceHolder != null && callback != null)
            {
                startLiveDataCapture(callback);
                rechargePreviewBuffer();
            }
            else
            {
                new IllegalStateException("Live data can only be requested after calling startPreview() !");
            }
        }
    }

    private void rechargePreviewBuffer()
    {
        final int imageFormat = rawCameraObject.getParameters().getPreviewFormat();
        final android.hardware.Camera.Size size = rawCameraObject.getParameters().getPreviewSize();

        if (imageFormat != ImageFormat.NV21)
        {
            throw new UnsupportedOperationException(String.format("Bad reported image format, wanted NV21 (%s) but got %s", ImageFormat.NV21, imageFormat));
        }

        int bufferSize = size.width * size.height * ImageFormat.getBitsPerPixel(imageFormat) / 8;

        if (previewBuffer == null || previewBuffer.length != bufferSize)
        {
            previewBuffer = new byte[bufferSize];
        }

        rawCameraObject.addCallbackBuffer(previewBuffer);
        rawCameraObject.setPreviewCallbackWithBuffer(this);
    }

    @Override
    public void takePicture(final PictureProcessingCallback callback)
    {
        if (isCameraReadyForUserOperations())
        {
            rawCameraObject.takePicture(new Camera.ShutterCallback()
            {
                @Override
                public void onShutter()
                {
                    if (callback != null)
                    {
                        callback.onShutterTriggered();
                    }
                }
            }, new Camera.PictureCallback()
            {
                @Override
                public void onPictureTaken(byte[] data, Camera camera)
                {
                    callback.onRawPictureTaken(data);
                }
            }, new Camera.PictureCallback()
            {
                @Override
                public void onPictureTaken(byte[] data, Camera camera)
                {
                    if (callback != null)
                    {
                        callback.onPictureTaken(data);
                    }
                }
            });
        }
    }

    @Override
    public void switchFlashlight(boolean turnOn)
    {
        if (isCameraReadyForUserOperations())
        {
            final android.hardware.Camera.Parameters parameters = rawCameraObject.getParameters();

            if (parameters != null && parameters.getSupportedFlashModes() != null && parameters.getFlashMode() != null && parameters.getSupportedFlashModes().size() > 0)
            {
                if (turnOn)
                {
                    if (!parameters.getFlashMode().equals(android.hardware.Camera.Parameters.FLASH_MODE_TORCH))
                    {
                        parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
                    }
                }
                else
                {
                    if (!parameters.getFlashMode().equals(android.hardware.Camera.Parameters.FLASH_MODE_OFF))
                    {
                        parameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                    }
                }

                rawCameraObject.setParameters(parameters);
            }
        }
        else
        {
            throw new IllegalAccessError("Camera is closed. Cannot toggle flash on non open camera.");
        }
    }

    @Override
    public void switchAutofocus(boolean useAutofocus)
    {
        if (isCameraReadyForUserOperations())
        {

        }
    }

    @Override
    public void requestFocus()
    {
        if (isCameraReadyForUserOperations())
        {

        }
    }

    private void open(final CameraDelayedOperationResult callback)
    {
        if (isOpen.get())
        {
            return;
        }

        if (isInInitState.compareAndSet(false, true))
        {
            new CAMViewAsyncTask()
            {
                private int cameraErrorCode = 0;

                @Override
                protected void onError(Throwable t)
                {
                    isOpen.set(false);
                    isInInitState.set(false);

                    if (callback != null)
                    {
                        callback.onOperationFailed(t, cameraErrorCode);
                    }
                }

                @Override
                protected void onPostExecute(Object o)
                {
                    isOpen.set(true);
                    isInInitState.set(false);

                    if (callback != null)
                    {
                        callback.onOperationCompleted(DefaultCameraV1Controller.this);
                    }
                }

                @Override
                protected Object doInBackground(Object[] args) throws Throwable
                {
                    rawCameraObject = null;
                    int retriesCount = 5;
                    Throwable lastError = null;

                    while (rawCameraObject == null && retriesCount > 0)
                    {
                        try
                        {
                            rawCameraObject = android.hardware.Camera.open(Integer.parseInt(camera.getCameraId()));
                        }
                        catch (Throwable openError)
                        {
                            lastError = openError;
                            retriesCount--;

                            try
                            {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException itre)
                            {
                            }
                        }
                    }

                    if (rawCameraObject == null)
                    {
                        cameraErrorCode = -1;
                        throw lastError;
                    }
                    else
                    {
                        cameraErrorCode = 0;
                    }

                    return null;
                }
            }.execSerial();
        }
    }


    private boolean isCameraReadyForUserOperations()
    {
        return isOpen.get() && !isInInitState.get();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (liveFrameProcessingThread != null && data != null && isCameraReadyForUserOperations())
        {
            final android.hardware.Camera.Size size = rawCameraObject.getParameters().getPreviewSize();
            liveFrameProcessingThread.submitLiveFrame(data, size.width, size.height);
        }
    }
}
