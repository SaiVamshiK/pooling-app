package com.example.miniproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class UsersGoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback,

        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener{
    public boolean i=false;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    Location lastlocation;
    LocationRequest locationRequest;
    private FirebaseAuth mAuth;
    private FirebaseUser currentuser;
    LatLng customerpickuplocation;
    private int radius=1;
    boolean driverfound=false;
    String driverfoundId;
    String customerid;

    private  DatabaseReference driveravailableref;
    private Button userlogoutbutton,usercallacabbutton;

    private DatabaseReference driverworkingrefence;

    Marker drivermarker;

    DatabaseReference driverreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_google_maps);


        mAuth=FirebaseAuth.getInstance();
        currentuser=mAuth.getCurrentUser();
        customerid=FirebaseAuth.getInstance().getCurrentUser().getUid();

        userlogoutbutton=(Button)findViewById(R.id.button);
        usercallacabbutton=(Button)findViewById(R.id.callacabbutton);
        driveravailableref=FirebaseDatabase.getInstance().getReference().child("Driver's Available");
        driverworkingrefence=FirebaseDatabase.getInstance().getReference().child("Driver's Working");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        userlogoutbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(i==true)
                {
                    String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference customerrequestsref=FirebaseDatabase.getInstance().getReference().child("Customer's Requests").child(userID);
                    customerrequestsref.removeValue();
                }
                mAuth.signOut();
                Intent i=new Intent(UsersGoogleMapsActivity.this,MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                finish();
                startActivity(i);

            }
        });

        usercallacabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i=true;
                String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference customerrequestsref=FirebaseDatabase.getInstance().getReference().child("Customer's Requests");
                GeoFire geoFire=new GeoFire(customerrequestsref);
                geoFire.setLocation(userID, new GeoLocation(lastlocation.getLatitude(), lastlocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });
                customerpickuplocation=new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(customerpickuplocation).title("Pick Up from here"));


                usercallacabbutton.setText("Getting your Driver!!");
               GetClosestDriverCab();


            }
        });


    }

    private void GetClosestDriverCab()
    {
        GeoFire geoFire=new GeoFire(driveravailableref);
        GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(customerpickuplocation.latitude,customerpickuplocation.longitude),radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverfound)
                {
                    driverfound=true;
                    driverfoundId=key;


                    driverreference=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverfoundId);

                    HashMap drivermap=new HashMap();
                    drivermap.put("Your Customer",customerid);
                    driverreference.updateChildren(drivermap);

                    usercallacabbutton.setText("Looking For Driver Location");
                    GettingYourDriverLocation();

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if(!driverfound)
                {
                    radius+=1;
                    GetClosestDriverCab();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void GettingYourDriverLocation() {


        driverworkingrefence.child(driverfoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            List<Object> driverlocation=(List<Object>)dataSnapshot.getValue();
                            double locationlat=0,locationlng=0;
                            usercallacabbutton.setText("Driver Found");
                            if(driverlocation.get(0)!=null)
                            {
                                locationlat=Double.parseDouble(driverlocation.get(0).toString());
                            }
                            if(driverlocation.get(1)!=null)
                            {
                                locationlng=Double.parseDouble(driverlocation.get(1).toString());
                            }
                            LatLng drivermarkerlatlng=new LatLng(locationlat,locationlng);
                            if(drivermarker!=null)
                            {
                                drivermarker.remove();
                            }
                            drivermarker=  mMap.addMarker(new MarkerOptions().position(drivermarkerlatlng).title("Your Driver Here"));

                            Location loc1=new Location("");
                            loc1.setLatitude(customerpickuplocation.latitude);
                            loc1.setLongitude(customerpickuplocation.longitude);

                            Location loc2=new Location("");
                            loc2.setLatitude(drivermarkerlatlng.latitude);
                            loc2.setLongitude(drivermarkerlatlng.longitude);
                            float dist=loc1.distanceTo(loc2);

                            usercallacabbutton.setText("Driver At "+ String.valueOf(dist));

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

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
        lastlocation=location;
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
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
    protected void onStop() {
        super.onStop();
         if(i==true)
         {
             String userID=FirebaseAuth.getInstance().getCurrentUser().getUid();
             DatabaseReference customerrequestsref=FirebaseDatabase.getInstance().getReference().child("Customer's Requests").child(userID);
             customerrequestsref.removeValue();
         }
         mAuth.signOut();
         Intent i=new Intent(UsersGoogleMapsActivity.this,MainActivity.class);
         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
         finish();
         startActivity(i);

    }

}
