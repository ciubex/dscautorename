This is an Android application which was made to automatically change the name of images files taken by the camera based on the date and time when the file was created.<br />
The application is available at [Google Play Store - DSC Auto Rename](https://play.google.com/store/apps/details?id=ro.ciubex.dscautorename)<br />
The file rename format use Java date and time format parameters:
  * yyyy for years;
  * MM for month;
  * dd for day;
  * HH for hour;
  * mm for minutes;
  * ss for seconds;
  * SSS for milliseconds.
See more format information on: [Java SimpleDateFormat class](http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html)<br />
The rename service, if is enabled, automatically is triggered when the image file is saved on the device media storage.<br />
The trigger depends on the camera application used to take the pictures. The camera application must send <b>com.android.camera.NEW_PICTURE</b> or <b>android.hardware.action.NEW_PICTURE</b>, otherwise, the rename service is not triggered.