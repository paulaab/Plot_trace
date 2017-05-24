package com.vodafone.innogaragepb.geomapp;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;

import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Iterator;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMarkerClickListener {
    private JSONObject jsonObj;
    TextView temp;
    private GoogleMap mMap;
    public Boolean ready;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Marker mCurrentLocation;
    Location mLastLocation;
    public float minValue;
    public float acceptedValue;
    Context mContext = this;

    private Handler handler = new Handler();
    public ListView msgView;
    public ArrayAdapter<String> msgList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
//Obtain the cell ID

        /*TelephonyManager tm =(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        GsmCellLocation cellLocation = (GsmCellLocation)tm.getCellLocation();
        int cellID = cellLocation.getCid() & 0xFFFF;
*/
// Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*---------------List of messages initialization - MSG-----------------*/
        msgView = (ListView) findViewById(R.id.listView);
        msgList = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        msgView.setAdapter(msgList);

        final Button resultsButton = (Button) findViewById(R.id.resultsButton);
        final Button settingsButton = (Button) findViewById(R.id.settingsButton);
        //DEFAULT VALUES

        acceptedValue = -65;
        minValue = -90;


        final String jsonString = loadJSONFromAsset("DataJoined.json");

        try {
            jsonObj = new JSONObject(jsonString);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Error loading JSON files");
        }


        resultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    setMarkers(jsonObj);
                } catch (JSONException e) {
                    System.out.println("Could not set markers");
                }
            }
        });


        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showSettingsDialog();
            }
        });








    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Initialize Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        ready = true;

    }

    public void setMarkers(JSONObject mJson) throws JSONException {
        for (int i = 0; i < mJson.length(); i++) {
            try {
                final JSONObject dataset = mJson.getJSONObject(Integer.toString(i));

                final Double latitude = dataset.getDouble("latitude");
                final Double longitude = dataset.getDouble("longitude");


                final Double RSRQ = dataset.getDouble("RSRQ (dB)");
                final Double SINR = dataset.getDouble("SINR (dB)");
                final Double RSRP = dataset.getJSONObject("PCC RxD RSSI").getDouble("RSRP (dBm)");
                final String color;
                final int number = i;




                if (RSRP <= minValue) {
                    color = "red";
                } else if (RSRQ > minValue && RSRP < acceptedValue) {
                    color = "yellow";
                } else {
                    color = "green";
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        setCoordinates(latitude, longitude, RSRP, SINR, RSRQ, color, number);


                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }


    private void setCoordinates(Double lat, Double lon, Double rsrp, Double sinr, Double rsrq, String color, int number) {

        int id = getResources().getIdentifier(color, "drawable", getPackageName());
        LatLng myPosition = new LatLng(lat, lon);

        mMap.addMarker(new MarkerOptions()
                        .position(myPosition)
                        .icon(BitmapDescriptorFactory.fromResource(id))
                        .title("LTE PEGEL")
                        .snippet("RSRP (dBm): " + rsrp+"\n"+"SINR(dB): "+sinr+"\n"+"RSRQ (dB): "+rsrq)
                //.snippet("Value RSRQ: " + rsrq)

        ).setTag(number);
        mMap.setOnMarkerClickListener(this);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter(){
                                      @Override
                                      public View getInfoWindow(Marker arg0) {
                                          return null;
                                      }

                                      @Override
                                      public View getInfoContents(Marker marker) {


                                          LinearLayout info = new LinearLayout(mContext);
                                          info.setOrientation(LinearLayout.VERTICAL);

                                          TextView title = new TextView(mContext);
                                          title.setTextColor(Color.BLACK);
                                          title.setGravity(Gravity.CENTER);
                                          title.setTypeface(null, Typeface.BOLD);
                                          title.setText(marker.getTitle());

                                          TextView snippet = new TextView(mContext);
                                          snippet.setTextColor(Color.GRAY);
                                          snippet.setText(marker.getSnippet());

                                          info.addView(title);
                                          info.addView(snippet);

                                          return info;
                                      }
                                      });


    }


//Customize characteristics of the markers: Size and time to fade


    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrentLocation != null) {
            mCurrentLocation.remove();
        }
        //Place my location Marker
        LatLng latlong = new LatLng(location.getLatitude(), location.getLongitude());

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlong));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission Granted
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    /*------------------Reading JSON Data------------------------*/
    public String loadJSONFromAsset(String fileName) {
        String json = null;
        try {

            InputStream is = getAssets().open(fileName);


            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }


    public void setValues(JSONObject jobj, Marker marker) throws JSONException {
        Integer tag = (Integer) marker.getTag();
        JSONObject dataset = jobj.getJSONObject(String.valueOf(tag));
        Iterator<?> keys = dataset.keys();

        while(keys.hasNext()){
            String mKey = (String)keys.next();
            String mKey2 = mKey.replace(" ","_");
            int id = getResources().getIdentifier(mKey2, "id", getPackageName());
            switch (mKey){
                case "IntraFreq":
                    break;
                case "InterFreq":
                    break;
                case "PCC RxD RSSI":
                    break;
                case "PCC RxM RSSI":
                    break;
                case "Serving":
                    break;
                case "sats":
                    break;
                default:
                    displayMsg(mKey + ": "+dataset.getString(mKey));
                    //temp.setText(mKey + ": "+dataset.getString(mKey));
                    break;
            }
        }
    }


    public void displayMsg(String msg) {

            final String data = msg;

            handler.post(new Runnable() {
                @Override
                public void run() {

                    msgList.add(data);
                    msgView.setAdapter(msgList);
                    msgView.smoothScrollToPosition(msgList.getCount() - 1);
                }
            });
        }

    @Override
    public boolean onMarkerClick(Marker marker) {
        msgList.clear();
        try {
            setValues(jsonObj,marker);
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return false;
    }







    public void showSettingsDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        final View dView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dView);
        final EditText minimumInput =(EditText) dView.findViewById(R.id.minimumInput);
        final EditText normalInput =(EditText) dView.findViewById(R.id.normalInput);


        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

                // Add action buttons
        builder        .setPositiveButton(R.string.continues, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        acceptedValue = Float.parseFloat(normalInput.getText().toString());
                        minValue = Float.parseFloat(minimumInput.getText().toString());
                        try {
                            setMarkers(jsonObj);
                        } catch (JSONException e) {
                            System.out.println("Could not set markers");
                        }

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog ad = builder.create();
        ad.show();



    }
}








//http://stackoverflow.com/questions/28109597/gradually-fade-out-a-custom-map-marker