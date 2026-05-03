package com.usc.lugarlangfinal.driverconductor;

import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.*;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.R;

public class LocationService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference tripRef;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location loc : locationResult.getLocations()) {
                    if (tripRef != null) {
                        tripRef.child("currentLocation").setValue(loc.getLatitude() + "," + loc.getLongitude());
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String tripId = intent.getStringExtra("TRIP_ID");
        String franchise = intent.getStringExtra("FRANCHISE");
        if (tripId != null && franchise != null) {
            tripRef = FirebaseDatabase.getInstance().getReference("trips").child(franchise).child(tripId);
        }

        createChannel();
        Notification n = new NotificationCompat.Builder(this, "loc_channel")
                .setContentTitle("LugarLang Live Tracking")
                .setContentText("Sharing your live location with passengers.")
                .setSmallIcon(R.drawable.navigation_24px)
                .setOngoing(true).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, n);
        }

        startUpdates();
        return START_STICKY;
    }

    private void startUpdates() {
        LocationRequest r = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        try { fusedLocationClient.requestLocationUpdates(r, locationCallback, Looper.getMainLooper()); } catch (SecurityException ignored) {}
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel("loc_channel", "Tracking", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    @Override public void onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}