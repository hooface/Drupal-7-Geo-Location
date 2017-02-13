package net.iplanet.Drupal7GeoLocation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import net.iplanet.drupal.v7.Authentication;
import net.iplanet.utils.Logger;
import net.iplanet.drupal.v7.Crud;
import net.iplanet.drupal.v7.Tokens;

import org.apache.http.HttpResponse;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
                                                              GoogleApiClient.ConnectionCallbacks,
                                                              GoogleApiClient.OnConnectionFailedListener,
                                                              LocationListener {
    //region Global Obj & Vars
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    Logger Log = new Logger();
    public String session_name;
    public String session_id;
    public String token;
    public String login;
    public boolean GPS_ENABLED = false;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            session_name  = extras.getString(Tokens.KEY_SESSION_NAME_RESPONSE);
            session_id  = extras.getString(Tokens.KEY_SESSION_ID_RESPONSE);
            token  = extras.getString(Tokens.KEY_TOKEN_RESPONSE);
            login  = extras.getString(Tokens.KEY_LOGIN);
        }
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
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        GPS_ENABLED = true;

        ImageView v = (ImageView)findViewById((R.id.GPS_STATUS));
        v.setImageResource(R.drawable.sateliteon);

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        TextView mLat = (TextView) findViewById(R.id.txtLatitude);
        TextView mLon = (TextView) findViewById(R.id.txtLongitude);

        mLat.setText("Latitude: " + latitude);
        mLon.setText("Longitude: " + longitude);

        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        try {
            String  data = "{\"title\":\"" + android.os.Build.MODEL + "\", \"type\": \"geo_localization\",\"field_longitude\": {\"und\":[{\"value\": \"" + longitude + "\"}]},\"field_latitude\": {\"und\": [{\"value\": \"" + latitude + "\"}]}}";
            new MapsActivity.clodSyncActivityTask().execute(session_name, session_id, token, Double.toString(longitude), Double.toString(latitude), Tokens.serv_end_Pnt_NODE, data);
        } catch (Exception ex) {
            v.setImageResource(R.drawable.sateliteoff);
            v = (ImageView)findViewById((R.id.TRANSMISION_STATUS));
            v.setImageResource(R.drawable.transmisonerror);
            Log.SyslogException(ex);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        GPS_ENABLED = true;
        ImageView v = (ImageView)findViewById((R.id.GPS_STATUS));
        v.setImageResource(R.drawable.sateliteon);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        GPS_ENABLED = false;
        ImageView v = (ImageView)findViewById((R.id.GPS_STATUS));
        v.setImageResource(R.drawable.sateliteoff);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        GPS_ENABLED = false;
        ImageView v = (ImageView)findViewById((R.id.GPS_STATUS));
        v.setImageResource(R.drawable.sateliteoff);
    }

    private class clodSyncActivityTask extends AsyncTask<String, Void, Integer> {

        protected Integer doInBackground(String... params) {
            try {
                String session_name_parm = params[0];
                String session_id_parm = params[1];
                String session_token_parm = params[2];
                String longitude_parm = params[3];
                String latitude_parm = params[4];
                String endpoint_parm = params[5];
                String query_parm = params[6];
                Crud CrudMngr = new Crud();
                ImageView v = (ImageView)findViewById((R.id.TRANSMISION_STATUS));
                v.setImageResource(R.drawable.transmisioninvoked);
                CrudMngr.post_Drupal(query_parm, session_name_parm, session_id_parm, session_token_parm);
            }catch (Exception ex){
                Log.SyslogException(ex);
            }
            return 0;
        }

        protected void onPostExecute(Integer result) {
            ImageView v = (ImageView)findViewById((R.id.TRANSMISION_STATUS));
            v.setImageResource(R.drawable.transmited);

        }
    }

    public void logout_click(View view){
        try {
            new logOutTask().execute();
        }catch (Exception ex) {
            Log.SyslogException(ex);
        }
    }

    private class logOutTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... params) {
            try {
                Authentication Auth = new  Authentication();
                HttpResponse response = Auth.doLogout(session_name, session_id, token);
            }catch (Exception ex) {
                Log.SyslogException(ex);
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            finish();
            Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
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

            // other 'case' lines to check for other permissions this app might request.
            //You can add here other case statements according to your requirement.
        }
    }



}
