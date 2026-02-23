package com.example.prakriya20;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    private final List<Notice> noticeList;
    private final Context context;

    public NoticeAdapter(Context context, List<Notice> noticeList) {
        this.context = context;
        this.noticeList = noticeList;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notice_card, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.bind(notice);
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    // =======================================================================================
    // === NEW METHOD: dismissItem() - THIS IS THE CORRECT LOGIC FOR HIDING, NOT DELETING  ===
    // =======================================================================================
    public void dismissItem(int position) {
        if (position >= 0 && position < noticeList.size()) {
            Notice noticeToDismiss = noticeList.get(position);
            String noticeId = noticeToDismiss.getNoticeId();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // Safety checks
            if (currentUser == null) {
                Toast.makeText(context, "You must be logged in to dismiss a notice.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (noticeId == null || noticeId.isEmpty()) {
                Toast.makeText(context, "Cannot dismiss notice: ID is missing.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the current user's ID
            String userId = currentUser.getUid();

            // Remove the notice from the visible list immediately for a fast UI update
            noticeList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, noticeList.size());

            // Create a simple map to store the dismissed notice ID.
            // We can add more data here if needed in the future, like a timestamp.
            Map<String, Object> dismissedData = new HashMap<>();
            dismissedData.put("dismissed", true);

            // Add the notice ID to the user's personal "dismissedNotices" subcollection.
            // Firestore path will be: users/{userId}/dismissedNotices/{noticeId}
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("dismissedNotices")
                    .document(noticeId) // Use the notice ID as the document ID for easy lookup
                    .set(dismissedData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Notice dismissed.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to dismiss notice. Please try again.", Toast.LENGTH_SHORT).show();
                        // If saving the dismissal fails, add the item back to the list to maintain consistency
                        noticeList.add(position, noticeToDismiss);
                        notifyItemInserted(position);
                    });
        }
    }


    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoticeTitle, tvNoticeDescription, tvNoticeLocation, tvNoticeDateRange, tvNoticeTimestamp;
        LinearLayout locationLayout, dateRangeLayout;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoticeTitle = itemView.findViewById(R.id.tvNoticeTitle);
            tvNoticeDescription = itemView.findViewById(R.id.tvNoticeDescription);
            tvNoticeLocation = itemView.findViewById(R.id.tvNoticeLocation);
            tvNoticeDateRange = itemView.findViewById(R.id.tvNoticeDateRange);
            tvNoticeTimestamp = itemView.findViewById(R.id.tvNoticeTimestamp);
            locationLayout = itemView.findViewById(R.id.location_layout);
            dateRangeLayout = itemView.findViewById(R.id.date_range_layout);
        }

        public void bind(final Notice notice) {
            tvNoticeTitle.setText(notice.getTitle());
            tvNoticeDescription.setText(notice.getDescription());

            if (notice.getLocation() != null && !notice.getLocation().isEmpty()) {
                locationLayout.setVisibility(View.VISIBLE);
                tvNoticeLocation.setText(notice.getLocation());
            } else {
                locationLayout.setVisibility(View.GONE);
            }

            if (notice.getDateRange() != null && !notice.getDateRange().isEmpty()) {
                dateRangeLayout.setVisibility(View.VISIBLE);
                tvNoticeDateRange.setText(notice.getDateRange());
            } else {
                dateRangeLayout.setVisibility(View.GONE);
            }

            if (notice.getTimestamp() != null) {
                long time = notice.getTimestamp().getTime();
                long now = System.currentTimeMillis();
                CharSequence ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
                tvNoticeTimestamp.setText(ago);
            } else {
                tvNoticeTimestamp.setText("");
            }
        }
    }
}
