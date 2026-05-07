package com.usc.lugarlangfinal.adapters;

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
import java.util.Locale;

public class SuggestedRouteAdapter extends RecyclerView.Adapter<SuggestedRouteAdapter.RouteViewHolder> {

    private final List<SuggestedRoutes.ScoredTrip> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SuggestedRoutes.ScoredTrip scoredTrip);
    }

    public SuggestedRouteAdapter(List<SuggestedRoutes.ScoredTrip> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggested_route, parent, false);
        return new RouteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder h, int position) {
        SuggestedRoutes.ScoredTrip scored = items.get(position);
        Trip trip = scored.trip;

        h.tvRouteCode.setText(trip.getRouteCode());
        h.tvTerminals.setText(trip.getTerminal1() + " → " + trip.getTerminal2());
        h.tvTransportType.setText(trip.getAssignedTransport());
        h.tvStatus.setText(trip.getStatus());

        // Shows the name of the stop (e.g., San Remigio)
        h.tvBoardStop.setText("🟢 Board: " + SuggestedRoutes.getStopName(trip, scored.pickupIndex));
        h.tvBoardWalk.setText(formatDist(scored.pickupDistMeters));

        h.tvAlightStop.setText("🔴 Alight: " + SuggestedRoutes.getStopName(trip, scored.dropoffIndex));
        h.tvAlightWalk.setText(formatDist(scored.dropoffDistMeters));

        h.tvDeparture.setText("🕐 Departure: " + trip.getDepartureTime());
        h.tvDriver.setText("👤 Driver: " + trip.getDriverName());
        h.tvPlate.setText("🆔 " + trip.getPlateNumber());
        h.tvFare.setText(String.format(Locale.getDefault(), "₱%.2f", scored.fare));

        h.itemView.setOnClickListener(v -> listener.onItemClick(scored));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatDist(float m) {
        return m < 1000 ? Math.round(m) + "m" : String.format(Locale.getDefault(), "%.1fkm", m / 1000f);
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteCode, tvTerminals, tvTransportType, tvBoardStop, tvBoardWalk,
                tvAlightStop, tvAlightWalk, tvDeparture, tvDriver, tvPlate, tvFare, tvStatus;

        RouteViewHolder(@NonNull View v) {
            super(v);
            tvRouteCode = v.findViewById(R.id.tvRouteCode);
            tvTerminals = v.findViewById(R.id.tvTerminals);
            tvTransportType = v.findViewById(R.id.tvTransportType);
            tvBoardStop = v.findViewById(R.id.tvBoardStop);
            tvBoardWalk = v.findViewById(R.id.tvBoardWalk);
            tvAlightStop = v.findViewById(R.id.tvAlightStop);
            tvAlightWalk = v.findViewById(R.id.tvAlightWalk);
            tvDeparture = v.findViewById(R.id.tvDeparture);
            tvDriver = v.findViewById(R.id.tvDriver);
            tvPlate = v.findViewById(R.id.tvPlate);
            tvFare = v.findViewById(R.id.tvFare);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}