package eu.livotov.labs.android.camview.camera;

/**
 * Created by dlivotov on 02/09/2015.
 */
public interface CameraDelayedOperationResult
{
    void onOperationCompleted(CameraController controller);

    void onOperationFailed(Throwable exception, int cameraErrorCode);
}
