package eu.livotov.labs.android.camview.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import eu.livotov.labs.android.camview.CameraLiveView;
import eu.livotov.labs.android.camview.camera.CameraManager;
import eu.livotov.labs.android.camview.camera.CameraController;
import eu.livotov.labs.android.camview.camera.CameraDelayedOperationResult;
import eu.livotov.labs.android.camview.camera.CameraInfo;


public class MainActivity extends Activity
{

    private CameraLiveView camera;
    private CameraController controller;
    private boolean flashStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera = (CameraLiveView) findViewById(R.id.camview);

        for (CameraInfo cameraInfo : CameraManager.getAvailableCameras(this))
        {
            if (!cameraInfo.isFrontFacingCamera())
            {
                controller = CameraManager.open(cameraInfo, new CameraDelayedOperationResult()
                {
                    @Override
                    public void onOperationCompleted()
                    {
                        try
                        {
                            camera.setCamera(controller);
                        }
                        catch (IOException err)
                        {
                            Toast.makeText(MainActivity.this, err.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onOperationFailed(Throwable exception, int cameraErrorCode)
                    {
                        Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        camera.resumeDisplay();
    }

    @Override
    protected void onPause()
    {
        camera.pauseDisplay();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        controller.close();
        super.onDestroy();
    }

    public void toggleFlash(View view)
    {
        flashStatus = !flashStatus;
        controller.switchFlashlight(flashStatus);
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


}
