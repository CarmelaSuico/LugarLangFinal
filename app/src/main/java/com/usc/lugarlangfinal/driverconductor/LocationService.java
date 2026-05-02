package com.usc.lugarlangfinal.driverconductor;

import android.app.*;
import android.content.*;
import android.location.Location;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;
import androidx.core.content.ContextCompat;
import android.util.Log;

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

        // High accuracy for real-time passenger tracking
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
            // Updating the "active" trip node so passengers can see the vehicle moving
            String coords = loc.getLatitude() + "," + loc.getLongitude();
            tripRef.child("currentLocation").setValue(coords)
                    .addOnFailureListener(e -> Log.e("LOC_SERVICE", "Failed to update location"));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String tripId = intent.getStringExtra("TRIP_ID");
            String franchise = intent.getStringExtra("FRANCHISE");

            // We update the active trip under the Franchise node
            if (tripId != null && franchise != null) {
                tripRef = FirebaseDatabase.getInstance(DB_URL)
                        .getReference("trips").child(franchise).child(tripId);
            }
        }

        Notification notification = createNotification();

        // Android 14 (API 34) and above requires the specific foreground type
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
            Log.e("LOC_SERVICE", "Location permission not granted");
        }
    }

    private Notification createNotification() {
        String channelId = "loc_channel";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "LugarLang Trip Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Tapping the notification takes the driver back to the active map
        Intent notificationIntent = new Intent(this, StartEndTrip.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("LugarLang Live")
                .setContentText("Your location is being shared with passengers.")
                .setSmallIcon(R.drawable.navigation_24px)
                .setColor(ContextCompat.getColor(this, R.color.lugar_lang_blue))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        // Clean up location updates when the service is stopped
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }
}