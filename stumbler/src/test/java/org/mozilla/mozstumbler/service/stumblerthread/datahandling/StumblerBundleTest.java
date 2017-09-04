/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.mozstumbler.service.stumblerthread.datahandling;

import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.mozstumbler.service.stumblerthread.scanners.cellscanner.CellInfo;
import org.mozilla.mozstumbler.service.utils.Zipper;
import org.mozilla.mozstumbler.svclocator.ServiceLocator;
import org.mozilla.mozstumbler.svclocator.services.ISystemClock;
import org.mozilla.mozstumbler.svclocator.services.MockSystemClock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;


@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class StumblerBundleTest {


    // Set the wifi scan time to be 100 milliseconds, but expressed in microseconds
    public static final int WIFI_SCAN_uSec_DELTA = 100 * 1000;

    // Set the time since boot to be 50 milliseconds (but expressed in nanos)
    public static final int BOOT_TIME_NS_DELTA = 50 * 1000 * 1000;

    // Set to ~5:19pm April 6, 2016
    public static final long CURRENT_TIME = 1459977530162L;

    private ISystemClock real_clock;
    @Before
    public void setUp() {
        real_clock = (ISystemClock) ServiceLocator.getInstance().getService(ISystemClock.class);

        MockSystemClock mockClock = new MockSystemClock();
        ServiceLocator.getInstance().putService(ISystemClock.class, mockClock);

        mockClock.setCurrentTime(CURRENT_TIME);
        mockClock.setElapsedRealtime(BOOT_TIME_NS_DELTA);
    }

    @After
    public void tearDown() {
        if (real_clock != null) {
            ServiceLocator.getInstance().putService(ISystemClock.class, real_clock);
        }
    }

    protected JSONObject getExpectedGeoLocate() throws JSONException {
        List<JSONObject> wifiList = new ArrayList<JSONObject>();

        JSONObject wifi = new JSONObject();
        wifi.put("macAddress", "01:23:45:67:89:ab");

        wifiList.add(wifi);
        wifi = new JSONObject();
        wifi.put("macAddress", "23:45:67:89:ab:cd");
        wifiList.add(wifi);
        JSONArray wifiArray = new JSONArray(wifiList);

        JSONObject stumbleBlob = new JSONObject();

        JSONObject fallbackPayload = new JSONObject();
        fallbackPayload.put("lacf", false);
        fallbackPayload.put("ipf", false);
        stumbleBlob.put("fallbacks", fallbackPayload);

        stumbleBlob.put("wifiAccessPoints", wifiArray);

        List<JSONObject> cellTowerList = new ArrayList<JSONObject>();
        JSONObject cellTower = new JSONObject();
        cellTower.put("cellId", 12345);
        cellTower.put("radioType", "lte");
        cellTower.put("locationAreaCode", 2);
        cellTower.put("mobileCountryCode", 208);
        cellTower.put("mobileNetworkCode", 1);
        cellTower.put("signalStrength", -51);
        cellTower.put("timingAdvance", 1);
        cellTower.put("asu", 31);
        cellTowerList.add(cellTower);

        JSONArray cellTowerArray = new JSONArray(cellTowerList);

        stumbleBlob.put(DataStorageConstants.ReportsColumns.CELL, cellTowerArray);

        stumbleBlob.put(DataStorageConstants.ReportsColumns.TIME, 1405602028568L);
        stumbleBlob.put("radioType", "lte");
        stumbleBlob.put(DataStorageConstants.ReportsColumns.LON, -43.5f);
        stumbleBlob.put(DataStorageConstants.ReportsColumns.LAT, -22.5f);

        return stumbleBlob;
    }


    protected JSONObject getExpectedGeosubmit() throws JSONException {
        ISystemClock clock = (ISystemClock) ServiceLocator.getInstance().getService(ISystemClock.class);

        // Set each wifi scan to be WIFI_SCAN_uSec expressed in millisec precision
        long ageMS = WIFI_SCAN_uSec_DELTA/1000;

        List<JSONObject> wifiList = new ArrayList<JSONObject>();

        JSONObject wifi = new JSONObject();
        wifi.put("macAddress", "01:23:45:67:89:ab");
        wifi.put("age", ageMS);
        wifiList.add(wifi);
        wifi = new JSONObject();
        wifi.put("macAddress", "23:45:67:89:ab:cd");
        wifi.put("age", ageMS);
        wifiList.add(wifi);
        JSONArray wifiArray = new JSONArray(wifiList);

        JSONObject stumbleBlob = new JSONObject();
        stumbleBlob.put("wifiAccessPoints", wifiArray);

        JSONObject itemsContainer = new JSONObject();

        List<JSONObject> cellTowerList = new ArrayList<JSONObject>();
        JSONObject cellTower = new JSONObject();
        cellTower.put("cellId", 12345);
        cellTower.put("radioType", "lte");
        cellTower.put("locationAreaCode", 2);
        cellTower.put("mobileCountryCode", 208);
        cellTower.put("mobileNetworkCode", 1);
        cellTower.put("signalStrength", -51);
        cellTower.put("timingAdvance", 1);
        cellTower.put("asu", 31);
        cellTowerList.add(cellTower);

        JSONArray cellTowerArray = new JSONArray(cellTowerList);

        stumbleBlob.put(DataStorageConstants.ReportsColumns.CELL, cellTowerArray);

        // The timestamp of the blob should be the current system time which is what the mock location
        // in geosubmit and geolocate will use for the GPS timestamp
        stumbleBlob.put(DataStorageConstants.ReportsColumns.TIME, clock.currentTimeMillis());
        stumbleBlob.put(DataStorageConstants.ReportsColumns.ALTITUDE, 202.9f);
        stumbleBlob.put(DataStorageConstants.ReportsColumns.LON, -43.5f);
        stumbleBlob.put(DataStorageConstants.ReportsColumns.LAT, -22.5f);
        stumbleBlob.put(DataStorageConstants.ReportsColumns.ACCURACY, 20.5f);
        stumbleBlob.put(DataStorageConstants.ReportsColumns.SPEED, 0.0);
        stumbleBlob.put(DataStorageConstants.ReportsColumns.HEADING, 0.0);

        List<JSONObject> itemsList= new ArrayList<JSONObject>();
        itemsList.add(stumbleBlob);

        JSONArray itemsArray = new JSONArray(itemsList);
        itemsContainer.put("items", itemsArray);
        return itemsContainer;
    }

    @Test
    public void testToGeosubmitJSON() throws JSONException {

        ISystemClock clock = (ISystemClock) ServiceLocator.getInstance().getService(ISystemClock.class);

        JSONObject expectedJson = getExpectedGeosubmit();

        Location mockLocation = new Location(LocationManager.GPS_PROVIDER); // a string
        mockLocation.setLatitude(-22.5f);
        mockLocation.setLongitude(-43.5f);
        mockLocation.setTime(clock.currentTimeMillis());
        mockLocation.setAccuracy(20.5f);
        mockLocation.setAltitude(202.9f);

        assertTrue(mockLocation.hasAccuracy());
        assertTrue(mockLocation.hasAltitude());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        StumblerBundle bundle = new StumblerBundle(mockLocation);

        for (String bssid: new String[]{"01:23:45:67:89:ab", "23:45:67:89:ab:cd"}){
            ScanResult scanResult = createScanResult(bssid, "", 0, 0, 0);
            bundle.addWifiData(bssid, scanResult);
        }

        CellInfo cellInfo = createLteCellInfo(208, 1, 12345, CellInfo.UNKNOWN_CID, 2, 31, 1);

        bundle.addCellData(cellInfo.getCellIdentity(), cellInfo);

        List<StumblerBundle> bundleList = new ArrayList<StumblerBundle>();
        bundleList.add(bundle);

        ReportBatchBuilder rbb = new ReportBatchBuilder();
        for (StumblerBundle b: bundleList) {
            rbb.addRow(b.toMLSGeosubmit());
        }

        String finalReport = Zipper.unzipData(rbb.finalizeToJSONRowsObject().data);
        JSONObject actualJson = new JSONObject(finalReport);


        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }


    @Test
    public void testToGeolocateJSON() throws JSONException {
        JSONObject expectedJson = getExpectedGeoLocate();

        Location mockLocation = new Location(LocationManager.GPS_PROVIDER); // a string
        mockLocation.setLatitude(-22.5f);
        mockLocation.setLongitude(-43.5f);
        mockLocation.setTime(1405602028568L);
        mockLocation.setAccuracy(20.5f);
        mockLocation.setAltitude(202.9f);

        assertTrue(mockLocation.hasAccuracy());
        assertTrue(mockLocation.hasAltitude());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        StumblerBundle bundle = new StumblerBundle(mockLocation);

        for (String bssid: new String[]{"01:23:45:67:89:ab", "23:45:67:89:ab:cd"}){
            ScanResult scanResult = createScanResult(bssid, "", 0, 0, 0);
            bundle.addWifiData(bssid, scanResult);
        }

        CellInfo cellInfo = createLteCellInfo(208, 1, 12345, CellInfo.UNKNOWN_CID, 2, 31, 1);

        bundle.addCellData(cellInfo.getCellIdentity(), cellInfo);

        JSONObject actualJson = bundle.toMLSGeolocate();
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    public static CellInfo createLteCellInfo(int mcc,
                                             int mnc,
                                             int cid,
                                             int psc,
                                             int lac,
                                             int asu,
                                             int ta) {
        CellInfo cell = new CellInfo();
        cell.setLteCellInfo(mcc, mnc, cid, psc, lac, asu, ta);
        cell.setSignalStrength(-51);

        return cell;
    }

    public static ScanResult createScanResult(String BSSID, String caps, int level, int frequency,
                                              long tsf) {
        ISystemClock clock = (ISystemClock) ServiceLocator.getInstance().getService(ISystemClock.class);

        // Set each wifi scan to occur 100,000 nanoseconds after the boot time
        long microSecondTS = WIFI_SCAN_uSec_DELTA;

        Class<?> c = null;
        try {
            c = Class.forName("android.net.wifi.ScanResult");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error loading ScanResult class");
        }
        Constructor[] constructors = c.getConstructors();

        Constructor<?> myConstructor = null;
        for (Constructor<?> construct : constructors) {
            if (construct.getParameterTypes().length == 6) {
                myConstructor = construct;
                break;
            }
        }

        if (myConstructor == null) {
            throw new RuntimeException("No constructor found");
        }
        ScanResult scan = null;
        try {
            scan = (ScanResult) myConstructor.newInstance(null, BSSID, caps, level, frequency, tsf);
            scan.timestamp = microSecondTS;
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
        return scan;
    }


}
