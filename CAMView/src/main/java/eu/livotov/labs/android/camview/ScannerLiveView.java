package eu.livotov.labs.android.camview;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Collection;

import eu.livotov.labs.android.camview.camera.CameraController;
import eu.livotov.labs.android.camview.camera.CameraInfo;
import eu.livotov.labs.android.camview.camera.CameraManager;
import eu.livotov.labs.android.camview.camera.LiveDataProcessingCallback;
import eu.livotov.labs.android.camview.scanner.decoder.BarcodeDecoder;
import eu.livotov.labs.android.camview.scanner.decoder.zxing.ZXDecoder;
import eu.livotov.labs.android.camview.scanner.util.SoundPlayer;


/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 03/11/2014
 */
public class ScannerLiveView extends FrameLayout implements LiveDataProcessingCallback, CameraLiveView.CameraLiveViewEventsListener
{
    public final static long DEFAULT_SAMECODE_RESCAN_PROTECTION_TIME_MS = 5000;
    public final static long DEFAULT_DECODE_THROTTLE_MS = 300;

    protected CameraLiveView camera;
    protected ImageView hud;
    protected ScannerViewEventListener scannerViewEventListener;
    protected BarcodeDecoder decoder = new ZXDecoder();
    protected int scannerSoundAudioResource = R.raw.camview_beep;
    protected boolean playSound = true;

    protected SoundPlayer soundPlayer;

    private volatile String lastDataDecoded;
    private volatile long lastDataDecodedTimestamp;
    private volatile long decodeThrottleMillis = DEFAULT_DECODE_THROTTLE_MS;
    private volatile long sameCodeRescanProtectionTime = DEFAULT_SAMECODE_RESCAN_PROTECTION_TIME_MS;
    private CameraController controller;
    private long lastTimeFrameProcessed;

    public ScannerLiveView(final Context context)
    {
        super(context);
        initUI();
    }

    protected void initUI()
    {
        final View root = LayoutInflater.from(getContext()).inflate(getScannerLayoutResource(), this);
        camera = (CameraLiveView) root.findViewById(R.id.camview_camera);
        hud = (ImageView) root.findViewById(R.id.cameraHud);
        decoder = new ZXDecoder();
        soundPlayer = new SoundPlayer(getContext());

        camera.setCameraLiveViewEventsListener(this);
    }

    protected int getScannerLayoutResource()
    {
        return R.layout.camview_view_scanner;
    }

