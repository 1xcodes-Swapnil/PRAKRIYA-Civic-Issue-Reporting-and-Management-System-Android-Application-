package com.example.prakriya20;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IndividualPostAdapter extends RecyclerView.Adapter<IndividualPostAdapter.ViewHolder> {

    private final Context context;
    private final List<Post> postList;

    // ====================================================================
    // === NECESSARY ADDITION: A listener to communicate with the parent ===
    // ====================================================================
    private OnItemHandledListener onItemHandledListener;

    public interface OnItemHandledListener {
        void onItemHandled();
    }

    public void setOnItemHandledListener(OnItemHandledListener listener) {
        this.onItemHandledListener = listener;
    }
    // ====================================================================


    public IndividualPostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_individual_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        if (post == null) return;

        holder.reporterName.setText(post.getUserName());
        holder.postDescription.setText(post.getDescription());

        if (post.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.postTimestamp.setText(sdf.format(post.getTimestamp()));
        }

        PostManager.loadImageFromBase64(post.getUserProfileImageString(), holder.reporterAvatar, context);

        if ("Verified".equals(post.getStatus()) || "Spam".equals(post.getStatus())) {
            holder.btnVerify.setText(post.getStatus());
            holder.btnVerify.setEnabled(false);
            holder.btnVerify.setBackgroundColor(Color.GRAY);
        } else {
            holder.btnVerify.setText("Verify");
            holder.btnVerify.setEnabled(true);
            holder.btnVerify.setBackgroundColor(context.getResources().getColor(R.color.prakriya_teal));
        }

        holder.btnVerify.setOnClickListener(v -> {
            if (context instanceof AppCompatActivity) {
                FragmentManager fm = ((AppCompatActivity) context).getSupportFragmentManager();

                DuplicateVerificationDialog.OnVerificationCompleteListener listener = (postId, newStatus) -> {
                    post.setStatus(newStatus);
                    notifyItemChanged(holder.getAdapterPosition());

                    // This is the new line that calls the listener
                    if (onItemHandledListener != null) {
                        onItemHandledListener.onItemHandled();
                    }
                };

                DuplicateVerificationDialog dialog = DuplicateVerificationDialog.newInstance(post, listener);
                dialog.show(fm, "DuplicateVerificationDialog");
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView reporterAvatar;
        TextView reporterName, postDescription, postTimestamp;
        Button btnVerify;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            reporterAvatar = itemView.findViewById(R.id.individual_reporter_avatar);
            reporterName = itemView.findViewById(R.id.individual_reporter_name);
            postDescription = itemView.findViewById(R.id.individual_post_description);
            postTimestamp = itemView.findViewById(R.id.individual_post_timestamp);
            btnVerify = itemView.findViewById(R.id.btnVerify);
        }
    }
}
