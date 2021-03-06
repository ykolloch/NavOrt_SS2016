package eu.de.bw.hn.consizemaps;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class
MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    private final static String TAG = "MainActivity";
    private MapView map;
    private TextView latitudeView, longitudeView, velocityView, altitudeView;
    private Button downloadButton, followButton;
    private NumberPicker numberPicker;
    private static final boolean DEBUG = true;
    private int textViewBackgroundColor = Color.argb(150, 0, 0, 0);
    private int numberPickerBackgroundColor = Color.argb(150, 255, 255, 255);
    private boolean follow = true;
    private int downloadZoomScale = 2;
    private int oldVelocity = 0;
    private double oldAltitude = 0, oldBearing;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPQUESTOSM);
        //map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setUseDataConnection(false);

        map.setTileSource(TileSourceFactory.MAPQUESTOSM);


        IMapController mapController = map.getController();
        mapController.setCenter(new GeoPoint(49.122244, 9.211033));
        mapController.setZoom(17);


        LocationManager locationManager;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Needed permissions not granted! App can't work properly!", Toast.LENGTH_SHORT).show();
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        // Für eigene location overlay
        MyLocationNewOverlay myLocationNewOverlay = new MyLocationNewOverlay(this, map);
        myLocationNewOverlay.enableMyLocation();
        map.getOverlays().add(myLocationNewOverlay);
        // Für Kompass overlay
        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        initVariables();

    }

    private void initVariables() {
        latitudeView = (TextView) findViewById(R.id.latitude);
        latitudeView.setBackgroundColor(textViewBackgroundColor);
        longitudeView = (TextView) findViewById(R.id.longitude);
        longitudeView.setBackgroundColor(textViewBackgroundColor);
        downloadButton = (Button) findViewById(R.id.download);
        downloadButton.setOnClickListener(this);
        followButton = (Button) findViewById(R.id.follow);
        followButton.setOnClickListener(this);
        numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(18);
        numberPicker.setValue(2);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        numberPicker.setBackgroundColor(numberPickerBackgroundColor);
        velocityView = (TextView) findViewById(R.id.velocityView);
        altitudeView = (TextView) findViewById(R.id.altitudeView);
    }

    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = "osmdroid permissions:";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\nLocation to show user location.";
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\nStorage access to store map tiles.";
        }
        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    private void download() {

        final Context context = this;

        downloadZoomScale = numberPicker.getValue();

        int minZoomScale = map.getZoomLevel() - downloadZoomScale;
        if (minZoomScale < 0) {
            minZoomScale = 0;
        }
        int maxZoomScale = map.getZoomLevel() + downloadZoomScale;

        if (maxZoomScale > 18) {
            maxZoomScale = 18;
        }
        final int minZoom = minZoomScale, maxZoom = maxZoomScale;

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Download Map Tiles");
        alertDialog.setMessage("This will download the map tiles for the current field of view, for zoom levels " +
                minZoom +
                "-" + maxZoom +
                "\n\nCurrent zoom level is: " + map.getZoomLevel() +
                "\nDownload Zoom Scale is: " + downloadZoomScale + "\n(adjust at the bottom right)");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        CacheManager cacheManager = new CacheManager(map);
                        cacheManager.downloadAreaAsync(context, map.getBoundingBox(), minZoom, maxZoom);
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {

        currentLocation = location;

        if (DEBUG) {
            Log.d(TAG, "onLocationChanged Lat: " + location.getLatitude() + " | Long: " + location.getLongitude());
            Log.d(TAG, "Bearing: " + location.getBearing());
            Log.d(TAG, "Altitude: " + location.getAltitude());
        }
//        Marker marker = new Marker(location.getLatitude(), location.getLongitude());
        int velocity = (int) ((location.getSpeed() * 3600) / 1000);
        if (oldVelocity != velocity) {
            if (DEBUG)
                Log.d(TAG, "Current velocity: " + velocity + " km/h");
            oldVelocity = velocity;
            velocityView.setText(String.valueOf(velocity));
        }

        if (oldAltitude != location.getAltitude()) {
            oldAltitude = location.getAltitude();
            altitudeView.setText(String.valueOf((int) location.getAltitude()));
        }

        if (follow) {
            map.getController().setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
        }
        //map.getController().setZoom(17);
        latitudeView.setText("Lat:   " + String.valueOf(location.getLatitude()));
        longitudeView.setText("Long: " + String.valueOf(location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (DEBUG)
            Log.d(TAG, "onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (DEBUG)
            Log.d(TAG, "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (DEBUG)
            Log.d(TAG, "onProviderDisabled");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.download):
                download();
                break;
            case (R.id.follow):
                if (follow) {
                    follow = false;
                    Toast.makeText(this, "Following user position disabled.", Toast.LENGTH_SHORT).show();
                    followButton.setBackgroundResource(R.drawable.follow1);
                } else {
                    follow = true;
                    Toast.makeText(this, "Following user position enabled.", Toast.LENGTH_SHORT).show();
                    followButton.setBackgroundResource(R.drawable.follow2);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (DEBUG)
                Log.d(TAG, "GPS Info pressed!");
            if (currentLocation != null) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("GPS Info");
                alertDialog.setMessage("Latitude: " + currentLocation.getLatitude() + "\n\nLongitude: "
                        + currentLocation.getLongitude() + "\n\nAltitude: " + (int) currentLocation.getAltitude() + " m" +
                        "\n\nVelocity: " + ((int) currentLocation.getSpeed() * 3.6) + " km/h" +
                        "\n\nGPS bearing: " + currentLocation.getBearing() + "°");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else {
                Toast.makeText(this, "No GPS connection!", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
