package com.usc.lugarlangfinal.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Trip;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {
    private List<Trip> tripList;

    public TripAdapter(List<Trip> tripList) {
        this.tripList = tripList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        // 1. Route Code (Firebase key: RouteCode)
        // Make sure your Trip.java has @PropertyName("RouteCode")
        holder.tvVehicleCode.setText("Route: " + nullSafe(trip.getRouteCode()));

        // 2. Terminals (Firebase keys: Terminal1 and Terminal2)
        holder.tvTerminal1.setText(nullSafe(trip.getTerminal1()));
        holder.tvTerminal2.setText(nullSafe(trip.getTerminal2()));

        // 3. Driver and Conductor
        holder.tvDriver.setText("Driver: " + nullSafe(trip.getDriverName()));
        holder.tvConductor.setText("Conductor: " + nullSafe(trip.getConductorName()));

        // More Details Click
        holder.btnMoreDetails.setOnClickListener(v -> {
            // Your logic here
        });
    }

    @Override
    public int getItemCount() {
        return (tripList != null) ? tripList.size() : 0;
    }

    private String nullSafe(String s) {
        return (s == null || s.trim().isEmpty()) ? "N/A" : s;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleCode, tvTerminal1, tvTerminal2, tvDriver, tvConductor;
        Button btnMoreDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match your item_trip.xml exactly
            tvVehicleCode = itemView.findViewById(R.id.txtcode);
            tvTerminal1 = itemView.findViewById(R.id.txtterminal1);
            tvTerminal2 = itemView.findViewById(R.id.txtterminal2);
            tvDriver = itemView.findViewById(R.id.txtassigneddriver);
            tvConductor = itemView.findViewById(R.id.txtassignedconductor);
            btnMoreDetails = itemView.findViewById(R.id.btnmoredetails);
        }
    }
}