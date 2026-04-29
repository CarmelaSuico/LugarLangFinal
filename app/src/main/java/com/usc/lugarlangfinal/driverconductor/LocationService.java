package com.usc.lugarlangfinal.driverconductor;

import android.app.*;
import android.content.*;
import android.location.Location;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;

import com.google.android.gms.location.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.usc.lugarlangfinal.R;

public class LocationService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private DatabaseReference tripRef;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateFirebase(location);
                }
            }
        };
    }

    private void updateFirebase(Location loc) {
        if (tripRef != null) {
            String coords = loc.getLatitude() + "," + loc.getLongitude();
            tripRef.child("currentLocation").setValue(coords);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String tripId = intent.getStringExtra("TRIP_ID");
            String franchise = intent.getStringExtra("FRANCHISE");

            if (tripId != null && franchise != null) {
                tripRef = FirebaseDatabase.getInstance(DB_URL)
                        .getReference("trips").child(franchise).child(tripId);
            }
        }

        Notification notification = createNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private Notification createNotification() {
        String channelId = "loc_channel";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the Channel for Android 8.0+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Trip Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Active location tracking for drivers");
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Tap notification to go back to the Trip screen
        Intent notificationIntent = new Intent(this, StartEndTrip.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("LugarLang Live")
                .setContentText("Driver location tracking is active.")
                .setSmallIcon(R.drawable.trip_origin_24px)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }
}
