package com.usc.lugarlangfinal;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

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

    LinearLayout btnHomePage, btnSearch, btnSetting;

    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private SearchView searchView;
    private SimpleCursorAdapter suggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_commuterhome);

        map = findViewById(R.id.mapdisplay);
        searchView = findViewById(R.id.searchPlace);

        btnHomePage = findViewById(R.id.btnhomepage);
        btnSearch = findViewById(R.id.btnsearch);
        btnSetting = findViewById(R.id.btnsetting);

        btnHomePage.setSelected(true);

        btnSetting.setOnClickListener(v -> {
            startActivity(new Intent(commuterhome.this, Settings.class));
        });


        setupMap();
        setupSearch();
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    private void setupSearch() {
        // 1. Initialize the adapter for suggestions
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
                // 2. Only fetch suggestions if user typed 3 or more characters
                if (newText.length() >= 3) {
                    fetchSuggestions(newText);
                }
                return true;
            }
        });

        // 3. Handle clicking a suggestion from the dropdown
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) { return false; }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = suggestionAdapter.getCursor();
                if (cursor.moveToPosition(position)) {
                    int index = cursor.getColumnIndexOrThrow("place_name");
                    String selection = cursor.getString(index);
                    searchView.setQuery(selection, true); // Submits the search
                }
                return true;
            }
        });
    }

    private void fetchSuggestions(String text) {
        new Thread(() -> {
            try {
                NominatimPOIProvider poiProvider = new NominatimPOIProvider(getPackageName());
                GeoPoint currentPos = mLocationOverlay.getMyLocation();

                // Fetch up to 5 potential matches near the commuter
                // Fixed: getPOIsAround replaced with getPOICloseTo which is available in osmbonuspack 6.9.0
                ArrayList<POI> pois = poiProvider.getPOICloseTo(currentPos, text, 5, 0.1);

                // Create a MatrixCursor to pass data to the SimpleCursorAdapter
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
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address address = addresses.get(0);
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
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
