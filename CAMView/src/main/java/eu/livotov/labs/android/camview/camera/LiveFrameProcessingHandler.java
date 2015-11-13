package eu.livotov.labs.android.camview.camera;

import android.os.Handler;
import android.os.Message;

import eu.livotov.labs.android.camview.R;

/**
 * Created by dlivotov on 12/05/2015.
 */
public class LiveFrameProcessingHandler extends Handler
{
    private static final String TAG = LiveFrameProcessingHandler.class.getSimpleName();

    private Handler uiHandler;
    private LiveDataProcessingCallback callback;
    private boolean running = true;

    LiveFrameProcessingHandler(Handler uiHandler, LiveDataProcessingCallback callback)
    {
        this.uiHandler = uiHandler;
        this.callback = callback;
    }

    @Override
    public void handleMessage(Message message)
    {
        if (!running)
        {
            return;
        }

        if (message.what == R.id.camview_core_msg_livedataprocess_request)
        {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        }
    }

    private void decode(byte[] data, int width, int height)
    {
        try
        {
            uiHandler.removeMessages(R.id.camview_core_msg_livedataprocess_ok);
            final Object result = callback.onProcessCameraFrame(data, width, height);
            Message.obtain(uiHandler, R.id.camview_core_msg_livedataprocess_ok, result).sendToTarget();
        }
        catch (Throwable err)
        {
        }
    }
}
