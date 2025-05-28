package licenta.andrei.catanoiu.securehive.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import licenta.andrei.catanoiu.securehive.R;
import licenta.andrei.catanoiu.securehive.models.ActivityItem;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final List<ActivityItem> activityItems;

    public RecentActivityAdapter(List<ActivityItem> activityItems) {
        this.activityItems = activityItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityItem item = activityItems.get(position);
        holder.deviceName.setText(item.getDeviceName());
        holder.activityDescription.setText(item.getDescription());
        holder.timestamp.setText(item.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return activityItems.size();
    }

    public void updateItems(List<ActivityItem> newItems) {
        activityItems.clear();
        activityItems.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceName;
        final TextView activityDescription;
        final TextView timestamp;

        ViewHolder(View view) {
            super(view);
            deviceName = view.findViewById(R.id.deviceName);
            activityDescription = view.findViewById(R.id.activityDescription);
            timestamp = view.findViewById(R.id.timestamp);
        }
    }
} 