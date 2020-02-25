package de.hpi3d.gamepgrog.trap.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import de.hpi3d.gamepgrog.trap.R;
import de.hpi3d.gamepgrog.trap.datatypes.Displayable;
import de.hpi3d.gamepgrog.trap.tasks.Task;


public class DisplayableRecyclerViewAdapter extends RecyclerView.Adapter<DisplayableRecyclerViewAdapter.ViewHolder> {

    private final List<Displayable> displayableList;
    private Activity activity = null;


    public DisplayableRecyclerViewAdapter(List<Displayable> items) {
        displayableList = items;
    }

    public DisplayableRecyclerViewAdapter(List<Displayable> items, Activity activity) {
        displayableList = items;
        this.activity = activity;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_displayable, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.displayable = displayableList.get(position);
        holder.displayableTextView.setText(displayableList.get(position).getDisplayString());
    }

    @Override
    public int getItemCount() {
        return displayableList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView displayableTextView;
        public Displayable displayable;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            displayableTextView = (TextView) view.findViewById(R.id.content);
            displayableTextView.setOnClickListener(v -> {
                if (displayable instanceof Task) {

                    ((Task) displayable).execute(activity).then(this.displayableTextView::invalidate);

                    Intent telegramWithTextIntent = new Intent();
                    telegramWithTextIntent.setData(Uri.parse("http://telegram.me/AndyAbbot"));
                    telegramWithTextIntent.putExtra(Intent.EXTRA_TEXT, "CUSTOM TELEGRAM MESSAGE HERE");
               //     telegramWithTextIntent.setAction(Intent.ACTION_SEND);

                    activity.startActivity(telegramWithTextIntent);
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + displayableTextView.getText() + "'";
        }
    }
}
