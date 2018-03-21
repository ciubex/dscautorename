# dscautorename
This is an Android application which was made to automatically change the name of images files taken by the camera based on the date and time when the file was created.

# Installation
The application is available at Google Play Store and F-Droid:

<a href="https://f-droid.org/packages/ro.ciubex.dscautorename/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=ro.ciubex.dscautorename" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

# Licence:
GNU General Public License v3.0 - GNU Project - Free Software Foundation (FSF)
Full licence text: https://github.com/ciubex/dscautorename/blob/master/assets/gpl-3.0-standalone.html

# Date time format on file name
The file rename format use Java date and time format parameters:

yyyy for years;
MM for month;
dd for day;
HH for hour;
mm for minutes;
ss for seconds;
SSS for milliseconds.
See more format information on: Java SimpleDateFormat class
Link: http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html

The rename service, if is enabled, automatically is triggered when the image file is saved on the device media storage.

# Automatically rename
The application CAN rename automaticcaly if the Android os trigger the application.
The trigger depends on the camera application used to take the pictures. The camera application must send com.android.camera.NEW_PICTURE or android.hardware.action.NEW_PICTURE, otherwise, the rename service is not triggered.

Triggers:
- Camera events (deprecated from Android 5.x Lollipop)
- Media content changes
- File changes
- A permanent background service (deprecated)
