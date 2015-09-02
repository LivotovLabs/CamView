package eu.livotov.labs.android.camview.scanner.decoder.zxing;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import eu.livotov.labs.android.camview.scanner.decoder.BarcodeDecoder;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 03/11/2014
 */
public class ZXDecoder implements BarcodeDecoder
{
    protected Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);

    private MultiFormatReader reader;

    public ZXDecoder()
    {
        reader = new MultiFormatReader();

        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        hints.put(DecodeHintType.TRY_HARDER, true);

        reader.setHints(hints);
    }

    public String decode(final byte[] image, final int width, final int height)
    {
        Result result = null;

        try
        {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new PlanarRotatedYUVLuminanceSource(image, width, height, 0, 0, width, height, true)));
            result = reader.decodeWithState(bitmap);
        }
        catch (Throwable re)
        {
            re.printStackTrace();
        }
        finally
        {
            reader.reset();
        }

        if (result != null)
        {
            return result.getText();
        }

        return null;
    }

}
