package eu.livotov.labs.android.camview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * (c) Livotov Labs Ltd. 2014
 * <p>This is the main view class to be used in layouts or other UI's where the live streaming from device camera is required.</p>
 *
 * <p>Just put this class to a layout or add programmatically at runtime to your activity/fragment, then call start(...);
 * when you need to start streaming live picture and stop(); when you need to stop the streaming.</p>
 *
 * <p>Set your listener via setCamViewListener(...); to receive picture frames for further processing (barcode recognition, etc)</p>
 */
public class CAMView extends FrameLayout implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback
{

    private static final Collection<String> FOCUS_MODES_CALLING_AF;

    static
    {
        FOCUS_MODES_CALLING_AF = new ArrayList<String>(2);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
    }

    private static final long AUTO_FOCUS_INTERVAL_MS = 1000L;
    private SurfaceHolder surfaceHolder;
    private SurfaceView surface;
    private Camera camera;
    private int previewFormat = ImageFormat.NV21;
    private int cameraId = -1;
    private Camera.PreviewCallback previewCallback;
    private Camera.AutoFocusCallback autoFocusCallback;
    private Handler autoFocusHandler;
    private CAMViewListener camViewListener;
    private transient boolean live = false;
    private int lastUsedCameraId = -1;
    private boolean useAutoFocus = true;
    private boolean disableContinuousFocus = false;
    private boolean captureStreamingFrames = false;

    /**
     * We do store here initial state (at the moment of last live streaming start) of preview capture mode. This is used
     * to decide should we trigger camera re-initialization when the <code>setCaptureStreamingFrames</code> method is called
     * during the live process, so user may enable/disable preview capturing at runtime without disturbing the picture view
     */
    private boolean initialCaptureStreamingFramesMode = false;

    public CAMView(final Context context)
    {
        super(context);
        initUI();
    }

