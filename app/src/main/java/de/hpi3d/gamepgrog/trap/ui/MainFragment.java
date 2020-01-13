package de.hpi3d.gamepgrog.trap.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import androidx.fragment.app.Fragment;
import de.hpi3d.gamepgrog.trap.R;
import de.hpi3d.gamepgrog.trap.api.StorageManager;


public class MainFragment extends Fragment {

    private static final String TAG = "MAIN_FRAGMENT";

    private Button upButton;


    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        upButton = view.findViewById(R.id.button_temporary_telegram);
        upButton.setOnClickListener((View v) -> {
            sendInitialTelegramMessage();
        });


        Button debugUploadContacts = view.findViewById(R.id.button_debug_contacts);

        Switch safetySwitch = view.findViewById(R.id.switch_safety);
        safetySwitch.setChecked(StorageManager.isInSafetyMode(getContext()));

        safetySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            StorageManager.setSafetyMode(isChecked, getContext());
        });

        return view;


    }


    @Override
    public void onResume() {
        super.onResume();
        // TODO use firebase and remove API call
        //  FragmentActivity activity = getActivity();
        //  Intent testButtonStatus = new Intent(activity, StorageManager.class);
        //  testButtonStatus.putExtra(StorageManager.KEY_MANAGE_TYPE, StorageManager.MANAGE_TELEGRAM_BUTTON_STATUS);

        // activity.startService(testButtonStatus);

        boolean playerHasStartedConversation = StorageManager.getHasPlayerStartedConversation(getContext());
        Log.d(TAG, "has player started conversation: " + playerHasStartedConversation);
        upButton.setEnabled(!playerHasStartedConversation);
    }

    private void sendInitialTelegramMessage() {
        String botUrl = StorageManager.getBotUrl(getContext());
        Log.d(TAG, "Sending Telegram Message with url: " + botUrl);
        try {
            Intent telegram = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + botUrl));
            startActivity(telegram);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
