package eu.livotov.labs.android.camview;

import android.hardware.Camera;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 09/01/2014
 */
public class CameraEnumeration
{

    private int cameraId;
    private boolean frontCamera;
    private Camera.CameraInfo cameraInfo;

    public CameraEnumeration(final int cameraId, final Camera.CameraInfo cameraInfo)
    {
        this.cameraId = cameraId;
        this.cameraInfo = cameraInfo;
        frontCamera = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public int getCameraId()
    {
        return cameraId;
    }

    public Camera.CameraInfo getCameraInfo()
    {
        return cameraInfo;
    }

    public boolean isFrontCamera()
    {
        return frontCamera;
    }
}
