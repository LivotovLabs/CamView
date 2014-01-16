CAMView
=======

 Android component to display live preview picture from the device camera and optionally provide the preview data 
 for any external decoding processes within application.

 The main goal of this project is to have a simple and clean component, which can be simply added to an existing
 view hierarchy of any existing activity, fragment or other layout of the hosting application and which will internally    handle all dirty routines for a camera initialization, configuration, streaming and orientation changes.


Status
======

 The component is now fully functional and is around beta release. 
 Please feel free to share your comments and suggestions, report any bugs or submit your pull requests (evem better :)


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
             android:id="@+id/my_camera_view"
             android:layout_width="match_parent"
             android:layout_height="match_parent"
             android:layout_gravity="center"/>

     ...

 </FrameLayout>
 ```


 2. Get CAMView instance and register a listener (optionally, if you nedd to process live stream frames only)

 ```
 cameraView = (CAMView) findViewById(R.id.my_camera_view);
 cameraView.setCamViewListener(this);
 ```


 3. Begin streaming live picture from the camera.

 ```
 cameraView.start();
 ```


 3.1 Or, if you need to display stream from the specific camera:

 ```
 // Get the list of availabvle cameras
 Collection<CameraEnumeration> allCams = cameraView.enumerateCameras();

 // Start streaming with the specific camera id:
 cameraView.start(allCams.get(somePosition).getCameraId());
 ```


 4. If you need to process live data from the camera (for instance, for a barcode recognition) - use the listener callback:

 ```
     public void onPreviewData(final byte[] data, final int previewFormat, final Camera.Size size)
     {
         // do smth with the frame from the camera here
     }
 ```


 5. Stop streaming from the camera

 ```
 cameraView.stop();
 ```
