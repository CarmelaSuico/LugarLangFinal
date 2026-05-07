package com.usc.lugarlangfinal.commuter;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
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

    private RecyclerView recyclerView;
    private SuggestedRouteAdapter adapter;
    private final List<ScoredTrip> scoredTrips = new ArrayList<>();
    private String transportType;
    private double oLat, oLng, dLat, dLng;
    private static final String TAG = "ROUTE_CHECK";
    private static final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final float WALK_THRESHOLD_METERS = 5000f;

    public static class ScoredTrip {
        public Trip trip;
        public int pickupIndex, dropoffIndex;
        public float pickupDistMeters, dropoffDistMeters, fare;

        public ScoredTrip(Trip trip, int pIdx, int dIdx, float pDist, float dDist, float fare) {
            this.trip = trip;
            this.pickupIndex = pIdx;
            this.dropoffIndex = dIdx;
            this.pickupDistMeters = pDist;
            this.dropoffDistMeters = dDist;
            this.fare = fare;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_routes);

        transportType = getIntent().getStringExtra("TRANSPORT_TYPE");
        oLat = getIntent().getDoubleExtra("ORIGIN_LAT", 0);
        oLng = getIntent().getDoubleExtra("ORIGIN_LNG", 0);
        dLat = getIntent().getDoubleExtra("DEST_LAT", 0);
        dLng = getIntent().getDoubleExtra("DEST_LNG", 0);

        initViews();
        fetchSmartRoutes();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SuggestedRouteAdapter(scoredTrips, this);
        recyclerView.setAdapter(adapter);

        ((SearchView)findViewById(R.id.searchorigin)).setQuery(getIntent().getStringExtra("ORIGIN_NAME"), false);
        ((SearchView)findViewById(R.id.searchdestination)).setQuery(getIntent().getStringExtra("DEST_NAME"), false);
        findViewById(R.id.btnback).setOnClickListener(v -> finish());
    }

    @Override
    public void onItemClick(ScoredTrip scored) {
        showTripSummarySheet(scored);
    }

    private void showTripSummarySheet(ScoredTrip scored) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_trip_summary, null);

        ((TextView)view.findViewById(R.id.smRouteCode)).setText(scored.trip.getRouteCode());
        ((TextView)view.findViewById(R.id.smDestination)).setText(scored.trip.getTerminal2());
        ((TextView)view.findViewById(R.id.smEta)).setText("Departure: " + scored.trip.getDepartureTime());

        view.findViewById(R.id.btnMoreDetailsAction).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    private void fetchSmartRoutes() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("trips");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scoredTrips.clear();
                for (DataSnapshot franchiseSnap : snapshot.getChildren()) {
                    for (DataSnapshot tripSnap : franchiseSnap.getChildren()) {
                        Trip trip = tripSnap.getValue(Trip.class);
                        if (trip != null && trip.getAssignedTransport() != null) {
                            if (trip.getAssignedTransport().trim().equalsIgnoreCase(transportType.trim())) {
                                ScoredTrip scored = scoreTrip(trip);
                                if (scored != null) scoredTrips.add(scored);
                            }
                        }
                    }
                }
                Collections.sort(scoredTrips, (a, b) -> Float.compare(a.pickupDistMeters, b.pickupDistMeters));
                adapter.notifyDataSetChanged();
                if (scoredTrips.isEmpty()) {
                    Toast.makeText(SuggestedRoutes.this, "No routes found nearby.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private ScoredTrip scoreTrip(Trip trip) {
        List<String> allCoords = new ArrayList<>();
        if (trip.getT1_Coords() != null) allCoords.add(trip.getT1_Coords());

        // CORRECT: Splits coordinates by PIPE |
        if (trip.getStops_Coords() != null && !trip.getStops_Coords().isEmpty()) {
            for (String s : trip.getStops_Coords().split("\\|")) allCoords.add(s.trim());
        }

        if (trip.getT2_Coords() != null) allCoords.add(trip.getT2_Coords());

        int pIdx = -1, dIdx = -1;
        float pDist = Float.MAX_VALUE, dDist = Float.MAX_VALUE;

        for (int i = 0; i < allCoords.size(); i++) {
            double[] latLng = parseLatLng(allCoords.get(i));
            if (latLng == null) continue;
            float[] res = new float[1];

            Location.distanceBetween(oLat, oLng, latLng[0], latLng[1], res);
            if (res[0] < WALK_THRESHOLD_METERS && res[0] < pDist) { pDist = res[0]; pIdx = i; }

            Location.distanceBetween(dLat, dLng, latLng[0], latLng[1], res);
            if (res[0] < WALK_THRESHOLD_METERS && res[0] < dDist) { dDist = res[0]; dIdx = i; }
        }

        if (pIdx != -1 && dIdx != -1 && pIdx < dIdx) {
            return new ScoredTrip(trip, pIdx, dIdx, pDist, dDist, 15.0f);
        }
        return null;
    }

    private double[] parseLatLng(String raw) {
        try {
            String[] parts = raw.split(",");
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (Exception e) { return null; }
    }

    public static String getStopName(Trip trip, int index) {
        List<String> names = new ArrayList<>();
        names.add(trip.getTerminal1() != null ? trip.getTerminal1() : "T1");

        // CORRECT: Splits readable names by COMMA ,
        if (trip.getStops() != null && !trip.getStops().isEmpty()) {
            for (String s : trip.getStops().split(",")) names.add(s.trim());
        }

        names.add(trip.getTerminal2() != null ? trip.getTerminal2() : "T2");
        return (index >= 0 && index < names.size()) ? names.get(index) : "Stop " + index;
    }
}