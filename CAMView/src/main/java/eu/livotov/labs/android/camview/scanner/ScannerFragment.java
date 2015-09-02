package eu.livotov.labs.android.camview.scanner;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import eu.livotov.labs.android.camview.R;
import eu.livotov.labs.android.camview.ScannerLiveView;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 03/11/2014
 */
public class ScannerFragment extends Fragment implements ScannerLiveView.ScannerViewEventListener
{
    protected ScannerLiveView scanner;
    protected ScannerLiveView.ScannerViewEventListener scannerViewEventListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.camview_fragment_scanner, container, false);
        return rootView;
    }

    public void onViewCreated(final View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        scanner = (ScannerLiveView) view.findViewById(R.id.scanner);
        scanner.setScannerViewEventListener(this);
    }

    public void onResume()
    {
        super.onResume();

        try
        {
            scanner.startScanner();
        }
        catch (Throwable err)
        {
            onCameraOpenError(err);
        }
    }

    /**
     * In case we're calling startScanner before the previous scanner session is finished (quick activity start-stop-start) or
     * camera is used by another app - startScanner() method may raise a RuntimeException, so we need to handle it here.
     * Override this method to add a custom action. Default behaviour is to show a toast.
     *
     * @param error
     */
    protected void onCameraOpenError(Throwable error)
    {
        Toast.makeText(getActivity(), getString(R.string.camview_string_camerainfragment_error, error.getMessage()), Toast.LENGTH_LONG).show();
    }

    public void onPause()
    {
        try
        {
            scanner.stopScanner();
        }
        catch (Throwable err)
        {
            onCameraCloseError(err);
        }
        super.onPause();
    }

    /**
     * If we're stopping scanner before it was initialized (quick activity start and stop), stopScanner() method may raise a RuntimeException about that,
     * so one need either to handle it or ignore. Override this method to add custom action on error. Default action is ignore.
     *
     * @param error camera close error
     */
    protected void onCameraCloseError(Throwable error)
    {
    }

    public ScannerLiveView.ScannerViewEventListener getScannerViewEventListener()
    {
        return scannerViewEventListener;
    }

    public void setScannerViewEventListener(final ScannerLiveView.ScannerViewEventListener scannerViewEventListener)
    {
        this.scannerViewEventListener = scannerViewEventListener;
    }

    public ScannerLiveView getScanner()
    {
        return scanner;
    }

    public void onCodeScanned(final String data)
    {
        if (scannerViewEventListener != null)
        {
            scannerViewEventListener.onCodeScanned(data);
        }
    }
}
