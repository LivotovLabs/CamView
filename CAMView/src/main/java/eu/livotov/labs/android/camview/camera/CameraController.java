package eu.livotov.labs.android.camview.camera;

import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by dlivotov on 02/09/2015.
 */
public interface CameraController
{
    boolean isReady();

    void close();

    void close(CameraDelayedOperationResult callback);

    void startPreview(SurfaceView holder) throws IOException;

    void stopPreview();

    void requestLiveData(LiveDataProcessingCallback callback);

    void takePicture(PictureProcessingCallback callback);

    void switchFlashlight(boolean turnOn);

    void switchAutofocus(boolean useAutofocus);

    void requestFocus();

}
