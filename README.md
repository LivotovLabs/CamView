CAMView
=======

 Android component to display live preview from the device camera and optionally provide the preview data for any
 external decoding processes within application.

 The main goal of this project is to have simple and clean component acting just like a view, which can me added to
 any activity, fragment or any other layout of the hosting application and which can internally handle all routines
 for camera initialization and support as well as for orientation changes and picture alignment stuff.


Usage
=====

 Usage is very simple - just add this component to your view hierarchy and invoke the start() method when you need to
 begin receiving live stream from the camera:


 1. Add to layout

 ```
 <?xml version="1.0" encoding="utf-8"?>
 <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
         >

     ...

     <eu.livotov.labs.android.camview.CAMView
             android:id="@+id/scanner_fragment_cameraview"
             android:layout_width="match_parent"
             android:layout_height="match_parent"
             android:layout_gravity="center"/>

     ...

 </FrameLayout>
 ```


 2. Inflate CAMView and register a listener (optionally)

 ```
 cameraView = (CAMView) findViewById(R.id.my_camera_activity_layout);
 cameraView.setCamViewListener(this);
 ```


 3. Begin streaming live picture from camera. You can now rotate you device - camera stream will be adjusted automatically !

 ```
 cameraView.start();
 ```


 3.1 Or if you need to display stream from a specific camera:

 ```
 // Get the list of availabvle cameras
 Collection<CameraEnumeration> allCams = cameraView.enumerateCameras();

 // Start streaming with the specific camera id:
 cameraView.start(allCams.get(somePosition).getCameraId());
 ```


 4. If you need to process the picture from a camera (for instance, for a barcode recognition) - use the listener callbacks

 ```
     public void onPreviewData(final byte[] data, final int previewFormat, final Camera.Size size)
     {
         // do smth with the sample frame from the camera here
     }
 ```


 5. Stop streaming live preview from a camera

 ```
 cameraView.stop();
 ```
