package com.usc.lugarlangfinal.commuter;

import android.content.Intent;
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
    private static final float WALK_THRESHOLD = 10000f;
    private static final double DISCOUNT_RATE = 0.20;

    // ── Data class ───────────────────────────────────────────────────────────────
    public static class ScoredTrip {
        public Trip trip;
        public int pickupIndex, dropoffIndex;
        public float pickupDist, dropoffDist;
        public float totalWalk;
        public double regularFare = -1; // -1 = not yet loaded

        public ScoredTrip(Trip trip, int pIdx, int dIdx, float pDist, float dDist) {
            this.trip         = trip;
            this.pickupIndex  = pIdx;
            this.dropoffIndex = dIdx;
            this.pickupDist   = pDist;
            this.dropoffDist  = dDist;
            this.totalWalk    = pDist + dDist;
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_routes);

        ImageButton btnBack           = findViewById(R.id.btnback);
        SearchView  searchOrigin      = findViewById(R.id.searchorigin);
        SearchView  searchDestination = findViewById(R.id.searchdestination);
        RecyclerView recyclerView     = findViewById(R.id.recyclerView);
        tvNoRoutes                    = findViewById(R.id.tvNoRoutesMessage);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        transportType = getIntent().getStringExtra("TRANSPORT_TYPE");
        oLat = getIntent().getDoubleExtra("ORIGIN_LAT", 0);
        oLng = getIntent().getDoubleExtra("ORIGIN_LNG", 0);
        dLat = getIntent().getDoubleExtra("DEST_LAT", 0);
        dLng = getIntent().getDoubleExtra("DEST_LNG", 0);

        if (searchOrigin != null)
            searchOrigin.setQuery(getIntent().getStringExtra("ORIGIN_NAME"), false);
        if (searchDestination != null)
            searchDestination.setQuery(getIntent().getStringExtra("DEST_NAME"), false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuggestedRouteAdapter(scoredTrips, this);
        recyclerView.setAdapter(adapter);

        fetchSmartRoutes();
    }

    // ── Step 1: fetch matching trips ─────────────────────────────────────────────
    private void fetchSmartRoutes() {
        FirebaseDatabase.getInstance(DB_URL).getReference("trips")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        scoredTrips.clear();
                        for (DataSnapshot franchiseSnap : snapshot.getChildren()) {
                            for (DataSnapshot tripSnap : franchiseSnap.getChildren()) {
                                Trip trip = tripSnap.getValue(Trip.class);
                                if (trip == null) continue;
                                if (trip.getAssignedTransport() == null) continue;
                                if (!trip.getAssignedTransport().equalsIgnoreCase(transportType)) continue;
                                ScoredTrip scored = scoreTrip(trip);
                                if (scored != null) scoredTrips.add(scored);
                            }
                        }
                        Collections.sort(scoredTrips, (a, b) -> Float.compare(a.totalWalk, b.totalWalk));

                        if (scoredTrips.isEmpty()) {
                            if (tvNoRoutes != null) tvNoRoutes.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        } else {
                            fetchFaresForResults();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (tvNoRoutes != null) tvNoRoutes.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ── Step 2: fetch fare data from routes/{routeCode} ──────────────────────────
    private void fetchFaresForResults() {
        FirebaseDatabase.getInstance(DB_URL).getReference("routes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (ScoredTrip s : scoredTrips) {
                            String routeCode = s.trip.getRouteCode();
                            if (routeCode == null || routeCode.isEmpty()) continue;

                            DataSnapshot route = snapshot.child(routeCode);
                            if (!route.exists()) continue;

                            Double baseFare   = route.child("BaseFare").getValue(Double.class);
                            Double addFare    = route.child("AdditionalFarePerBand").getValue(Double.class);
                            Integer distBands = route.child("DistanceBands").getValue(Integer.class);

                            s.regularFare = calculateFare(
                                    s.trip, s.pickupIndex, s.dropoffIndex,
                                    baseFare   != null ? baseFare   : 0.0,
                                    addFare    != null ? addFare    : 0.0,
                                    distBands  != null ? distBands  : 0
                            );
                        }
                        adapter.notifyDataSetChanged();
                        if (tvNoRoutes != null) tvNoRoutes.setVisibility(View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // ── Fare formula ─────────────────────────────────────────────────────────────
    public static double calculateFare(Trip trip, int pickupIdx, int dropoffIdx,
                                       double baseFare, double additionalFarePerBand,
                                       int distanceBands) {
        List<String> coords = buildCoordList(trip);
        if (pickupIdx < 0 || dropoffIdx >= coords.size()) return baseFare;
        try {
            String[] p1 = coords.get(pickupIdx).split(",");
            String[] p2 = coords.get(dropoffIdx).split(",");
            float[] res = new float[1];
            Location.distanceBetween(
                    Double.parseDouble(p1[0].trim()), Double.parseDouble(p1[1].trim()),
                    Double.parseDouble(p2[0].trim()), Double.parseDouble(p2[1].trim()), res);
            double km = res[0] / 1000.0;
            double fare = baseFare;
            if (km > distanceBands) {
                fare += Math.ceil(km - distanceBands) * additionalFarePerBand;
            }
            return fare;
        } catch (Exception e) {
            return baseFare;
        }
    }

    public static double applyDiscount(double regularFare, String passengerType) {
        if (passengerType == null || passengerType.equalsIgnoreCase("Regular"))
            return regularFare;
        return regularFare - (regularFare * DISCOUNT_RATE);
    }

    // ── Scoring ──────────────────────────────────────────────────────────────────
    private ScoredTrip scoreTrip(Trip trip) {
        List<String> coordList = buildCoordList(trip);

        int pIdx = -1; float pMin = Float.MAX_VALUE;
        for (int i = 0; i < coordList.size(); i++) {
            float d = distanceTo(coordList.get(i), oLat, oLng);
            if (d < pMin && d < WALK_THRESHOLD) { pMin = d; pIdx = i; }
        }
        int dIdx = -1; float dMin = Float.MAX_VALUE;
        for (int i = pIdx + 1; i < coordList.size(); i++) {
            float d = distanceTo(coordList.get(i), dLat, dLng);
            if (d < dMin && d < WALK_THRESHOLD) { dMin = d; dIdx = i; }
        }
        if (pIdx == -1 || dIdx == -1) return null;
        return new ScoredTrip(trip, pIdx, dIdx, pMin, dMin);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────
    public static List<String> buildCoordList(Trip trip) {
        List<String> list = new ArrayList<>();
        // 1. Add Terminal 1 Coords
        list.add(trip.getT1Coords() != null ? trip.getT1Coords() : "0,0");

        // 2. Add intermediate stops Coords
        String raw = trip.getStopCoords();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String seg : raw.split("\\|")) {
                String t = seg.trim();
                if (!t.isEmpty()) list.add(t);
            }
        }

        // 3. Add Terminal 2 Coords
        list.add(trip.getT2Coords() != null ? trip.getT2Coords() : "0,0");
        return list;
    }

    public static String getStopName(Trip trip, int index) {
        List<String> names = new ArrayList<>();
        names.add(trip.getTerminal1() != null ? trip.getTerminal1() : "Terminal 1");
        if (trip.getStops() != null && !trip.getStops().trim().isEmpty()) {
            for (String s : trip.getStops().split(",")) names.add(s.trim());
        }
        names.add(trip.getTerminal2() != null ? trip.getTerminal2() : "Terminal 2");
        return (index >= 0 && index < names.size()) ? names.get(index) : "Stop";
    }

    private float distanceTo(String coordStr, double lat, double lng) {
        try {
            String[] p = coordStr.split(",");
            float[] r = new float[1];
            Location.distanceBetween(lat, lng,
                    Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()), r);
            return r[0];
        } catch (Exception e) { return Float.MAX_VALUE; }
    }

    private String formatDist(float m) {
        return m < 1000 ? Math.round(m) + " m" : String.format("%.1f km", m / 1000f);
    }

    // ── Bottom sheet on item click ───────────────────────────────────────────────
    @Override
    public void onItemClick(ScoredTrip s) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_trip_summary, null);

        TextView smRoute        = view.findViewById(R.id.smRouteCode);
        TextView smDest         = view.findViewById(R.id.smDestination);
        TextView smEta          = view.findViewById(R.id.smEta);
        TextView btnMoreDetails = view.findViewById(R.id.btnMoreDetailsAction);

        if (smRoute != null) smRoute.setText(s.trip.getRouteCode());
        if (smDest  != null) smDest.setText(s.trip.getTerminal2());

        String fareLine;
        if (s.regularFare >= 0) {
            fareLine = String.format(
                    "💰 Regular:             ₱%.2f\n" +
                            "   Student/PWD/Senior: ₱%.2f",
                    s.regularFare,
                    applyDiscount(s.regularFare, "Student")
            );
        } else {
            fareLine = "💰 Fare: unavailable";
        }

        String summary =
                "🟢 Board at:  " + getStopName(s.trip, s.pickupIndex)
                        + "  (" + formatDist(s.pickupDist) + " walk)\n" +
                        "🔴 Alight at: " + getStopName(s.trip, s.dropoffIndex)
                        + "  (" + formatDist(s.dropoffDist) + " walk)\n" +
                        "🕐 Departure: " + s.trip.getDepartureTime() + "\n" +
                        "👤 Driver:    " + s.trip.getDriverName() + "\n" +
                        fareLine;

        if (smEta != null) smEta.setText(summary);

        if (btnMoreDetails != null) {
            btnMoreDetails.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, TripMoreDetails.class);

                // Essential Data for Path Reconstruction
                intent.putExtra("TRIP_DATA", s.trip);
                intent.putExtra("PICKUP_INDEX", s.pickupIndex);
                intent.putExtra("DROPOFF_INDEX", s.dropoffIndex);

                // Fare and Distance Data
                intent.putExtra("CALCULATED_FARE", s.regularFare >= 0 ? s.regularFare : 0.0);
                intent.putExtra("PICKUP_DIST", s.pickupDist);
                intent.putExtra("DROPOFF_DIST", s.dropoffDist);

                // Origin and Destination names for the "Origin > Destination" Header fix
                SearchView searchOrigin = findViewById(R.id.searchorigin);
                SearchView searchDestination = findViewById(R.id.searchdestination);

                if (searchOrigin != null && searchDestination != null) {
                    intent.putExtra("USER_ORIGIN", searchOrigin.getQuery().toString());
                    intent.putExtra("USER_DEST", searchDestination.getQuery().toString());
                }

                startActivity(intent);
            });
        }

        dialog.setContentView(view);
        dialog.show();
    }
}