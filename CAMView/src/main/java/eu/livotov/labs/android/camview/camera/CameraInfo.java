package eu.livotov.labs.android.camview.camera;

/**
 * (c) Livotov Labs Ltd. 2014
 * This is the simple utility object to hold most important device camera parameters in a bit more developer friendly way.
 */
public class CameraInfo
{

    private String cameraId;
    private boolean frontFacingCamera;

    public CameraInfo(final String cameraId, final boolean frontFacingCamera)
    {
        this.cameraId = cameraId;
        this.frontFacingCamera = frontFacingCamera;
    }

    public String getCameraId()
    {
        return cameraId;
    }

    public boolean isFrontFacingCamera()
    {
        return frontFacingCamera;
    }
}