package com.example.smartessay.Archive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartessay.R;

import java.util.List;

public class ArchivedClassAdapter extends RecyclerView.Adapter<ArchivedClassAdapter.ViewHolder> {

    private Context context;
    private List<ArchivedClass> archivedList;

    public ArchivedClassAdapter(Context context, List<ArchivedClass> archivedList) {
        this.context = context;
        this.archivedList = archivedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_archived_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArchivedClass item = archivedList.get(position);
        holder.className.setText(item.getClassroom_name());
        holder.roomCode.setText("Code: " + item.getRoom_code());
        holder.createdAt.setText("Created: " + item.getCreated_at());
    }

    @Override
    public int getItemCount() {
        return archivedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView className, roomCode, createdAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.txtClassName);
            roomCode = itemView.findViewById(R.id.txtRoomCode);
            createdAt = itemView.findViewById(R.id.txtCreatedAt);
        }
    }
}
