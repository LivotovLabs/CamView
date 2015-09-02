package eu.livotov.labs.android.camview.camera.v1;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

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

    static void computeAspectRatiosForSurface(int cameraId, Camera camera, SurfaceView surfaceView)
    {
        final SurfaceHolder surfaceHolder = surfaceView.getHolder();

        try
        {
            android.hardware.Camera.Parameters p = camera.getParameters();


            int result = 90;
            int outputResult = 90;


            if (Build.VERSION.SDK_INT > 8)
            {
                int[] results = calculateResults(surfaceView, cameraId, result, outputResult);
                result = results[0];
                outputResult = results[1];
            }

            if (Build.VERSION.SDK_INT > 7)
            {
                try
                {
                    camera.setDisplayOrientation(result);
                }
                catch (Throwable err)
                {
                    // very bad devices goes here
                }
            }

            p.setRotation(outputResult);
            camera.setPreviewDisplay(surfaceHolder);

            android.hardware.Camera.Size closestSize = findClosestPreviewSize(surfaceView, p.getSupportedPreviewSizes());

            if (closestSize != null)
            {
                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                params.width = (surfaceView.getWidth() > surfaceView.getHeight() ? closestSize.width : closestSize.height);
                params.height = (surfaceView.getWidth() > surfaceView.getHeight() ? closestSize.height : closestSize.width);

                if (params.width < surfaceView.getWidth() || params.height < surfaceView.getHeight())
                {
                    final int extraPixels = Math.max(surfaceView.getWidth() - params.width, surfaceView.getHeight() - params.height);
                    params.width += extraPixels;
                    params.height += extraPixels;
                }

                surfaceView.setLayoutParams(params);
                p.setPreviewSize(closestSize.width, closestSize.height);
            }

            camera.setParameters(p);
        }
        catch (Exception e)
        {
            Log.d(CameraUtilsV1.class.getSimpleName(), "Error starting camera preview: " + e.getMessage(), e);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    static int[] calculateResults(View view, int cameraId, int _result, int _outputResult)
    {
        int result = _result;
        int outputResult = _outputResult;

        try
        {
            android.hardware.Camera.CameraInfo info = new Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);

            int rotation = ((WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            int degrees = 0;

            switch (rotation)
            {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
            {
                result = (info.orientation + degrees) % 360;
                outputResult = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            }
            else
            {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
        }
        catch (Throwable err)
        {
            // very bad devices goes here
        }
        return new int[]{result, outputResult};
    }


    static android.hardware.Camera.Size findClosestPreviewSize(View view, List<android.hardware.Camera.Size> sizes)
    {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int i = 0; i < sizes.size(); i++)
        {
            android.hardware.Camera.Size s = sizes.get(i);

            int dx = s.width - view.getWidth();
            int dy = s.height - view.getHeight();

            int score = dx * dx + dy * dy;
            if (score < bestScore)
            {
                best = i;
                bestScore = score;
            }
        }

        return best >= 0 ? sizes.get(best) : null;
    }
}
