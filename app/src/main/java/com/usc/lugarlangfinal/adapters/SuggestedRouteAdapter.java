package com.usc.lugarlangfinal.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.commuter.SuggestedRoutes;
import com.usc.lugarlangfinal.models.Trip;
import java.util.List;

public class SuggestedRouteAdapter extends RecyclerView.Adapter<SuggestedRouteAdapter.RouteViewHolder> {

    private final List<SuggestedRoutes.ScoredTrip> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SuggestedRoutes.ScoredTrip scored);
    }

    public SuggestedRouteAdapter(List<SuggestedRoutes.ScoredTrip> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggested_route, parent, false);
        return new RouteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder h, int position) {
        SuggestedRoutes.ScoredTrip s = items.get(position);
        Trip t = s.trip;

        h.tvRouteCode.setText(t.getRouteCode());
        h.tvTerminals.setText(t.getTerminal1() + " → " + t.getTerminal2());
        h.tvTransportType.setText(t.getAssignedTransport());
        h.tvDeparture.setText("🕐 " + t.getDepartureTime());
        h.tvDriver.setText("👤 " + t.getDriverName());
        h.tvPlate.setText("🆔 " + t.getPlateNumber());
        h.tvFare.setText("₱15.00");

        // Board / alight stop names using the helper in SuggestedRoutes
        h.tvBoardStop.setText("🟢 Board: " + SuggestedRoutes.getStopName(t, s.pickupIndex));
        h.tvAlightStop.setText("🔴 Alight: " + SuggestedRoutes.getStopName(t, s.dropoffIndex));

        // Display walk distances
        h.tvBoardWalk.setText(formatDist(s.pickupDist));
        h.tvAlightWalk.setText(formatDist(s.dropoffDist));

        // Status badge color-coding
        String status = t.getStatus() != null ? t.getStatus() : "";
        h.tvStatus.setText(status);
        switch (status.toLowerCase()) {
            case "active":
            case "on route":
                h.tvStatus.setBackgroundColor(Color.parseColor("#2E7D32")); // green
                break;
            case "scheduled":
                h.tvStatus.setBackgroundColor(Color.parseColor("#1565C0")); // blue
                break;
            case "completed":
                h.tvStatus.setBackgroundColor(Color.parseColor("#757575")); // grey
                break;
            default:
                h.tvStatus.setBackgroundColor(Color.parseColor("#E65100")); // orange
                break;
        }

        h.itemView.setOnClickListener(v -> listener.onItemClick(s));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatDist(float metres) {
        if (metres < 1000) return Math.round(metres) + " m";
        return String.format("%.1f km", metres / 1000f);
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteCode, tvTerminals, tvTransportType, tvStatus;
        TextView tvBoardStop, tvBoardWalk, tvAlightStop, tvAlightWalk;
        TextView tvDeparture, tvDriver, tvPlate, tvFare;

        RouteViewHolder(@NonNull View v) {
            super(v);
            tvRouteCode     = v.findViewById(R.id.tvRouteCode);
            tvTerminals     = v.findViewById(R.id.tvTerminals);
            tvTransportType = v.findViewById(R.id.tvTransportType);
            tvStatus        = v.findViewById(R.id.tvStatus);
            tvBoardStop     = v.findViewById(R.id.tvBoardStop);
            tvBoardWalk     = v.findViewById(R.id.tvBoardWalk);
            tvAlightStop    = v.findViewById(R.id.tvAlightStop);
            tvAlightWalk    = v.findViewById(R.id.tvAlightWalk);
            tvDeparture     = v.findViewById(R.id.tvDeparture);
            tvDriver        = v.findViewById(R.id.tvDriver);
            tvPlate         = v.findViewById(R.id.tvPlate);
            tvFare          = v.findViewById(R.id.tvFare);
        }
    }
}