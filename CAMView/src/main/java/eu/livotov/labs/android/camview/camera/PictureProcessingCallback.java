package eu.livotov.labs.android.camview.camera;

/**
 * Created by dlivotov on 02/09/2015.
 */
public interface PictureProcessingCallback
{
    void onShutterTriggered();

    void onRawPictureTaken(byte[] rawData);

    void onPictureTaken(byte[] jpegData);
}
