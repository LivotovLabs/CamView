package eu.livotov.labs.android.camview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.livotov.labs.android.camview.camera.CameraController;
import eu.livotov.labs.android.camview.camera.CameraDelayedOperationResult;
import eu.livotov.labs.android.camview.camera.CameraInfo;
import eu.livotov.labs.android.camview.camera.CameraManager;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 06/12/2013
 */
public class CameraLiveView extends FrameLayout implements SurfaceHolder.Callback
{
    private CameraController camera;
    private SurfaceView surfaceView;
    private AtomicBoolean holderReady = new AtomicBoolean(false);
    private CameraLiveViewEventsListener cameraLiveViewEventsListener;

    public CameraLiveView(Context context)
    {
        super(context);
        initUI();
    }

    public CameraLiveView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initUI();
    }

    public CameraLiveView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initUI();
    }

    @TargetApi(21)
    public CameraLiveView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        initUI();
    }

    public CameraLiveViewEventsListener getCameraLiveViewEventsListener()
    {
        return cameraLiveViewEventsListener;
    }

    public void setCameraLiveViewEventsListener(CameraLiveViewEventsListener cameraLiveViewEventsListener)
    {
        this.cameraLiveViewEventsListener = cameraLiveViewEventsListener;
    }

    private void initUI()
    {
        surfaceView = new SurfaceView(getContext());
        //should be adjusted before surface is added to view
        if (Build.VERSION.SDK_INT < 11)
        {
            adjustSurfaceHolderPre11();
        }
        surfaceView.getHolder().addCallback(this);
        addView(surfaceView);
    }

    @TargetApi(10)
    private void adjustSurfaceHolderPre11()
    {
        try
        {
            // only to address some old devices weird compat issues
            surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        catch (Throwable ignored)
        {
        }
    }

    /**
     * Provides the list of all available cameras on this device
     *
     * @return
     */
    public Collection<CameraInfo> getAvailableCameras()
    {
        return CameraManager.getAvailableCameras(getContext());
    }

    /**
     * Starts scanner, using device default camera
     */
    public void startCamera()
    {
        startCamera(null);
    }

    /**
     * Starts scanner, using particular camera. Use {@link #getAvailableCameras()} in order to get a list of all accessible cameras on this device
     *
     * @param camInfo
     */
    public void startCamera(CameraInfo camInfo)
    {
        CameraInfo finalCamera = camInfo;

        if (finalCamera == null)
        {
            finalCamera = CameraManager.findDefaultCamera(getContext());
        }

        if (finalCamera != null)
        {
            CameraManager.open(finalCamera, new CameraDelayedOperationResult()
            {
                @Override
                public void onOperationCompleted(CameraController controller)
                {
                    try
                    {
                        setCamera(controller);

                        if (cameraLiveViewEventsListener != null)
                        {
                            cameraLiveViewEventsListener.onCameraStarted(CameraLiveView.this);
                        }
                    }
                    catch (IOException e)
                    {
                        Log.e(ScannerLiveView.class.getSimpleName(), e.getMessage(), e);

                        if (cameraLiveViewEventsListener != null)
                        {
                            cameraLiveViewEventsListener.onCameraError(e);
                        }
                    }
                }

                @Override
                public void onOperationFailed(Throwable e, int cameraErrorCode)
                {
                    Log.e(ScannerLiveView.class.getSimpleName(), e != null ? e.getMessage() : "n/a");

                    if (cameraLiveViewEventsListener != null)
                    {
                        cameraLiveViewEventsListener.onCameraError(e != null ? e : new RuntimeException("Camera system error " + cameraErrorCode));
                    }
                }
            });
        }
        else
        {
            throw new RuntimeException("Cannot find any camera on device");
        }
    }

    public void stopCamera()
    {
        try
        {
            if (cameraLiveViewEventsListener != null)
            {
                cameraLiveViewEventsListener.onCameraStopped(this);
            }
            setCamera(null);
        }
        catch (IOException ignored)
        {
        }
    }

    protected void setCamera(CameraController camera) throws IOException
    {
        if (camera == null)
        {
            if (this.camera != null)
            {
                this.camera.close();
                this.camera = null;
            }
        }
        else if (!camera.isReady())
        {
            throw new IllegalArgumentException("Camera is not ready, please provide only initialized camera objects here !");
        }
        else
        {
            this.camera = camera;

            if (holderReady.get())
            {
                camera.startPreview(surfaceView);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        holderReady.set(true);
        if (camera != null)
        {
            try
            {
                camera.startPreview(surfaceView);
            }
            catch (IOException e)
            {
                Log.e(CameraLiveView.class.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (camera != null)
        {
            camera.stopPreview();

            try
            {
                camera.startPreview(surfaceView);
            }
            catch (IOException e)
            {
                Log.e(CameraLiveView.class.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        holderReady.set(false);
    }

    protected void onConfigurationChanged(final Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        if (camera != null)
        {
            restartCameraOnConfigurationChanged();
        }
    }

    private void restartCameraOnConfigurationChanged()
    {
        camera.stopPreview();
        try
        {
            camera.startPreview(surfaceView);
        }
        catch (IOException e)
        {
            Log.e(CameraLiveView.class.getSimpleName(), e.getMessage(), e);
        }
    }

    public boolean resumeDisplay()
    {
        if (camera != null)
        {
            try
            {
                camera.startPreview(surfaceView);
                return true;
            }
            catch (IOException e)
            {
                Log.e(CameraLiveView.class.getSimpleName(), e.getMessage(), e);
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public void pauseDisplay()
    {
        if (camera != null)
        {
            camera.stopPreview();
        }
    }

    public CameraController getController()
    {
        return camera;
    }

    public interface CameraLiveViewEventsListener
    {
        void onCameraStarted(CameraLiveView camera);

        void onCameraStopped(CameraLiveView camera);

        void onCameraError(Throwable err);
    }
}
