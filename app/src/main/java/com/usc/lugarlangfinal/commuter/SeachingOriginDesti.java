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
import android.widget.LinearLayout;
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

import java.io.IOException;
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

    private SimpleCursorAdapter originAdapter, destAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_seaching_origin_desti);

        initViews();
        setupMap();
        setupSearchWithSuggestions();

        // UI state: highlight current tab
        btnSearch.setSelected(true);

        btnHomePage.setOnClickListener(v -> startActivity(new Intent(this, commuterhome.class)));
        btnSetting.setOnClickListener(v -> startActivity(new Intent(this, Settings.class)));
        btnSuggestionRoutes.setOnClickListener(v -> showTransportSelectionDialog());
    }

    private void initViews() {
        map = findViewById(R.id.mapCommuter);
        searchOrigin = findViewById(R.id.searchorigin);
        searchDestination = findViewById(R.id.searchdestination);
        btnSuggestionRoutes = findViewById(R.id.btnSuggestionRoutes);
        btnHomePage = findViewById(R.id.btnhomepage);
        btnSearch = findViewById(R.id.btnsearch);
        btnSetting = findViewById(R.id.btnsetting);
    }

    private void showTransportSelectionDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_transport_selection, null);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.optionBus).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            navigateToSuggestedRoutes("Bus");
        });

        view.findViewById(R.id.optionJeepney).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            navigateToSuggestedRoutes("Jeepney");
        });

        bottomSheetDialog.show();
    }

    private void navigateToSuggestedRoutes(String transportType) {
        if (originPoint == null || destinationPoint == null) {
            Toast.makeText(this, "Wait for the route to be calculated on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SuggestedRoutes.class);
        intent.putExtra("TRANSPORT_TYPE", transportType);

        // Safety check for query text
        String oName = searchOrigin.getQuery() != null ? searchOrigin.getQuery().toString() : "";
        String dName = searchDestination.getQuery() != null ? searchDestination.getQuery().toString() : "";

        intent.putExtra("ORIGIN_NAME", oName);
        intent.putExtra("DEST_NAME", dName);
        intent.putExtra("ORIGIN_LAT", originPoint.getLatitude());
        intent.putExtra("ORIGIN_LNG", originPoint.getLongitude());
        intent.putExtra("DEST_LAT", destinationPoint.getLatitude());
        intent.putExtra("DEST_LNG", destinationPoint.getLongitude());
        startActivity(intent);
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(14.0);
        map.getController().setCenter(new GeoPoint(10.3157, 123.8854)); // Cebu Default
    }

    private void setupSearchWithSuggestions() {
        String[] from = new String[]{"place_name"};
        int[] to = new int[]{android.R.id.text1};

        originAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        destAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        searchOrigin.setSuggestionsAdapter(originAdapter);
        searchDestination.setSuggestionsAdapter(destAdapter);

        setupListeners(searchOrigin, originAdapter, true);
        setupListeners(searchDestination, destAdapter, false);
    }

    private void setupListeners(SearchView searchView, SimpleCursorAdapter adapter, boolean isOrigin) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                geocodeLocation(query, isOrigin);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 3) fetchSuggestions(newText, adapter);
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int pos) { return false; }
            @Override
            public boolean onSuggestionClick(int pos) {
                Cursor cursor = adapter.getCursor();
                if (cursor.moveToPosition(pos)) {
                    String selection = cursor.getString(cursor.getColumnIndexOrThrow("place_name"));
                    searchView.setQuery(selection, true);
                }
                return true;
            }
        });
    }

    private void geocodeLocation(String name, boolean isOrigin) {
        mainHandler.post(() -> btnSuggestionRoutes.setVisibility(View.GONE));
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(name, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    GeoPoint point = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                    mainHandler.post(() -> {
                        if (isOrigin) {
                            originPoint = point;
                            updateMarker(true, point, "Start: " + name);
                        } else {
                            destinationPoint = point;
                            updateMarker(false, point, "End: " + name);
                        }
                        checkAndDrawRoute();
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        });
    }

    private void updateMarker(boolean isOrigin, GeoPoint point, String title) {
        if (isOrigin) {
            if (originMarker != null) map.getOverlays().remove(originMarker);
            originMarker = new Marker(map);
            originMarker.setPosition(point);
            originMarker.setTitle(title);
            originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.trip_origin_24px));
            map.getOverlays().add(originMarker);
        } else {
            if (destinationMarker != null) map.getOverlays().remove(destinationMarker);
            destinationMarker = new Marker(map);
            destinationMarker.setPosition(point);
            destinationMarker.setTitle(title);
            destinationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.location_on_24px));
            map.getOverlays().add(destinationMarker);
        }
        map.getController().animateTo(point);
        map.invalidate();
    }

    private void checkAndDrawRoute() {
        if (originPoint != null && destinationPoint != null) {
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(originPoint);
            waypoints.add(destinationPoint);

            executor.execute(() -> {
                OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
                Road road = roadManager.getRoad(waypoints);
                mainHandler.post(() -> {
                    if (road.mStatus == Road.STATUS_OK) {
                        if (currentRouteLine != null) map.getOverlays().remove(currentRouteLine);
                        currentRouteLine = RoadManager.buildRoadOverlay(road);
                        currentRouteLine.getOutlinePaint().setColor(Color.BLACK);
                        currentRouteLine.getOutlinePaint().setStrokeWidth(12.0f);
                        map.getOverlays().add(0, currentRouteLine);

                        btnSuggestionRoutes.setVisibility(View.VISIBLE);
                        map.zoomToBoundingBox(road.mBoundingBox, true, 150);
                        map.invalidate();
                    }
                });
            });
        }
    }

    private void fetchSuggestions(String text, SimpleCursorAdapter adapter) {
        GeoPoint center = (GeoPoint) map.getMapCenter();
        executor.execute(() -> {
            try {
                NominatimPOIProvider poiProvider = new NominatimPOIProvider(getPackageName());
                ArrayList<POI> pois = poiProvider.getPOICloseTo(center, text, 5, 0.2);
                MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID, "place_name"});
                if (pois != null) {
                    for (int i = 0; i < pois.size(); i++) {
                        cursor.addRow(new Object[]{i, pois.get(i).mDescription});
                    }
                }
                mainHandler.post(() -> adapter.changeCursor(cursor));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause() { super.onPause(); map.onPause(); }
}