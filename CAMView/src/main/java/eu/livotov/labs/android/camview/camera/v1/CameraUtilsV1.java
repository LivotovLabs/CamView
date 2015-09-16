package eu.livotov.labs.android.camview.camera.v1;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Collection;
import java.util.List;

/**
 * Created by dlivotov on 02/09/2015.
 */
public class CameraUtilsV1
{
    static String findSettableValue(Collection<String> supportedValues, String... desiredValues)
    {
        String result = null;
        if (supportedValues != null)
        {
            for (String desiredValue : desiredValues)
            {
                if (supportedValues.contains(desiredValue))
                {
                    result = desiredValue;
                    break;
                }
            }
        }
        return result;
    }


    static android.hardware.Camera.Parameters getMainCameraParameters(Camera camera)
    {
        android.hardware.Camera.Parameters parameters = camera.getParameters();

        if (Build.VERSION.SDK_INT >= 9)
        {
            setFocusMode(parameters);
        }

        if (Build.VERSION.SDK_INT > 13)
        {
            setAutoExposureLock(parameters);
        }

        if (Build.VERSION.SDK_INT > 7)
        {
            try
            {
                if (parameters.getMaxExposureCompensation() != 0 || parameters.getMinExposureCompensation() != 0)
                {
                    parameters.setExposureCompensation(0);
                }
            }
            catch (Throwable ignored)
            {
            }
        }

        return parameters;
    }

    @TargetApi(14)
    static String getFocusMode14(List<String> focusModes)
    {

        boolean safeMode = false;

        if (safeMode)
        {
            return findSettableValue(focusModes, android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        }
        else
        {
            return findSettableValue(focusModes, android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        }
    }

    @TargetApi(9)
    static String getFocusMode9(List<String> focusModes)
    {
        return findSettableValue(focusModes, android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
    }

    @TargetApi(9)
    static void setFocusMode(android.hardware.Camera.Parameters parameters)
    {
        String focusMode;

        List<String> focusModes = parameters.getSupportedFocusModes();

        if (Build.VERSION.SDK_INT >= 14)
        {
            focusMode = getFocusMode14(focusModes);
        }
        else
        {
            focusMode = getFocusMode9(focusModes);
        }

        if (null == focusMode)
        {
            focusMode = findSettableValue(focusModes, android.hardware.Camera.Parameters.FOCUS_MODE_MACRO, android.hardware.Camera.Parameters.FOCUS_MODE_EDOF);
        }

        if (null != focusMode)
        {
            parameters.setFocusMode(focusMode);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static void setAutoExposureLock(android.hardware.Camera.Parameters parameters)
    {
        try
        {
            if (parameters.isAutoExposureLockSupported())
            {
                parameters.setAutoExposureLock(false);
            }
        }
        catch (Throwable ignored)
        {
        }
    }

    static android.hardware.Camera.Parameters getFailsafeCameraParameters(Camera camera)
    {
        android.hardware.Camera.Parameters parameters = camera.getParameters();

        if (Build.VERSION.SDK_INT >= 9)
        {
            setFocusMode(parameters);
        }
        return parameters;
    }

    static void setupSurfaceAndCameraForPreview(int cameraId, Camera camera, SurfaceView surfaceView)
    {
        ImageParameters mImageParameters = new ImageParameters();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        // Clockwise rotation needed to align the window display to the natural position
        int rotation = ((WindowManager) surfaceView.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation)
        {
            case Surface.ROTATION_0:
            {
                degrees = 0;
                break;
            }
            case Surface.ROTATION_90:
            {
                degrees = 90;
                break;
            }
            case Surface.ROTATION_180:
            {
                degrees = 180;
                break;
            }
            case Surface.ROTATION_270:
            {
                degrees = 270;
                break;
            }
        }

        int displayOrientation;

        // CameraInfo.Orientation is the angle relative to the natural position of the device
        // in clockwise rotation (angle that is rotated clockwise from the natural position)
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            // Orientation is angle of rotation when facing the camera for
            // the camera image to match the natural orientation of the device
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        }
        else
        {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        mImageParameters.mDisplayOrientation = displayOrientation;
        mImageParameters.mLayoutOrientation = degrees;

        camera.setDisplayOrientation(mImageParameters.mDisplayOrientation);


        Camera.Parameters parameters = camera.getParameters();

        Camera.Size sz = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), surfaceView.getWidth(), surfaceView.getHeight());
        parameters.setPreviewSize(sz.width, sz.height);


        //landscape
        float ratio = 0;

        if (surfaceView.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            ratio = (float) sz.width / sz.height;
        }
        else
        {
            ratio = (float) sz.height / sz.width;
        }

        int new_width = 0, new_height = 0;
        if (surfaceView.getWidth() / surfaceView.getHeight() < ratio)
        {
            new_width = Math.round(surfaceView.getHeight() * ratio);
            new_height = surfaceView.getHeight();
        }
        else
        {
            new_width = surfaceView.getWidth();
            new_height = Math.round(surfaceView.getWidth() / ratio);
        }

        FrameLayout.LayoutParams prms = new FrameLayout.LayoutParams(new_width, new_height);
        prms.gravity = Gravity.CENTER;
        surfaceView.setLayoutParams(prms);


        // Set continuous picture focus, if it's supported
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        // Lock in the changes
        camera.setParameters(parameters);
    }

    private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h)
    {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (sizes == null)
        {
            return null;
        }

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes)
        {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
            {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff)
            {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null)
        {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes)
            {
                if (Math.abs(size.height - targetHeight) < minDiff)
                {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}
