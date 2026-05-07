package com.usc.lugarlangfinal.commuter;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.Settings;
import com.usc.lugarlangfinal.commuterhome;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SeachingOriginDesti extends AppCompatActivity {

    private LinearLayout btnHomePage, btnSearch, btnSetting;
    private MapView map;
    private SearchView searchOrigin, searchDestination;
    private Button btnSuggestionRoutes;
    private GeoPoint originPoint, destinationPoint;
    private Marker originMarker, destinationMarker;
    private Polyline currentRouteLine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_seaching_origin_desti);

        map = findViewById(R.id.mapCommuter);
        searchOrigin = findViewById(R.id.searchorigin);
        searchDestination = findViewById(R.id.searchdestination);
        btnSuggestionRoutes = findViewById(R.id.btnSuggestionRoutes);
        btnHomePage = findViewById(R.id.btnhomepage);
        btnSearch = findViewById(R.id.btnsearch);
        btnSetting = findViewById(R.id.btnsetting);

        btnSearch.setSelected(true);

        btnHomePage.setOnClickListener(v -> {
            startActivity(new Intent(SeachingOriginDesti.this, commuterhome.class));
        });

        btnSetting.setOnClickListener(v -> {
            startActivity(new Intent(SeachingOriginDesti.this, Settings.class));
        });

        setupMap();

        searchOrigin.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { geocode(q, true); return true; }
            @Override public boolean onQueryTextChange(String n) { return false; }
        });

        searchDestination.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { geocode(q, false); return true; }
            @Override public boolean onQueryTextChange(String n) { return false; }
        });

        btnSuggestionRoutes.setOnClickListener(v -> showTransportDialog());
    }

    private void geocode(String name, boolean isOrigin) {
        executor.execute(() -> {
            try {
                Geocoder g = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = g.getFromLocationName(name, 1);
                if (addrs != null && !addrs.isEmpty()) {
                    Address a = addrs.get(0);
                    GeoPoint p = new GeoPoint(a.getLatitude(), a.getLongitude());
                    mainHandler.post(() -> {
                        if (isOrigin) {
                            originPoint = p;
                            if (originMarker != null) map.getOverlays().remove(originMarker);
                            originMarker = new Marker(map);
                            originMarker.setPosition(p);
                            originMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.trip_origin_24px));
                            map.getOverlays().add(originMarker);
                        } else {
                            destinationPoint = p;
                            if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
                            destinationMarker = new Marker(map);
                            destinationMarker.setPosition(p);
                            destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            destinationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.location_on_24px));
                            map.getOverlays().add(destinationMarker);
                        }
                        map.getController().animateTo(p);
                        drawRoutePreview();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void drawRoutePreview() {
        if (originPoint != null && destinationPoint != null) {
            ArrayList<GeoPoint> points = new ArrayList<>();
            points.add(originPoint); points.add(destinationPoint);
            executor.execute(() -> {
                Road road = new OSRMRoadManager(this, getPackageName()).getRoad(points);
                mainHandler.post(() -> {
                    if (road.mStatus == Road.STATUS_OK) {
                        if (currentRouteLine != null) map.getOverlays().remove(currentRouteLine);
                        currentRouteLine = RoadManager.buildRoadOverlay(road);
                        currentRouteLine.getOutlinePaint().setColor(Color.BLACK);
                        currentRouteLine.getOutlinePaint().setStrokeWidth(10.0f);
                        map.getOverlays().add(0, currentRouteLine);
                        btnSuggestionRoutes.setVisibility(View.VISIBLE);
                        map.invalidate();
                    }
                });
            });
        }
    }

    private void showTransportDialog() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.layout_transport_selection, null);
        v.findViewById(R.id.optionBus).setOnClickListener(view -> startSuggested("Bus", d));
        v.findViewById(R.id.optionJeepney).setOnClickListener(view -> startSuggested("Jeepney", d));
        d.setContentView(v); d.show();
    }

    private void startSuggested(String type, BottomSheetDialog d) {
        d.dismiss();
        Intent i = new Intent(this, SuggestedRoutes.class);
        i.putExtra("TRANSPORT_TYPE", type);
        i.putExtra("ORIGIN_LAT", originPoint.getLatitude()); // Ensure double
        i.putExtra("ORIGIN_LNG", originPoint.getLongitude());
        i.putExtra("DEST_LAT", destinationPoint.getLatitude());
        i.putExtra("DEST_LNG", destinationPoint.getLongitude());
        i.putExtra("ORIGIN_NAME", searchOrigin.getQuery().toString());
        i.putExtra("DEST_NAME", searchDestination.getQuery().toString());
        startActivity(i);
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(10.3157, 123.8854));
    }
}