package eu.livotov.labs.android.camview.camera.v2;

import android.view.SurfaceView;

import eu.livotov.labs.android.camview.camera.CameraController;
import eu.livotov.labs.android.camview.camera.CameraDelayedOperationResult;
import eu.livotov.labs.android.camview.camera.CameraInfo;
import eu.livotov.labs.android.camview.camera.LiveDataProcessingCallback;
import eu.livotov.labs.android.camview.camera.PictureProcessingCallback;

/**
 * Created by dlivotov on 02/09/2015.
 */
public class DefaultCameraV2Controller implements CameraController
{
    public DefaultCameraV2Controller(CameraInfo camera, CameraDelayedOperationResult callback)
    {

    }

    @Override
    public boolean isReady()
    {
        return false;
    }

    @Override
    public void close()
    {

    }

    @Override
    public void close(CameraDelayedOperationResult callback)
    {

    }

    @Override
    public void startPreview(SurfaceView holder)
    {

    }

    @Override
    public void stopPreview()
    {

    }

    @Override
    public void requestLiveData(LiveDataProcessingCallback callback)
    {

    }

    @Override
    public void takePicture(PictureProcessingCallback callback)
    {

    }

    @Override
    public void switchFlashlight(boolean turnOn)
    {

    }

    @Override
    public void switchAutofocus(boolean useAutofocus)
    {

    }

    @Override
    public void requestFocus()
    {

    }
}
