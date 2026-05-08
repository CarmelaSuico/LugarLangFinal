package com.usc.lugarlangfinal.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.logmonitoring.DriverTripHistoryActivity;
import com.usc.lugarlangfinal.models.LogTrip;
import java.util.List;

public class LogTripAdapter extends RecyclerView.Adapter<LogTripAdapter.ViewHolder> {

    private List<LogTrip> logList;

    public LogTripAdapter(List<LogTrip> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trips, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogTrip trip = logList.get(position);

        // 1. Employee ID & Driver Name (using the ShapeableImageView layout IDs)
        holder.tvEmployeeId.setText(nullSafe(trip.getEmployeeId()));
        holder.tvName.setText(nullSafe(trip.getDriverName()));

        // 2. Route Code (txtassignedroute)
        holder.tvAssignedRoute.setText("Route Code: " + nullSafe(trip.getRouteCode()));

        // 3. Assigned Unit (txtassignedunit)
        holder.tvAssignedUnit.setText("Assigned Unit: " + nullSafe(trip.getAssignedTransport()));

        // 4. View All / History Button
        holder.btnViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DriverTripHistoryActivity.class);
            // Passing keys aligned with DriverTripHistoryActivity
            intent.putExtra("SELECTED_ID", trip.getEmployeeId());
            intent.putExtra("FRANCHISE", trip.getFranchise());
            intent.putExtra("DRIVER_NAME", trip.getDriverName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return (logList != null) ? logList.size() : 0;
    }

    // Required for the SearchView in LogMonitoring to work
    public void updateList(List<LogTrip> newList) {
        this.logList = newList;
        notifyDataSetChanged();
    }

    private String nullSafe(String s) {
        return (s == null || s.trim().isEmpty()) ? "N/A" : s;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeId, tvName, tvAssignedRoute, tvAssignedUnit;
        MaterialButton btnViewAll;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeId = itemView.findViewById(R.id.txtemployeeid);
            tvName = itemView.findViewById(R.id.txtname);
            tvAssignedRoute = itemView.findViewById(R.id.txtassignedroute);
            tvAssignedUnit = itemView.findViewById(R.id.txtassignedunit);
            btnViewAll = itemView.findViewById(R.id.btnviewall);
        }
    }
}