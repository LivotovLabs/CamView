package eu.livotov.labs.android.camview.scanner.decoder;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 03/11/2014
 */
public interface BarcodeDecoder
{
    String decode(byte[] image, int width, int height);
}
