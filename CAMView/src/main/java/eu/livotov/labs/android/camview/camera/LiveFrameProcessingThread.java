package eu.livotov.labs.android.camview.camera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.CountDownLatch;
import eu.livotov.labs.android.camview.R;

/**
 * Created by dlivotov on 12/05/2015.
 */
public class LiveFrameProcessingThread extends Thread
{
    private final CountDownLatch handlerInitLatch;
    private Handler decoderHandler;
    private Handler uiHandler;
    private LiveDataProcessingCallback processor;

    public LiveFrameProcessingThread(Handler uiHandler, LiveDataProcessingCallback processor)
    {
        handlerInitLatch = new CountDownLatch(1);
        this.uiHandler = uiHandler;
        this.processor = processor;
    }

    @Override
    public void run()
    {
        Looper.prepare();
        decoderHandler = new LiveFrameProcessingHandler(uiHandler, processor);
        handlerInitLatch.countDown();
        Looper.loop();
    }

    public void shutdown()
    {
        final Message message = Message.obtain(getDecoderHandler(), R.id.camview_core_msg_livedataprocess_quit);

        if (message != null)
        {
            message.sendToTarget();
        }
    }

    Handler getDecoderHandler()
    {
        try
        {
            handlerInitLatch.await();
        }
        catch (InterruptedException ignored)
        {
        }

        return decoderHandler;
    }

    public void submitLiveFrame(byte[] bytes, int width, int height)
    {
        getDecoderHandler().removeMessages(R.id.camview_core_msg_livedataprocess_request);
        final Message message = Message.obtain(getDecoderHandler(), R.id.camview_core_msg_livedataprocess_request, width, height, bytes);
        if (message != null)
        {
            message.sendToTarget();
        }
    }
}