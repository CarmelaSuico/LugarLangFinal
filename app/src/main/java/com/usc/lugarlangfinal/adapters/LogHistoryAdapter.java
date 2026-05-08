package com.usc.lugarlangfinal.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.LogTrip;
import java.util.List;

public class LogHistoryAdapter extends RecyclerView.Adapter<LogHistoryAdapter.ViewHolder> {

    private List<LogTrip> historyList;

    public LogHistoryAdapter(List<LogTrip> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_triphistory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogTrip trip = historyList.get(position);

        // Mapping model data to item_triphistory IDs
        holder.tvRouteCode.setText(nullSafe(trip.getRouteCode()));
        holder.tvOrigin.setText(nullSafe(trip.getTerminal1()));
        holder.tvDestination.setText(nullSafe(trip.getTerminal2()));
        holder.tvDeparture.setText("Departure time: " + nullSafe(trip.getDepartureTime()));
    }

    @Override
    public int getItemCount() {
        return (historyList != null) ? historyList.size() : 0;
    }

    private String nullSafe(String s) {
        return (s == null || s.trim().isEmpty()) ? "N/A" : s;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteCode, tvOrigin, tvDestination, tvDeparture;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Matching the IDs in your item_triphistory.xml
            tvRouteCode = itemView.findViewById(R.id.tvtroutecode);
            tvOrigin = itemView.findViewById(R.id.txtorigin);
            tvDestination = itemView.findViewById(R.id.txtdestination);
            tvDeparture = itemView.findViewById(R.id.txtdeparture);
        }
    }
}