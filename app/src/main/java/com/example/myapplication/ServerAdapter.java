package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

    private List<Server> serverList;
    private List<Server> serverListFull; // Для поиска
    private OnItemClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public ServerAdapter(List<Server> serverList, OnItemClickListener listener) {
        this.serverList = serverList;
        this.serverListFull = new ArrayList<>(serverList); // Копия для поиска
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.server_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Server server = serverList.get(position);
        Server.ServerInfo info = server.getInfo();
        
        // Название сервера
        holder.nameTextView.setText(info.getName());
        
        // Название карты
        if (info.getMap() != null && info.getMap().getName() != null) {
            holder.mapNameTextView.setText(info.getMap().getName());
        } else {
            holder.mapNameTextView.setText("Unknown Map");
        }
        
        // Тип игры и количество игроков
        int currentPlayers = (info.getClients() != null) ? info.getClients().size() : 0;
        int maxPlayers = info.getMax_clients();
        String gameTypeText = info.getGame_type() + " (" + currentPlayers + "/" + maxPlayers + ")";
        holder.gameTypeTextView.setText(gameTypeText);
        
        // Локация
        if (server.getLocation() != null && !server.getLocation().isEmpty()) {
            holder.locationTextView.setText(server.getLocation());
        } else {
            holder.locationTextView.setText("Unknown Location");
        }
        
        // Подсветка выбранного элемента
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD")); // Светло-синий
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            // Обновляем предыдущий и новый выбранный элементы
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            
            if (listener != null && selectedPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(serverList.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return serverList.size();
    }
    
    // Метод для поиска
    public void filter(String query) {
        serverList.clear();
        
        if (query.isEmpty()) {
            serverList.addAll(serverListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            
            for (Server server : serverListFull) {
                Server.ServerInfo info = server.getInfo();
                
                // Поиск по имени сервера
                if (info.getName().toLowerCase().contains(lowerCaseQuery)) {
                    serverList.add(server);
                    continue;
                }
                
                // Поиск по названию карты
                if (info.getMap() != null && info.getMap().getName() != null && 
                    info.getMap().getName().toLowerCase().contains(lowerCaseQuery)) {
                    serverList.add(server);
                    continue;
                }
                
                // Поиск по именам игроков
                if (info.getClients() != null) {
                    for (Server.Client client : info.getClients()) {
                        if (client.getName() != null && 
                            client.getName().toLowerCase().contains(lowerCaseQuery)) {
                            serverList.add(server);
                            break;
                        }
                    }
                }
            }
        }
        
        selectedPosition = RecyclerView.NO_POSITION; // Сбрасываем выбор при поиске
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView gameTypeTextView;
        public TextView mapNameTextView;
        public TextView locationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tvServerName);
            gameTypeTextView = itemView.findViewById(R.id.tvGameType);
            mapNameTextView = itemView.findViewById(R.id.tvMapName);
            locationTextView = itemView.findViewById(R.id.tvLocation);
        }
    }
}