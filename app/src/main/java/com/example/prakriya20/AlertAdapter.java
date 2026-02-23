package com.example.prakriya20;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Import Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private final Context context;
    private final List<Notification> notificationList;

    public AlertAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        if (notification == null) return;

        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());

        if (notification.getTimestamp() != null) {
            holder.timestamp.setText(
                    DateUtils.getRelativeTimeSpanString(
                            notification.getTimestamp().getTime(),
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                    )
            );
        } else {
            holder.timestamp.setText("Just now");
        }

        // Set icon and background based on notification type
        switch (notification.getType()) {
            case "Resolved":
                holder.iconBg.setBackgroundResource(R.drawable.circle_green_light);
                holder.icon.setImageResource(R.drawable.ic_check_circle);
                holder.icon.setColorFilter(context.getResources().getColor(R.color.green));
                break;
            case "Spam":
                holder.iconBg.setBackgroundResource(R.drawable.circle_red_light);
                holder.icon.setImageResource(R.drawable.ic_error);
                holder.icon.setColorFilter(context.getResources().getColor(R.color.red));
                break;
            case "Verified":
            case "In Progress":
            default:
                holder.iconBg.setBackgroundResource(R.drawable.circle_yellow_light);
                holder.icon.setImageResource(R.drawable.ic_clock);
                holder.icon.setColorFilter(context.getResources().getColor(R.color.orange));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    // =========================================================================
    // === NEW METHOD ADDED HERE - This handles deleting an item             ===
    // =========================================================================
    public void deleteItem(int position) {
        if (position >= 0 && position < notificationList.size()) {
            Notification notificationToDelete = notificationList.get(position);
            String notificationId = notificationToDelete.getNotificationId();

            if (notificationId == null || notificationId.isEmpty()) {
                Toast.makeText(context, "Cannot dismiss alert: ID is missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Immediately remove from the list to update the UI optimistically
            notificationList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, notificationList.size());


            // Delete from Firestore in the background
            FirebaseFirestore.getInstance().collection("notifications").document(notificationId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Alert dismissed.", Toast.LENGTH_SHORT).show();
                        // If the list becomes empty after deletion, show a toast.
                        if (notificationList.isEmpty()) {
                            // The activity is responsible for showing the empty message,
                            // but a toast here provides immediate feedback.
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to dismiss alert. Please try again.", Toast.LENGTH_SHORT).show();
                        // If deletion fails, add the item back to maintain consistency
                        notificationList.add(position, notificationToDelete);
                        notifyItemInserted(position);
                    });
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        View iconBg;
        ImageView icon;
        TextView title, message, timestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.alert_icon_bg);
            icon = itemView.findViewById(R.id.alert_icon);
            title = itemView.findViewById(R.id.alert_title);
            message = itemView.findViewById(R.id.alert_message);
            timestamp = itemView.findViewById(R.id.alert_timestamp);
        }
    }
}