    public CAMView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        initUI();
    }

    public CAMView(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        initUI();
    }

    /**
     * Provides the list of all cameras, available on the device
     * @return collection of CameraEnumeration objects, where each object represent the single camera and its main features
     */
    @TargetApi(9)
    public static Collection<CameraEnumeration> enumarateCameras()
    {
        if (Build.VERSION.SDK_INT < 9)
        {
            throw new UnsupportedOperationException("Camera enumeration is only available for Android SDK version 9 and above.");
        }

        List<CameraEnumeration> cameras = new ArrayList<CameraEnumeration>();

        final int camerasCount = Camera.getNumberOfCameras();

        for (int id = 0; id < camerasCount; id++)
        {
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(id, cameraInfo);
            cameras.add(new CameraEnumeration(id, cameraInfo));
        }

        return cameras;
    }

    /**
     * Finds the first front camera on the device.
     * @return First front camera found or null, if device does not have one.
     */
    public static CameraEnumeration findFrontCamera()
    {
        Collection<CameraEnumeration> cams = enumarateCameras();

        for (CameraEnumeration cam : cams)
        {
            if (cam.isFrontCamera())
            {
                return cam;
            }
        }
        return null;
    }

    public boolean isUseAutoFocus()
    {
        return useAutoFocus;
    }

    /**
     * Switches AF mode on or off. When AF is on, camera will try to focus the current scene once live preview is started. If the
     * continuous AF mode is ON (it is ON by default), camera will be trying keep the scene always in focus all the time.
     * @param useAutoFocus
     */
    public void setUseAutoFocus(final boolean useAutoFocus)
    {
        this.useAutoFocus = useAutoFocus;
        restartStreamingIfRunning();
    }

    public boolean isDisableContinuousFocus()
    {
        return disableContinuousFocus;
    }

    /**
     * Disables continuous AF mode. When disabled, camera will focus only once, after the streaming is started by the start(...); method.
     * @param disableContinuousFocus
     */
    public void setDisableContinuousFocus(final boolean disableContinuousFocus)
    {
        this.disableContinuousFocus = disableContinuousFocus;
        restartStreamingIfRunning();
    }

    private void initUI()
    {
        autoFocusHandler = new Handler();
    }

    public boolean isCaptureStreamingFrames()
    {
        return captureStreamingFrames;
    }

    /**
     * <p>Toggles live streaming frames capture. OFF by default. When capturing is ON and CAMView events listener is set via
     * the setCamViewListener(...), CAMView will be constantly capturing and sending bitmap frames to the corresponding callback
     * method for further user processing, such as barcode recognition, etc.</p>
     *
     * <p><b>WARNING: </b>frame capture extensively "eats" memory on the device as it send full frame bitmaps constantly from the camera. If you
     * don't need this feature, do not turn it ON to avoid stressing device memory and GC.</p>
     * @param captureStreamingFrames
     */
    public void setCaptureStreamingFrames(final boolean captureStreamingFrames)
    {
        this.captureStreamingFrames = captureStreamingFrames;

        if (!initialCaptureStreamingFramesMode)
        {
            restartStreamingIfRunning();
        }
    }

    /**
     * Sets the listener to receive streaming events and picture frames from the camera
     * @param camViewListener
     */
    public void setCamViewListener(final CAMViewListener camViewListener)
    {
        this.camViewListener = camViewListener;
    }

    /**
     * Stops the live streaming process if it is currently running. Does nothing, if live streaming was already stopped or not started.
     */
    public synchronized void stop()
    {

        live = false;

        if (camera != null)
        {
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }

        if (surfaceHolder != null)
        {
            try
            {
                surfaceHolder.removeCallback(this);
            } catch (Throwable ignored)
            {
            }
        }
    }

    /**
     * Starts the live streaming using device default camera.
     */
    public void start()
    {
        start(findDefaultCameraId());
    }

    /**
     * Starts the live streaming using provided camera ID.
     * @param cameraId device camera identifier
     */
    public synchronized void start(final int cameraId)
    {
        this.cameraId = cameraId;
        this.camera = setupCamera(cameraId);
        previewCallback = this;
        autoFocusCallback = this;
        initialCaptureStreamingFramesMode = captureStreamingFrames;

        try
        {
            Camera.Parameters parameters = getMainCameraParameters();
            parameters.setPreviewFormat(previewFormat);
            camera.setParameters(parameters);
        } catch (Throwable err)
        {
            Log.e(getClass().getSimpleName(), "Master parameters set was rejected by the camera, trying failsafe one now...", err);

            try
            {
                Camera.Parameters parameters = getFailsafeCameraParameters();
                parameters.setPreviewFormat(previewFormat);
                camera.setParameters(parameters);
            } catch (Throwable err2)
            {
                Log.e(getClass().getSimpleName(), "Failsafe parameters set was rejected by the camera, trying to use camera's default configuration.", err2);
            }
        }

        removeAllViews();
        surface = new SurfaceView(getContext());
        addView(surface);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surface.getLayoutParams();
        params.gravity = Gravity.CENTER;
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        surface.setLayoutParams(params);

        surfaceHolder = surface.getHolder();

        if (captureStreamingFrames)
        {
            surfaceHolder.addCallback(this);
        }

        if (Build.VERSION.SDK_INT < 11)
        {
            try
            {
                // deprecated setting, but required on Android versions prior to 3.0
                surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            } catch (Throwable err)
            {
                Log.e(getClass().getSimpleName(), "Failed to set surface holder to SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS, using it as is.", err);
            }
        }

        lastUsedCameraId = cameraId;
        live = true;
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            camera.setPreviewDisplay(holder);
        } catch (IOException e)
        {
            Log.d("DBG", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (surfaceHolder.getSurface() == null)
        {
            return;
        }

        try
        {
            camera.stopPreview();
        } catch (Exception ignored)
        {
        }

        try
        {
            Camera.Parameters p = camera.getParameters();


            int result = 90;
            int outputResult = 90;


            if (Build.VERSION.SDK_INT > 8)
            {
                int[] results = calculateResults(result, outputResult);
                result = results[0];
                outputResult = results[1];
            }

            if (Build.VERSION.SDK_INT > 7)
            {
                try
                {
                    camera.setDisplayOrientation(result);
                } catch (Throwable err)
                {
                    // very bad devices goes here
                }
            }

            p.setRotation(outputResult);
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(previewCallback);

            Camera.Size closestSize = findClosestPreviewSize(p.getSupportedPreviewSizes());

            if (closestSize != null)
            {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surface.getLayoutParams();
                params.gravity = Gravity.CENTER;
                params.width = (getWidth() > getHeight() ? closestSize.width : closestSize.height);
                params.height = (getWidth() > getHeight() ? closestSize.height : closestSize.width);
                surface.setLayoutParams(params);

                if (params.width < getWidth() || params.height < getHeight())
                {
                    final int extraPixels = Math.max(getWidth() - params.width, getHeight() - params.height);
                    params.width += extraPixels;
                    params.height += extraPixels;
                }

                p.setPreviewSize(closestSize.width, closestSize.height);
            }

            camera.setParameters(p);
            camera.startPreview();
            camera.autoFocus(autoFocusCallback);
        } catch (Exception e)
        {
            Log.d("DBG", "Error starting camera preview: " + e.getMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private int[] calculateResults(int _result, int _outputResult)
    {
        int result = _result;
        int outputResult = _outputResult;

        try
        {
            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);

            int rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            {
                result = (info.orientation + degrees) % 360;
                outputResult = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else
            {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
        } catch (Throwable err)
        {
            // very bad devices goes here
        }
        return new int[]{result, outputResult};
    }

    private Camera.Size findClosestPreviewSize(List<Camera.Size> sizes)
    {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int i = 0; i < sizes.size(); i++)
        {
            Camera.Size s = sizes.get(i);

            int dx = s.width - surface.getWidth();
            int dy = s.height - surface.getHeight();

            int score = dx * dx + dy * dy;
            if (score < bestScore)
            {
                best = i;
                bestScore = score;
            }
        }

        return best >= 0 ? sizes.get(best) : null;
    }

    protected void onConfigurationChanged(final Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        restartStreamingIfRunning();
    }

    private void restartStreamingIfRunning()
    {
        if (live)
        {
            postDelayed(new Runnable()
            {
                public void run()
                {
                    stop();

                    if (lastUsedCameraId >= 0)
                    {
                        start(lastUsedCameraId);
                    } else
                    {
                        start();
                    }
                }
            }, 50);
        }
    }

    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (camViewListener != null && captureStreamingFrames)
        {
            try
            {
                camViewListener.onPreviewData(data, previewFormat, camera.getParameters().getPreviewSize());
            } catch (Throwable ignored)
            {
            }
        }
    }

    public void onAutoFocus(boolean success, final Camera camera)
    {
        if (autoFocusCallback != null)
        {
            autoFocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        camera.autoFocus(autoFocusCallback);
                    } catch (Throwable ignored)
                    {
                    }
                }
            }, AUTO_FOCUS_INTERVAL_MS);
        }
    }

    protected int findDefaultCameraId()
    {
        if (Build.VERSION.SDK_INT < 9)
        {
            return 0;
        } else
        {
            return findCamera();
        }

    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private int findCamera()
    {
        final int camerasCount = Camera.getNumberOfCameras();
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int id = 0; id < camerasCount; id++)
        {
            Camera.getCameraInfo(id, cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                return id;
            }
        }
        if (camerasCount > 0)
        {
            return 0;
        }

        throw new RuntimeException("Did not find any camera on this device");
    }

    protected Camera setupCamera(final int cameraId)
    {
        try
        {
            if (Build.VERSION.SDK_INT < 9)
            {
                return Camera.open();
            } else
            {
                return openCamera(cameraId);
            }
        } catch (Throwable e)
        {
            throw new RuntimeException("Failed to open the camera with id " + cameraId + ": " + e.getMessage(), e);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private Camera openCamera(final int cameraId)
    {
        return Camera.open(cameraId);
    }


    private String findSettableValue(Collection<String> supportedValues, String... desiredValues)
    {
        //Log.i(TAG, "Supported values: " + supportedValues);
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


    private Camera.Parameters getMainCameraParameters()
    {
        Camera.Parameters parameters = camera.getParameters();

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
            } catch (Throwable ignored)
            {
            }
        }

        return parameters;
    }

    @TargetApi(14)
    private String getFocusMode14(List<String> focusModes)
    {
        if (disableContinuousFocus)
        {
            return findSettableValue(focusModes, Camera.Parameters.FOCUS_MODE_AUTO);
        } else
        {
            return findSettableValue(focusModes,
                                     Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                                     Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                                     Camera.Parameters.FOCUS_MODE_AUTO);
        }
    }

    @TargetApi(9)
    private String getFocusMode9(List<String> focusModes)
    {
        return findSettableValue(focusModes, Camera.Parameters.FOCUS_MODE_AUTO);
    }

    @TargetApi(9)
    private void setFocusMode(Camera.Parameters parameters)
    {
        String focusMode;

        List<String> focusModes = parameters.getSupportedFocusModes();

        if (Build.VERSION.SDK_INT >= 14)
        {
            focusMode = getFocusMode14(focusModes);
        } else
        {
            focusMode = getFocusMode9(focusModes);
        }

        if (focusMode == null)
        {
            focusMode = findSettableValue(focusModes,
                                          Camera.Parameters.FOCUS_MODE_MACRO,
                                          Camera.Parameters.FOCUS_MODE_EDOF);
        }

        if (focusMode != null)
        {
            parameters.setFocusMode(focusMode);
            boolean af = useAutoFocus && FOCUS_MODES_CALLING_AF.contains(focusMode);
            autoFocusCallback = af ? this : null;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setAutoExposureLock(Camera.Parameters parameters)
    {
        try
        {
            if (parameters.isAutoExposureLockSupported())
            {
                parameters.setAutoExposureLock(false);
            }
        } catch (Throwable ignored)
        {
        }
    }

    private Camera.Parameters getFailsafeCameraParameters()
    {
        Camera.Parameters parameters = camera.getParameters();

        if (Build.VERSION.SDK_INT >= 9)
        {
            setFocusMode(parameters);
        }
        return parameters;
    }

    /**
     * Tells the current streaming status
     * @return <code>true</code> if the live streaming is currently active
     */
    public boolean isLive()
    {
        return camera != null && live;
    }

    /**
     * Returns the system object of the current camera. This method effective only when live streaming is running. It will
     * return null if not.
     * @return Camera object of the current camera in use.
     */
    public Camera getCamera()
    {
        return camera;
    }

    /**
     * Callback interface to receive preview bitmaps and other events from the CAMView
     */
    public interface CAMViewListener
    {

        /**
         * <p>Called asynchronously in non-ui thread when next preview frame arrives from the camera. Note, that you must enable
         * preview frames capturing by calling <code>setCaptureStreamingFrames(true)</code>.</p>
         *
         * <p>Preview data comes directly from the camera. Please consult with Android documentation on data format and preview
         * format constants</p>
         *
         * @param data preview RAW data
         * @param previewFormat preview format
         * @param size preview size
         */
        void onPreviewData(byte[] data, int previewFormat, Camera.Size size);
    }
}