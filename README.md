# BBOXX Stumbler Library
This library is solely for use with the BBOXX Ichnaea instance hosted at location.bboxx.co.uk
It works in the background of Android apps by collecting cell tower and WiFi details when another app accesses the phone's GPS and then sending the details to Ichnaea.

This library is a close derivative of libMozStumbler. More details can be found on the original GitHub repo [wiki] (https://github.com/mozilla/MozStumbler/wiki).

An implementation of this library can be found on the stumbler branch of the BBOXX Sales Agent App, found [here](https://github.com/BBOXX/SalesAgentApp/tree/stumbler).
# Implementation Instructions
The instructions assumes that Android Studio and Gradle are being used for development.
## Importing stumbler library
To add the library to your project, place the stumbler folder inside your libs folder like so:
```
project
├── app
├── build
├── gradle
└── libs
    └── stumbler    
```
Change your settings.gradle to look like this:
```
include ':app'
include ':libs:stumbler'
```
And add the dependency to your app's build.gradle:
```
compile project(':libs:stumbler')
```
Gradle Sync your project to add the library files into Android Studio.
## Adding stumbler code to project
Place the following in your AndroidManifest.xml
```XML
<service
    android:name="org.mozilla.mozstumbler.service.stumblerthread.StumblerService"
    android:label="StumblerService" >
</service>

<receiver android:name="org.mozilla.mozstumbler.service.uploadthread.UploadAlarmReceiver"/>
<service android:name="org.mozilla.mozstumbler.service.uploadthread.UploadAlarmReceiver$UploadAlarmService"/>

<receiver android:name="org.mozilla.mozstumbler.service.mainthread.PassiveServiceReceiver">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.location.GPS_ENABLED_CHANGE" />
        <action android:name="android.location.GPS_FIX_CHANGE" />
    </intent-filter>
</receiver>
```

The code usually goes in your Main Activity, but if you have a splash screen, putting it there works too. As long as the code is called at startup or just after, the service will be started and will continue to run until the app is stopped/force closed.

In your main activity Java code, place the following import statements:
```JAVA
import android.os.Handler;
import org.mozilla.mozstumbler.service.core.http.IHttpUtil;
import org.mozilla.mozstumbler.service.core.http.ILocationService;
import org.mozilla.mozstumbler.service.mainthread.PassiveServiceReceiver;
import org.mozilla.mozstumbler.service.uploadthread.UploadAlarmReceiver;
import org.mozilla.mozstumbler.svclocator.ServiceConfig;
import org.mozilla.mozstumbler.svclocator.ServiceLocator;
import org.mozilla.mozstumbler.svclocator.services.ISystemClock;
import org.mozilla.mozstumbler.svclocator.services.log.ILogger;
```
The following code configures the ServiceLocator and then starts the service with an Intent:
```JAVA
// Setup ServiceConfig and ServiceLocator
ServiceConfig svcConfig = new ServiceConfig();
svcConfig.put(IHttpUtil.class,
        ServiceConfig.load("org.mozilla.mozstumbler.service.core.http.HttpUtil"));
svcConfig.put(ISystemClock.class,
        ServiceConfig.load("org.mozilla.mozstumbler.svclocator.services.SystemClock"));
svcConfig.put(ILocationService.class,
        ServiceConfig.load("org.mozilla.mozstumbler.service.core.http.MLS"));
svcConfig.put(ILogger.class,
        ServiceConfig.load("org.mozilla.mozstumbler.svclocator.services.log.ProductionLogger"));

ServiceLocator.newRoot(svcConfig);
// Create intent with API key "test" and User Agent
Intent i = PassiveServiceReceiver.createStartIntent(this, "test", "Just Another User-Agent");
startService(i);

// Create a scheduled repeating upload alarm, edit time accordingly (here, 2 mins)
long DELAY_MS = 30 * 1000;
new Handler().postDelayed(new Runnable() {
    public void run() {
        // Schedule for upload every 2 mins.
        final int TWO_MINS = 2 * 60;
        UploadAlarmReceiver.scheduleAlarm(SplashScreenActivity.this, TWO_MINS, true /* is repeating */);
    }
}, DELAY_MS);
```
That should be it! You can check to see if the stumbler works by running the project in debug mode (Shift+F9) on an Android device and opening the Android Monitor at the bottom of Android Studio. Observe the output while accessing another app that uses GPS (like Google Maps) and wait for the scheduled upload alarm to call AsyncUploader. An HTTP status code and bytes sent message should come up after a successful report batch submit.
