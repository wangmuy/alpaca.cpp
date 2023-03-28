package org.alpaca.test;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView personName;
        public TextView chatText;
        public ViewGroup root;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = (ViewGroup) itemView;
            personName = itemView.findViewById(R.id.personName);
            chatText = itemView.findViewById(R.id.chatText);
        }
    }

    private List<ChatItem> chats;

    public ChatAdapter(List<ChatItem> chats) {
        this.chats = chats;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatItem chatItem = chats.get(position);
        holder.personName.setText(chatItem.name+": ");
        holder.chatText.setText(chatItem.sentence);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }
}
