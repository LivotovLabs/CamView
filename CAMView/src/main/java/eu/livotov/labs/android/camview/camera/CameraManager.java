package eu.livotov.labs.android.camview.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.livotov.labs.android.camview.camera.v1.DefaultCameraV1Controller;
import eu.livotov.labs.android.camview.camera.v2.DefaultCameraV2Controller;

/**
 * Created by dlivotov on 02/09/2015.
 */
public class CameraManager
{

    public static Collection<CameraInfo> getAvailableCameras(final Context ctx)
    {
        if (Build.VERSION.SDK_INT>=21)
        {
            return enumerateCamerasViaApiV2(ctx);
        } else
        {
            return enumerateCamerasViaApiV1();
        }
    }

    public static CameraInfo findFrontCamera(Context ctx)
    {
        for (CameraInfo cameraInfo : getAvailableCameras(ctx))
        {
            if (cameraInfo.isFrontFacingCamera())
            {
                return cameraInfo;
            }
        }

        return null;
    }

    public static CameraInfo findDefaultCamera(Context ctx)
    {
        for (CameraInfo cameraInfo : getAvailableCameras(ctx))
        {
            if (!cameraInfo.isFrontFacingCamera())
            {
                return cameraInfo;
            }
        }

        return null;
    }

    public static CameraController open(CameraInfo camera)
    {
        return open(camera, null);
    }

    public static CameraController open(CameraInfo camera, CameraDelayedOperationResult callback)
    {
        if (Build.VERSION.SDK_INT>=21)
        {
//            return openCameraWithApiV2(camera, callback);
            return openCameraWithApiV1(camera, callback);
        } else
        {
            return openCameraWithApiV1(camera, callback);
        }
    }

    @TargetApi(21)
    private static Collection<CameraInfo> enumerateCamerasViaApiV2(Context ctx)
    {
        List<CameraInfo> cameras = new ArrayList<CameraInfo>();
        android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);

        try
        {
            String[] cams = manager.getCameraIdList();
            for (String camId : cams)
            {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(camId);
                cameras.add(new CameraInfo(camId,characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT));
            }
        } catch (CameraAccessException cae)
        {
            Log.e(CameraManager.class.getSimpleName(), cae.getMessage(), cae);
        }

        return cameras;
    }

    @TargetApi(20)
    private static Collection<CameraInfo> enumerateCamerasViaApiV1()
    {
        List<CameraInfo> cameras = new ArrayList<CameraInfo>();

        final int camerasCount = android.hardware.Camera.getNumberOfCameras();

        for (int id = 0; id < camerasCount; id++)
        {
            final android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(id, cameraInfo);
            cameras.add(new CameraInfo("" + id, cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT));
        }

        return cameras;
    }

    @TargetApi(21)
    private static CameraController openCameraWithApiV2(CameraInfo camera, CameraDelayedOperationResult callback)
    {
        return new DefaultCameraV2Controller(camera, callback);
    }

    private static CameraController openCameraWithApiV1(CameraInfo camera, CameraDelayedOperationResult callback)
    {
        return new DefaultCameraV1Controller(camera, callback);
    }


}
