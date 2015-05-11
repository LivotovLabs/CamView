package eu.livotov.labs.android.camview.test;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import eu.livotov.labs.android.camview.CAMView;


public class MainActivity extends Activity implements CAMView.CAMViewListener
{

    private CAMView camera;
    private boolean flashStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = (CAMView) findViewById(R.id.camview);
        camera.setCamViewListener(this);
    }

    protected void onResume()
    {
        super.onResume();
        camera.start();
    }

    protected void onPause()
    {
        camera.stop();
        super.onPause();
    }

    public void toggleFlash(View view)
    {
        if (camera.isStreaming())
        {
            flashStatus = !flashStatus;
            camera.switchFlash(flashStatus);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraReady(Camera camera)
    {
        Toast.makeText(this, getString(R.string.camera_status_ready), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraError(int i, Camera camera)
    {
        Toast.makeText(this, getString(R.string.camera_status_err, i), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCameraOpenError(Throwable err)
    {
        Toast.makeText(this, getString(R.string.camera_open_err, err.getMessage()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPreviewData(byte[] data, int previewFormat, Camera.Size size)
    {

    }
}
