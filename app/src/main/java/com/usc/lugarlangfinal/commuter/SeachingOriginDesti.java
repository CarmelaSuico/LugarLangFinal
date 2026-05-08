package com.usc.lugarlangfinal.commuter;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.Settings;
import com.usc.lugarlangfinal.commuterhome;

import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.POI;
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
    private MapView map;
    private SearchView searchOrigin, searchDestination;
    private Button btnSuggestionRoutes;
    private GeoPoint originPoint, destinationPoint;
    private Marker originMarker, destinationMarker;
    private Polyline currentRouteLine;

    private SimpleCursorAdapter originAdapter, destAdapter;

    // Debouncing variables
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Identify your app to OSM servers
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_seaching_origin_desti);

        map = findViewById(R.id.mapCommuter);
        searchOrigin = findViewById(R.id.searchorigin);
        searchDestination = findViewById(R.id.searchdestination);
        btnSuggestionRoutes = findViewById(R.id.btnSuggestionRoutes);

        setupMap();
        setupSearchSuggestions();

        // Origin Listeners with Debouncing
        searchOrigin.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { geocode(q, true); return true; }
            @Override public boolean onQueryTextChange(String n) {
                if (searchRunnable != null) mainHandler.removeCallbacks(searchRunnable);
                if (n.length() >= 3) {
                    searchRunnable = () -> fetchSuggestions(n, true);
                    mainHandler.postDelayed(searchRunnable, 600); // Wait 600ms
                }
                return true;
            }
        });

        // Destination Listeners with Debouncing
        searchDestination.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { geocode(q, false); return true; }
            @Override public boolean onQueryTextChange(String n) {
                if (searchRunnable != null) mainHandler.removeCallbacks(searchRunnable);
                if (n.length() >= 3) {
                    searchRunnable = () -> fetchSuggestions(n, false);
                    mainHandler.postDelayed(searchRunnable, 600); // Wait 600ms
                }
                return true;
            }
        });

        btnSuggestionRoutes.setOnClickListener(v -> showTransportDialog());

        // Nav logic
        findViewById(R.id.btnhomepage).setOnClickListener(v -> startActivity(new Intent(this, commuterhome.class)));
        findViewById(R.id.btnsetting).setOnClickListener(v -> startActivity(new Intent(this, Settings.class)));
        findViewById(R.id.btnsearch).setSelected(true);
    }

    private void fetchSuggestions(String text, boolean isOrigin) {
        executor.execute(() -> {
            try {
                // Unique provider ID
                NominatimPOIProvider poiProvider = new NominatimPOIProvider("LugarLang_Search_Provider");

                // Increased radius to 1.0 (approx 100km) to capture all of Cebu properly
                ArrayList<POI> pois = poiProvider.getPOICloseTo(new GeoPoint(10.3157, 123.8854), text, 8, 1.0);

                MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID, "place_name"});
                if (pois != null) {
                    for (int i = 0; i < pois.size(); i++) {
                        cursor.addRow(new Object[]{i, pois.get(i).mDescription});
                    }
                }

                mainHandler.post(() -> {
                    if (isOrigin) originAdapter.changeCursor(cursor);
                    else destAdapter.changeCursor(cursor);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void geocode(String name, boolean isOrigin) {
        executor.execute(() -> {
            try {
                Geocoder g = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = g.getFromLocationName(name + ", Cebu", 1);
                if (addrs != null && !addrs.isEmpty()) {
                    Address a = addrs.get(0);
                    GeoPoint p = new GeoPoint(a.getLatitude(), a.getLongitude());
                    mainHandler.post(() -> {
                        if (isOrigin) {
                            originPoint = p;
                            if (originMarker != null) map.getOverlays().remove(originMarker);
                            originMarker = new Marker(map);
                            originMarker.setPosition(p);
                            originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.trip_origin_24px));
                            map.getOverlays().add(originMarker);
                        } else {
                            destinationPoint = p;
                            if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
                            destinationMarker = new Marker(map);
                            destinationMarker.setPosition(p);
                            destinationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.location_on_24px));
                            map.getOverlays().add(destinationMarker);
                        }
                        map.getController().animateTo(p);
                        drawRoutePreview();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void drawRoutePreview() {
        if (originPoint != null && destinationPoint != null) {
            ArrayList<GeoPoint> points = new ArrayList<>();
            points.add(originPoint);
            points.add(destinationPoint);
            executor.execute(() -> {
                // Use a proper Road Manager
                OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
                Road road = roadManager.getRoad(points);
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

    private void setupSearchSuggestions() {
        String[] from = new String[]{"place_name"};
        int[] to = new int[]{android.R.id.text1};

        originAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        destAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        searchOrigin.setSuggestionsAdapter(originAdapter);
        searchDestination.setSuggestionsAdapter(destAdapter);

        SearchView.OnSuggestionListener sl = new SearchView.OnSuggestionListener() {
            @Override public boolean onSuggestionSelect(int p) { return false; }
            @Override public boolean onSuggestionClick(int p) {
                SearchView active = searchOrigin.hasFocus() ? searchOrigin : searchDestination;
                SimpleCursorAdapter adapter = searchOrigin.hasFocus() ? originAdapter : destAdapter;
                Cursor c = adapter.getCursor();
                if (c.moveToPosition(p)) {
                    active.setQuery(c.getString(c.getColumnIndexOrThrow("place_name")), true);
                }
                return true;
            }
        };
        searchOrigin.setOnSuggestionListener(sl);
        searchDestination.setOnSuggestionListener(sl);
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(10.3157, 123.8854));
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
        i.putExtra("ORIGIN_LAT", originPoint.getLatitude());
        i.putExtra("ORIGIN_LNG", originPoint.getLongitude());
        i.putExtra("DEST_LAT", destinationPoint.getLatitude());
        i.putExtra("DEST_LNG", destinationPoint.getLongitude());
        i.putExtra("ORIGIN_NAME", searchOrigin.getQuery().toString());
        i.putExtra("DEST_NAME", searchDestination.getQuery().toString());
        startActivity(i);
    }

    @Override protected void onResume() { super.onResume(); map.onResume(); }
    @Override protected void onPause() { super.onPause(); map.onPause(); }
}