package com.usc.lugarlangfinal;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.usc.lugarlangfinal.commuter.SeachingOriginDesti;

import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class commuterhome extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private LinearLayout btnHomePage, btnSearch, btnSetting;
    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private SearchView searchView;
    private SimpleCursorAdapter suggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid User Agent is required for map tile loading
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_commuterhome);

        // Initialize UI Elements
        map = findViewById(R.id.mapdisplay);
        searchView = findViewById(R.id.searchPlace);
        btnHomePage = findViewById(R.id.btnhomepage);
        btnSearch = findViewById(R.id.btnsearch);
        btnSetting = findViewById(R.id.btnsetting);

        btnHomePage.setSelected(true);

        // Navigation Listeners
        btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(commuterhome.this, SeachingOriginDesti.class));
        });

        btnSetting.setOnClickListener(v -> {
            // FIX: Explicitly use your local Settings activity
            startActivity(new Intent(commuterhome.this, com.usc.lugarlangfinal.Settings.class));
        });

        // Start Permission and GPS Sequence
        checkLocationPermissions();
        setupSearch();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkGpsEnabled();
        }
    }

    private void checkGpsEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {}

        if (!gpsEnabled) {
            new AlertDialog.Builder(this)
                    .setTitle("Location Services Disabled")
                    .setMessage("Please turn on your GPS to allow the app to find your current location.")
                    .setCancelable(false)
                    .setPositiveButton("Settings", (dialog, which) -> {
                        // FIX: Use the specific System Action to prevent ActivityNotFoundException
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        setupMap(); // Load map without GPS
                    })
                    .show();
        } else {
            setupMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGpsEnabled();
            } else {
                Toast.makeText(this, "Location permission denied. Map will not track you.", Toast.LENGTH_SHORT).show();
                setupMap();
            }
        }
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(10.3157, 123.8854)); // Cebu City

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            mLocationOverlay.enableMyLocation();
            mLocationOverlay.enableFollowLocation();
            map.getOverlays().add(mLocationOverlay);
        }
    }

    private void setupSearch() {
        String[] from = new String[]{"place_name"};
        int[] to = new int[]{android.R.id.text1};
        suggestionAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        searchView.setSuggestionsAdapter(suggestionAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchGlobalLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 3) {
                    fetchSuggestions(newText);
                }
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) { return false; }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = suggestionAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    String selection = cursor.getString(cursor.getColumnIndexOrThrow("place_name"));
                    searchView.setQuery(selection, true);
                }
                return true;
            }
        });
    }

    private void fetchSuggestions(String text) {
        final GeoPoint biasLocation = (mLocationOverlay != null && mLocationOverlay.getMyLocation() != null)
                ? mLocationOverlay.getMyLocation()
                : (GeoPoint) map.getMapCenter();

        new Thread(() -> {
            try {
                NominatimPOIProvider poiProvider = new NominatimPOIProvider(getPackageName());
                ArrayList<POI> pois = poiProvider.getPOICloseTo(biasLocation, text, 5, 0.1);

                MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID, "place_name"});
                if (pois != null) {
                    for (int i = 0; i < pois.size(); i++) {
                        cursor.addRow(new Object[]{i, pois.get(i).mDescription});
                    }
                }

                runOnUiThread(() -> suggestionAdapter.changeCursor(cursor));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void searchGlobalLocation(String locationName) {
        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(locationName + ", Cebu", 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    GeoPoint targetPoint = new GeoPoint(address.getLatitude(), address.getLongitude());

                    runOnUiThread(() -> {
                        map.getOverlays().removeIf(overlay -> overlay instanceof Marker);

                        Marker marker = new Marker(map);
                        marker.setPosition(targetPoint);
                        marker.setTitle(locationName);
                        map.getOverlays().add(marker);

                        map.getController().animateTo(targetPoint);
                        map.getController().setZoom(17.0);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }
}