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

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private Context context;
    private List<Room> roomList;

    public RoomAdapter(Context context, List<Room> roomList) {
        this.context = context;
        this.roomList = roomList;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_archived_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = roomList.get(position);

        // ✅ Match getters to your model's actual field names
        holder.txtClassName.setText(room.getClassroom_name());
        holder.txtRoomCode.setText("Code: " + room.getRoom_code());
        holder.txtCreatedAt.setText("Created: " + room.getCreated_at());
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView txtClassName, txtRoomCode, txtCreatedAt;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            txtClassName = itemView.findViewById(R.id.txtClassName);
            txtRoomCode = itemView.findViewById(R.id.txtRoomCode);
            txtCreatedAt = itemView.findViewById(R.id.txtCreatedAt);
        }
    }
}