    public ScannerLiveView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        initUI();
    }

    public ScannerLiveView(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initUI();
    }

    public BarcodeDecoder getDecoder()
    {
        return decoder;
    }

    public void setDecoder(BarcodeDecoder decoder)
    {
        this.decoder = decoder;
    }

    @TargetApi(21)
    public ScannerLiveView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        initUI();
    }

    /**
     * Provides the list of all available cameras on this device
     *
     * @return
     */
    public Collection<CameraInfo> getAvailableCameras()
    {
        return CameraManager.getAvailableCameras(getContext());
    }

    /**
     * Starts scanner, using device default camera
     */
    public void startScanner()
    {
        startScanner(null);
    }

    /**
     * Starts scanner, using particular camera. Use {@link #getAvailableCameras()} in order to get a list of all accessible cameras on this device
     *
     * @param camInfo
     */
    public void startScanner(CameraInfo camInfo)
    {
        lastDataDecoded = null;

        CameraInfo finalCamera = camInfo;
        if (finalCamera == null)
        {
            finalCamera = CameraManager.findDefaultCamera(getContext());
        }

        if (finalCamera != null)
        {
            camera.startCamera(finalCamera);
        }
        else
        {
            throw new RuntimeException("Cannot find any camera on device");
        }
    }

    private void resumeGrabbing()
    {
        if (controller != null)
        {
            controller.requestLiveData(this);
        }
    }

    /**
     * Stops currently running scanner
     */
    public void stopScanner()
    {
        camera.stopCamera();
    }

    public long getSameCodeRescanProtectionTime()
    {
        return sameCodeRescanProtectionTime;
    }

    public void setSameCodeRescanProtectionTime(long sameCodeRescanProtectionTime)
    {
        this.sameCodeRescanProtectionTime = sameCodeRescanProtectionTime;
    }

    public long getDecodeThrottleMillis()
    {
        return decodeThrottleMillis;
    }

    public void setDecodeThrottleMillis(long throttle)
    {
        this.decodeThrottleMillis = throttle;
    }

    public CameraLiveView getCamera()
    {
        return camera;
    }

    public ScannerViewEventListener getScannerViewEventListener()
    {
        return scannerViewEventListener;
    }

    public void setScannerViewEventListener(final ScannerViewEventListener scannerViewEventListener)
    {
        this.scannerViewEventListener = scannerViewEventListener;
    }

    public void setScannerSoundAudioResource(final int scannerSoundAudioResource)
    {
        this.scannerSoundAudioResource = scannerSoundAudioResource;
    }

    public boolean isPlaySound()
    {
        return playSound;
    }

    public void setPlaySound(final boolean playSound)
    {
        this.playSound = playSound;
    }

    public void setHudImageResource(int res)
    {
        if (hud != null)
        {
            hud.setBackgroundResource(res);
            setHudVisible(res != 0);
        }
    }

    public void setHudVisible(boolean visible)
    {
        if (hud != null)
        {
            hud.setVisibility(visible ? VISIBLE : INVISIBLE);
        }
    }

    protected void notifyBarcodeRead(final String data)
    {
        beep();

        if (scannerViewEventListener != null)
        {
            if (!TextUtils.isEmpty(data))
            {
                scannerViewEventListener.onCodeScanned(data);
            }
        }
    }

    private void beep()
    {
        if (playSound && scannerSoundAudioResource != 0)
        {
            soundPlayer.playRawResource(scannerSoundAudioResource, false);
        }
    }

    @Override
    public Object onProcessCameraFrame(byte[] data, int width, int height)
    {
        if (System.currentTimeMillis()-lastTimeFrameProcessed > decodeThrottleMillis)
        {
            final Object result = decoder.decode(data, width, height);
            lastTimeFrameProcessed = System.currentTimeMillis();
            return result;
        } else
        {
            return null;
        }
    }

    @Override
    public void onReceiveProcessedCameraFrame(Object object)
    {
        String data = object != null ? object.toString() : null;

        if (!TextUtils.isEmpty(data))
        {
            if (TextUtils.isEmpty(lastDataDecoded) || !lastDataDecoded.equalsIgnoreCase(data) || (lastDataDecoded.equalsIgnoreCase(data) && (System.currentTimeMillis() - lastDataDecodedTimestamp) > sameCodeRescanProtectionTime))
            {
                lastDataDecoded = data;
                lastDataDecodedTimestamp = System.currentTimeMillis();
                notifyBarcodeRead(data);
            }
        }

        resumeGrabbing();
    }

    @Override
    public void onCameraStarted(CameraLiveView camera)
    {
        controller = camera.getController();
        resumeGrabbing();

        if (scannerViewEventListener != null)
        {
            scannerViewEventListener.onScannerStarted(this);
        }
    }

    @Override
    public void onCameraStopped(CameraLiveView camera)
    {
        if (scannerViewEventListener != null)
        {
            scannerViewEventListener.onScannerStopped(this);
        }
    }

    @Override
    public void onCameraError(Throwable err)
    {
        if (scannerViewEventListener != null)
        {
            scannerViewEventListener.onScannerError(err);
        }
    }

    public interface ScannerViewEventListener
    {
        void onScannerStarted(ScannerLiveView scanner);

        void onScannerStopped(ScannerLiveView scanner);

        void onScannerError(Throwable err);

        void onCodeScanned(final String data);
    }


}
