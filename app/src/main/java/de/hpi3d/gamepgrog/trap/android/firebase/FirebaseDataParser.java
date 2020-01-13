package de.hpi3d.gamepgrog.trap.android.firebase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.hpi3d.gamepgrog.trap.datatypes.Clue;
import de.hpi3d.gamepgrog.trap.tasks.Task;

public class FirebaseDataParser {

    public static final String CALL_EXECUTE_TASK = "executeTasks";
    public static final String CALL_NEW_CLUE = "newClue";

    private static final String KEY_CALL = "call";
    private static final String KEY_VALUE = "value";

    private static final Type TYPE_TASKS = new TypeToken<ArrayList<Task>>(){}.getType();
    private static final Type TYPE_CLUE = new TypeToken<Clue>(){}.getType();


    public static boolean isValid(Map<String, String> data) {
        return data.containsKey(KEY_CALL)
                && data.containsKey(KEY_VALUE)
                && Arrays.asList(CALL_EXECUTE_TASK, CALL_NEW_CLUE).contains(data.get("call"));
    }

    public static String getCall(Map<String, String> data) {
        return data.get(KEY_CALL);
    }

    public static Clue parseClue(Map<String, String> data) {
        return new Gson().fromJson(data.get("value"), TYPE_CLUE);
    }

    public static ArrayList<Task> parseTasks(Map<String, String> data) {
        return new Gson().fromJson(data.get("value"), TYPE_TASKS);
    }
}
