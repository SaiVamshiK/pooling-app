package com.example.miniproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.GoogleApiAvailabilityCache;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.List;
import java.util.Map;

public class DriversGoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener {


    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    Location lastlocation;
    LocationRequest locationRequest;
    private Button LogoutDriverButton,SettingsDriverButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentuser;
    private DatabaseReference assignedcustref,assignedcustpickupref;


    private String driverid,custid="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_google_maps);

        mAuth=FirebaseAuth.getInstance();
        currentuser=mAuth.getCurrentUser();
        driverid=mAuth.getCurrentUser().getUid();

        LogoutDriverButton=(Button)findViewById(R.id.driverlogoutbutton);
        SettingsDriverButton=(Button)findViewById(R.id.driversettingsbutton);
        final SupportMapFragment mapFragment=(SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        LogoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference driverremovallocation=FirebaseDatabase.getInstance().getReference().child("Driver's Available").child(userID);
                driverremovallocation.removeValue();
                mAuth.signOut();

                Intent i=new Intent(DriversGoogleMapsActivity.this,UserDriverActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                finish();
                startActivity(i);

            }
        });

        GetAssignedCustomerRequest();


    }

    private void GetAssignedCustomerRequest() {

        assignedcustref=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverid);

        assignedcustref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                        Map<String,Object> map=(Map<String, Object>)dataSnapshot.getValue();
                        if(map.get("Your Customer")!=null)
                        {
                            custid=map.get("Your Customer").toString();
                            getAssignedCustomerPickupLocation();
                        }
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getAssignedCustomerPickupLocation() {
        assignedcustref=FirebaseDatabase.getInstance().getReference().child("").child("Riders").child(driverid);

        assignedcustref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    Map<String,Object> map=(Map<String, Object>)dataSnapshot.getValue();
                    if(map.get("Your Customer")!=null)
                    {
                        custid=map.get("Your Customer").toString();
                        getAssignedCustomerPickupLocation();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

  /*      assignedcustpickupref=FirebaseDatabase.getInstance().getReference().child("Customer's Requests").child(custid).child("l");
        assignedcustpickupref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    List<Object> custlocationmap=(List<Object>)dataSnapshot.getValue();

                    double locationlat=0,locationlng=0;
                    if(custlocationmap.get(0)!=null)
                    {
                        locationlat=Double.parseDouble(custlocationmap.get(0).toString());
                    }
                    if(custlocationmap.get(1)!=null)
                    {
                        locationlng=Double.parseDouble(custlocationmap.get(1).toString());
                    }
                    LatLng drivermarkerlatlng=new LatLng(locationlat,locationlng);
                    mMap.addMarker(new MarkerOptions().position(drivermarkerlatlng).title("Your Customer Here"));


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
*/
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleAPiClient();
        mMap.setMyLocationEnabled(true);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest=new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,locationRequest,this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

            if(getApplicationContext()!=null)
            {
                lastlocation=location;
                LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
                Toast.makeText(this, "Location Updating=("+location.getLatitude()+","+location.getLongitude()+")", Toast.LENGTH_SHORT).show();
                String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();

/*
        DatabaseReference driveravailableref=FirebaseDatabase.getInstance().getReference().child("Driver Available").child(userID);
        LocationCustomSaving obj=new LocationCustomSaving(location.getLatitude(),location.getLongitude());
        driveravailableref.setValue(obj);
        */
                DatabaseReference driveravailabilityreference=FirebaseDatabase.getInstance().getReference().child("Driver's Available");
                GeoFire geoFireavailability=new GeoFire(driveravailabilityreference);


                DatabaseReference driverworkingref=FirebaseDatabase.getInstance().getReference().child("Driver's Working");
                GeoFire geoFireworking=new GeoFire(driverworkingref);



                switch (custid)
                {
                    case "":
                        geoFireworking.removeLocation(userID);
                        geoFireavailability.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });
                        break;
                    default:
                        geoFireavailability.removeLocation(userID);
                        geoFireworking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });
                        break;
                }

            }

    }

    protected synchronized void buildGoogleAPiClient()
    {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient!=null)
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverremovallocation=FirebaseDatabase.getInstance().getReference().child("Driver's Available").child(userID);
        driverremovallocation.removeValue();

        mAuth.signOut();

        Intent i=new Intent(DriversGoogleMapsActivity.this,UserDriverActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(i);


    }

}
