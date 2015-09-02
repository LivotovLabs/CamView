package eu.livotov.labs.android.camview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.livotov.labs.android.camview.camera.CameraController;

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

    public void setCamera(CameraController camera) throws IOException
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
