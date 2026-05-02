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
        // Inflate your item_trip XML
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        // Binding data to your specific XML IDs
        holder.tvVehicleCode.setText("Vehicle Code: " + trip.vehicleCode);
        holder.tvTerminal1.setText(trip.terminal1);
        holder.tvTerminal2.setText(trip.terminal2);
        holder.tvDriver.setText("Driver: " + trip.driverName);
        holder.tvConductor.setText("Conductor: " + trip.conductorName);

        // More Details Button Logic
        holder.btnMoreDetails.setOnClickListener(v -> {
            // You can pass the trip data to a details page if needed
            // Intent intent = new Intent(v.getContext(), TripDetailsActivity.class);
            // intent.putExtra("TRIP_ID", trip.tripId);
            // v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleCode, tvTerminal1, tvTerminal2, tvDriver, tvConductor;
        Button btnMoreDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Matching the IDs in your XML exactly
            tvVehicleCode = itemView.findViewById(R.id.txtcode);
            tvTerminal1 = itemView.findViewById(R.id.txtterminal1);
            tvTerminal2 = itemView.findViewById(R.id.txtterminal2);
            tvDriver = itemView.findViewById(R.id.txtassigneddriver);
            tvConductor = itemView.findViewById(R.id.txtassignedconductor);
            btnMoreDetails = itemView.findViewById(R.id.btnmoredetails);
        }
    }
}