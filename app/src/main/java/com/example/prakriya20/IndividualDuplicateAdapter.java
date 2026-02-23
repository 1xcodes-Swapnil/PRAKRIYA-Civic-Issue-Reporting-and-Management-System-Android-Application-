package com.example.prakriya20;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class IndividualDuplicateAdapter extends RecyclerView.Adapter<IndividualDuplicateAdapter.ViewHolder> {
    private final List<Post> individualPosts;
    private final Context context;

    public IndividualDuplicateAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.individualPosts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_individual_duplicate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = individualPosts.get(position);
        holder.title.setText("Issue from: " + post.getUserName());
        holder.verifyButton.setOnClickListener(v -> {
            // TODO: Launch the actual verification dialog here
            Toast.makeText(context, "Verifying issue by " + post.getUserName(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return individualPosts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        Button verifyButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvIndividualIssueTitle);
            verifyButton = itemView.findViewById(R.id.btnVerifyIndividual);
        }
    }
}
