package de.hpi3d.gamepgrog.trap.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.hpi3d.gamepgrog.trap.DataStealer;
import de.hpi3d.gamepgrog.trap.R;
import de.hpi3d.gamepgrog.trap.api.BackendManagerIntentService;
import de.hpi3d.gamepgrog.trap.api.UserDataPostRequestFactory;
import de.hpi3d.gamepgrog.trap.datatypes.CalendarEvent;
import de.hpi3d.gamepgrog.trap.datatypes.Contact;
import de.hpi3d.gamepgrog.trap.datatypes.LocationData;
import de.hpi3d.gamepgrog.trap.gamelogic.IApp;
import de.hpi3d.gamepgrog.trap.gamelogic.NoPermissionsException;
import de.hpi3d.gamepgrog.trap.gamelogic.StoryController;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements IApp {


    private static final int PERMISSION_REQUEST_IDENTIFIER_READ_CONTACTS = 101;
    private static final int PERMISSION_REQUEST_IDENTIFIER_READ_CALENDAR = 102;
    private static final int PERMISSION_REQUEST_IDENTIFIER_READ_LOCATION = 103;

    private StoryController story;

    private Map<Integer, Consumer<Boolean>> permissionCallbacks = new HashMap<>();
    private int lastPermissionsIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        story = new StoryController(this);

        boolean isInSafetyMode = BackendManagerIntentService.isInSafetyMode(getApplicationContext());

        int playerId = BackendManagerIntentService.getPlayerId(this);
        if (-1 == playerId && !isInSafetyMode) {
            Intent registerPlayer = new Intent(this, BackendManagerIntentService.class);
            registerPlayer.putExtra(BackendManagerIntentService.KEY_MANAGE_TYPE, BackendManagerIntentService.MANAGE_PLAYER_REGISTRATION);
            startService(registerPlayer);
        }
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        story.doStoryActionIfNeeded();
        sendClueDownloadIntent();
    }

    private void sendClueDownloadIntent() {
        startNewBackendIntent(BackendManagerIntentService.MANAGE_CLUE_DOWNLOAD);
    }

    private boolean startNewBackendIntent(String type, BiConsumer<Integer, Bundle> receiver) {
        Intent intent = createNewBackendIntent(type, receiver);
        if (intent != null) {
            startService(intent);
            return true;
        }
        return false;
    }

    private boolean startNewBackendIntent(String type) {
        return startNewBackendIntent(type, (code, bundle) -> {});
    }

    private Intent createNewBackendIntent(String type, BiConsumer<Integer, Bundle> receiver) {
        Intent intent = createNewBackendIntent(type);
        if (intent != null)
            intent.putExtra("receiver", new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    receiver.accept(resultCode, resultData);
                }
            });
        return intent;
    }

    private Intent createNewBackendIntent(String type) {
        Intent intent = createNewBackendIntent();
        if (intent != null)
            intent.putExtra(BackendManagerIntentService.KEY_MANAGE_TYPE, type);
        return intent;
    }

    private Intent createNewBackendIntent() {
        boolean safetyMode = BackendManagerIntentService.isInSafetyMode(this);
        int playerId = BackendManagerIntentService.getPlayerId(this);

        if (playerId != -1 && !safetyMode) {
            return new Intent(this, BackendManagerIntentService.class);
        } else {
            Toast.makeText(this,
                    R.string.error_no_registration_or_safety_mode, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        boolean isGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionCallbacks.containsKey(requestCode)) {
            permissionCallbacks.get(requestCode).accept(isGranted);
            permissionCallbacks.remove(requestCode);
        }
    }

    private int getUserId() {
        return BackendManagerIntentService.getPlayerId(this);
    }

    @Override
    public void setPermission(String permission, Consumer<Boolean> callback) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{permission},
                    ++lastPermissionsIndex);
            permissionCallbacks.put(lastPermissionsIndex, callback);
        } else {
            callback.accept(true);
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public List<CalendarEvent> getCalendarEvents() throws NoPermissionsException {
        try {
            return DataStealer.takeCalendarData(getApplicationContext());
        } catch (SecurityException e) {
            throw new NoPermissionsException();
        }
    }

    @Override
    public List<Contact> getContacts() throws NoPermissionsException {
        try {

            return DataStealer.takeContactData(getApplicationContext());
        } catch (SecurityException e) {
            throw new NoPermissionsException();
        }

    }

    @Override
    public List<LocationData> getLocation() throws NoPermissionsException {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        DataStealer locationStealer = new DataStealer(client);
        locationStealer.getContinuousLocationUpdates(getApplicationContext());

        return null;
    }

    @Override
    public void executeApiCall(String call, BiConsumer<Integer, Bundle> callback) {
        startNewBackendIntent(call, callback);
    }

    @Override
    public void postUserData(String call, UserDataPostRequestFactory.UserDataPostRequest pr, Runnable callback) {
        Intent intent = createNewBackendIntent(call, (code, bundle) -> callback.run());
        intent.putExtra("postRequest", pr);
        startService(intent);
    }

    @Override
    public String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    private void getContinuousLocationUpdates() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        DataStealer locationStealer = new DataStealer(client);
        locationStealer.getContinuousLocationUpdates(getApplicationContext());

    }

   /* Needs higher API level. How to refactor?

    private void sendLocationData(List<Location> locations) {
        int userid = getUserId();
        List<LocationData> locationsData = locations
                .stream()
                .map(LocationData::fromLocation)
                .collect(Collectors.toList());
        server.addData(userid, UserDataPostRequestFactory.buildWithLocations(locationsData))
                .subscribe();
    }*/
}
