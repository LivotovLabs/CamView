package eu.livotov.labs.android.camview.scanner.decoder;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 03/11/2014
 */
public interface BarcodeDecoder
{
    void setScanAreaPercent(double percent);
    String decode(byte[] image, int width, int height);
}
