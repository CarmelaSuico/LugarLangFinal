package com.usc.lugarlangfinal.commuter;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.SuggestedRouteAdapter;
import com.usc.lugarlangfinal.models.Trip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuggestedRoutes extends AppCompatActivity implements SuggestedRouteAdapter.OnItemClickListener {

    private SuggestedRouteAdapter adapter;
    private final List<ScoredTrip> scoredTrips = new ArrayList<>();
    private String transportType;
    private double oLat, oLng, dLat, dLng;
    private TextView tvNoRoutes;

    private static final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    // 2km radius is a reasonable walking distance for a recommendation.
    private static final float WALK_THRESHOLD = 2000f; 

    public static class ScoredTrip {
        public Trip trip;
        public int pickupIndex, dropoffIndex;
        public float pickupDist, dropoffDist;
        public float totalWalk;

        public ScoredTrip(Trip trip, int pIdx, int dIdx, float pDist, float dDist) {
            this.trip = trip;
            this.pickupIndex = pIdx;
            this.dropoffIndex = dIdx;
            this.pickupDist = pDist;
            this.dropoffDist = dDist;
            this.totalWalk = pDist + dDist;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_routes);

        ImageButton btnBack = findViewById(R.id.btnback);
        SearchView searchOrigin = findViewById(R.id.searchorigin);
        SearchView searchDestination = findViewById(R.id.searchdestination);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        tvNoRoutes = findViewById(R.id.tvNoRoutesMessage);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        transportType = getIntent().getStringExtra("TRANSPORT_TYPE");
        oLat = getIntent().getDoubleExtra("ORIGIN_LAT", 0);
        oLng = getIntent().getDoubleExtra("ORIGIN_LNG", 0);
        dLat = getIntent().getDoubleExtra("DEST_LAT", 0);
        dLng = getIntent().getDoubleExtra("DEST_LNG", 0);

        String originName = getIntent().getStringExtra("ORIGIN_NAME");
        String destName   = getIntent().getStringExtra("DEST_NAME");
        if (searchOrigin != null)      searchOrigin.setQuery(originName, false);
        if (searchDestination != null) searchDestination.setQuery(destName, false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuggestedRouteAdapter(scoredTrips, this);
        recyclerView.setAdapter(adapter);

        fetchSmartRoutes();
    }

    private void fetchSmartRoutes() {
        DatabaseReference tripsRef = FirebaseDatabase.getInstance(DB_URL).getReference("trips");
        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scoredTrips.clear();

                for (DataSnapshot franchiseSnap : snapshot.getChildren()) {
                    for (DataSnapshot tripSnap : franchiseSnap.getChildren()) {
                        Trip trip = tripSnap.getValue(Trip.class);
                        if (trip == null) continue;

                        // Check transport type filter
                        if (transportType != null && !transportType.isEmpty() && 
                            !transportType.equalsIgnoreCase("All") && 
                            !transportType.equalsIgnoreCase("Select Transport Type")) {
                            if (trip.getAssignedTransport() == null || 
                                !trip.getAssignedTransport().equalsIgnoreCase(transportType)) {
                                continue;
                            }
                        }

                        ScoredTrip scored = scoreTrip(trip);
                        if (scored != null) scoredTrips.add(scored);
                    }
                }

                // Sort by least total walking distance (Best recommendations first)
                Collections.sort(scoredTrips, (a, b) -> Float.compare(a.totalWalk, b.totalWalk));
                adapter.notifyDataSetChanged();

                if (tvNoRoutes != null) {
                    tvNoRoutes.setVisibility(scoredTrips.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (tvNoRoutes != null) tvNoRoutes.setVisibility(View.VISIBLE);
            }
        });
    }

    private ScoredTrip scoreTrip(Trip trip) {
        List<String> coordList = new ArrayList<>();
        coordList.add(trip.getT1Coords());
        
        String stopCoordsRaw = trip.getStopCoords();
        if (stopCoordsRaw != null && !stopCoordsRaw.trim().isEmpty()) {
            for (String seg : stopCoordsRaw.split("\\|")) {
                String trimmed = seg.trim();
                if (!trimmed.isEmpty()) coordList.add(trimmed);
            }
        }
        coordList.add(trip.getT2Coords());

        ScoredTrip bestForTrip = null;
        float minTotalWalk = Float.MAX_VALUE;

        // Iterate through all possible board/alight pairs to find the most optimal one
        for (int i = 0; i < coordList.size(); i++) {
            float pDist = distanceTo(coordList.get(i), oLat, oLng);
            if (pDist > WALK_THRESHOLD) continue;

            for (int j = i + 1; j < coordList.size(); j++) {
                float dDist = distanceTo(coordList.get(j), dLat, dLng);
                if (dDist > WALK_THRESHOLD) continue;

                float total = pDist + dDist;
                if (total < minTotalWalk) {
                    minTotalWalk = total;
                    bestForTrip = new ScoredTrip(trip, i, j, pDist, dDist);
                }
            }
        }
        return bestForTrip;
    }

    private float distanceTo(String coordStr, double lat, double lng) {
        try {
            String[] parts = coordStr.split(",");
            float[] res = new float[1];
            Location.distanceBetween(lat, lng, Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()), res);
            return res[0];
        } catch (Exception e) {
            return Float.MAX_VALUE;
        }
    }

    public static String getStopName(Trip trip, int index) {
        List<String> names = new ArrayList<>();
        names.add(trip.getTerminal1() != null && !trip.getTerminal1().isEmpty() ? trip.getTerminal1() : "Terminal 1");

        if (trip.getStops() != null && !trip.getStops().trim().isEmpty()) {
            for (String s : trip.getStops().split(",")) {
                names.add(s.trim());
            }
        }

        names.add(trip.getTerminal2() != null && !trip.getTerminal2().isEmpty() ? trip.getTerminal2() : "Terminal 2");
        return (index >= 0 && index < names.size()) ? names.get(index) : "Stop " + index;
    }

    @Override
    public void onItemClick(ScoredTrip s) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_trip_summary, null);

        TextView smRoute   = view.findViewById(R.id.smRouteCode);
        TextView smDest    = view.findViewById(R.id.smDestination);
        TextView smEta     = view.findViewById(R.id.smEta);
        TextView btnClose  = view.findViewById(R.id.btnMoreDetailsAction);

        if (smRoute != null) smRoute.setText(s.trip.getRouteCode());
        if (smDest  != null) smDest.setText(s.trip.getTerminal2());

        String summary = "🟢 Board at: " + getStopName(s.trip, s.pickupIndex) + " (" + formatDist(s.pickupDist) + " walk)\n" +
                        "🔴 Alight at: " + getStopName(s.trip, s.dropoffIndex) + " (" + formatDist(s.dropoffDist) + " walk)\n" +
                        "🕐 Departure: " + s.trip.getDepartureTime() + "\n" +
                        "👤 Driver: "    + s.trip.getDriverName() + "\n" +
                        "💰 Fare: ₱15.00";

        if (smEta != null) smEta.setText(summary);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    private String formatDist(float metres) {
        if (metres < 1000) return Math.round(metres) + " m";
        return String.format("%.1f km", metres / 1000f);
    }
}