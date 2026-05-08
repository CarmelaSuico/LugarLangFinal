package com.usc.lugarlangfinal.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.Transportation.TranportationMoreDetails;
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

        // 1. Display Basic Info in the Card
        holder.tvVehicleCode.setText("Route: " + nullSafe(trip.getRouteCode()));
        holder.tvTerminal1.setText(nullSafe(trip.getTerminal1()));
        holder.tvTerminal2.setText(nullSafe(trip.getTerminal2()));
        holder.tvDriver.setText("Driver: " + nullSafe(trip.getDriverName()));
        holder.tvConductor.setText("Conductor: " + nullSafe(trip.getConductorName()));

        // 2. Full Intent for More Details
        holder.btnMoreDetails.setOnClickListener(v -> {
            // Use v.getContext() inside an adapter
            Intent intent = new Intent(v.getContext(), TranportationMoreDetails.class);

            // Pull data directly from the 'trip' object
            intent.putExtra("RouteCode", trip.getRouteCode());
            intent.putExtra("VehicleCode", trip.getVehicleCode());
            intent.putExtra("AssignedTransport", trip.getAssignedTransport());
            intent.putExtra("PlateNumber", trip.getPlateNumber());
            intent.putExtra("Terminal1", trip.getTerminal1());
            intent.putExtra("Terminal2", trip.getTerminal2());
            intent.putExtra("departureTime", trip.getDepartureTime());
            intent.putExtra("driverName", trip.getDriverName());
            intent.putExtra("conductorName", trip.getConductorName());
            intent.putExtra("status", trip.getStatus());

            // Start the activity
            v.getContext().startActivity(intent);
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