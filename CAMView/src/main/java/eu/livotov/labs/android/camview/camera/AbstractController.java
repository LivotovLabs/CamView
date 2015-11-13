package eu.livotov.labs.android.camview.camera;

import android.os.Handler;
import android.os.Message;

import eu.livotov.labs.android.camview.R;

/**
 * Created by dlivotov on 02/09/2015.
 */
public abstract class AbstractController implements CameraController
{
    protected LiveDataProcessingCallback liveDataProcessor;
    protected LiveFrameProcessingThread liveFrameProcessingThread;

    protected void startLiveDataCapture(LiveDataProcessingCallback processor)
    {
        liveDataProcessor = processor;

        if (liveFrameProcessingThread == null)
        {
            liveFrameProcessingThread = new LiveFrameProcessingThread(new ProcessingResultHandler(), processor);
            liveFrameProcessingThread.start();
        }
    }

    protected void stopLiveDataCapture()
    {
        if (liveFrameProcessingThread != null)
        {
            liveFrameProcessingThread.shutdown();
        }
    }

    class ProcessingResultHandler extends Handler
    {

        @Override
        public void handleMessage(Message msg)
        {
            if (msg.what == R.id.camview_core_msg_livedataprocess_ok && liveDataProcessor != null)
            {
                liveDataProcessor.onReceiveProcessedCameraFrame(msg.obj);
            }
        }
    }
}
