package eu.livotov.labs.android.camview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
public class CameraLiveView extends SurfaceView implements SurfaceHolder.Callback
{
    private CameraController camera;
    private AtomicBoolean holderReady = new AtomicBoolean(false);

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

    private void initUI()
    {
        getHolder().addCallback(this);
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
                    }
                    catch (IOException e)
                    {
                        Log.e(ScannerLiveView.class.getSimpleName(), e.getMessage(), e);
                    }
                }

                @Override
                public void onOperationFailed(Throwable e, int cameraErrorCode)
                {
                    Log.e(ScannerLiveView.class.getSimpleName(), e != null ? e.getMessage() : "n/a");
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
            if (this.camera!=null)
            {
                this.camera.close();
                this.camera = null;
            }
        } else if (!camera.isReady())
        {
            throw new IllegalArgumentException("Camera is not ready, please provide only initialized camera objects here !");
        } else
        {
            this.camera = camera;

            if (holderReady.get())
            {
                camera.startPreview(this);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        holderReady.set(true);
        if (camera!=null)
        {
            try
            {
                camera.startPreview(this);
            }
            catch (IOException e)
            {
                Log.e(CameraLiveView.class.getSimpleName(),e.getMessage(),e);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (camera!=null)
        {
            camera.stopPreview();

            try
            {
                camera.startPreview(this);
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

        if (camera!=null)
        {
            restartCameraOnConfigurationChanged();
        }
    }

    private void restartCameraOnConfigurationChanged()
    {
        camera.stopPreview();
        try
        {
            camera.startPreview(this);
        }
        catch (IOException e)
        {
            Log.e(CameraLiveView.class.getSimpleName(),e.getMessage(),e);
        }
    }

    public boolean resumeDisplay()
    {
        if (camera!=null)
        {
            try
            {
                camera.startPreview(this);
                return true;
            }
            catch (IOException e)
            {
                Log.e(CameraLiveView.class.getSimpleName(), e.getMessage(), e);
                return false;
            }
        } else
        {
            return false;
        }
    }

    public void pauseDisplay()
    {
        if (camera!=null)
        {
            camera.stopPreview();
        }
    }

    public CameraController getController()
    {
        return camera;
    }
}
