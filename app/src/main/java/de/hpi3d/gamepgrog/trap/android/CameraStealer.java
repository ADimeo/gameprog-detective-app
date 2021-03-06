package de.hpi3d.gamepgrog.trap.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hpi3d.gamepgrog.trap.datatypes.Image;
import de.hpi3d.gamepgrog.trap.future.Consumer;

public class CameraStealer {

    /**
     * Store the picture request while waiting for {@link Activity#onActivityResult(int, int, Intent)}
     */
    @SuppressLint("UseSparseArrays")
    private static Map<Integer, Consumer<Bitmap>> takePictureCallbacks = new HashMap<>();
    private static int lastPermissionsIndex = 0;

    /**
     * Opens the default camera app which lets the user take an image.
     * @param callback will be called when the image is available
     */
    public static void takeUserImage(Activity c, Consumer<List<Image>> callback) {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(c.getPackageManager()) != null) {
            takePictureCallbacks.put(++lastPermissionsIndex, (picture) ->
                    callback.accept(Collections.singletonList(new Image(picture))));

            c.startActivityForResult(takePicture, lastPermissionsIndex);
        }
    }

    /**
     * Is called by {@link Activity#onActivityResult(int, int, Intent)} when the image is returned
     */
    public static void onResult(int requestCode, int resultCode, Intent data) {
        if (takePictureCallbacks.containsKey(requestCode)) {
            if (resultCode == Activity.RESULT_OK) {
                Bitmap picture = (Bitmap) data.getExtras().get("data");
                takePictureCallbacks.get(requestCode).accept(picture);
            }
        }
    }
}
