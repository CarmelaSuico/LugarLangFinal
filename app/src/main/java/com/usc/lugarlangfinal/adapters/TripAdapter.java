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

        // USE GETTERS INSTEAD OF DIRECT FIELDS
        // This prevents crashes now that fields are private in the Trip model
        holder.tvVehicleCode.setText("Vehicle Code: " + nullSafe(trip.getVehicleCode()));
        holder.tvTerminal1.setText(nullSafe(trip.getTerminal1()));
        holder.tvTerminal2.setText(nullSafe(trip.getTerminal2()));
        holder.tvDriver.setText("Driver: " + nullSafe(trip.getDriverName()));
        holder.tvConductor.setText("Conductor: " + nullSafe(trip.getConductorName()));

        // More Details Button Logic
        holder.btnMoreDetails.setOnClickListener(v -> {
            // Passing data via Getter
            // Intent intent = new Intent(v.getContext(), TripDetailsActivity.class);
            // intent.putExtra("TRIP_ID", trip.getTripId());
            // v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tripList != null ? tripList.size() : 0;
    }

    // Helper method to prevent "null" text showing up in the UI
    private String nullSafe(String s) {
        return (s == null || s.isEmpty()) ? "N/A" : s;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleCode, tvTerminal1, tvTerminal2, tvDriver, tvConductor;
        Button btnMoreDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVehicleCode = itemView.findViewById(R.id.txtcode);
            tvTerminal1 = itemView.findViewById(R.id.txtterminal1);
            tvTerminal2 = itemView.findViewById(R.id.txtterminal2);
            tvDriver = itemView.findViewById(R.id.txtassigneddriver);
            tvConductor = itemView.findViewById(R.id.txtassignedconductor);
            btnMoreDetails = itemView.findViewById(R.id.btnmoredetails);
        }
    }
}