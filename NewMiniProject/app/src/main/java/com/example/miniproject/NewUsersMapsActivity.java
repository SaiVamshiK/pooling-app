package com.example.miniproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewUsersMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLoc;
    private LocationManager mLocManager;
    private LocationRequest mLocReq;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 2000;
    private long FASTEST_INTERVAL = 5000;
    private LocationManager locManager;
    private LatLng latLng;
    private boolean isPermission;



    Marker over;
    Intent starterIntent;


    //new
    private Button custlogoutbtn,custsettingsbtn;
    private FirebaseAuth mAuth;
    private FirebaseUser currentuser;
    private Button callacabbtn;
    String customerID;
    private DatabaseReference customeerdatabaseref;
    private LatLng customerpickuplocation;
    private DatabaseReference driveravailableref;
    private int radius=1;
    private Boolean driverfound=false;
    private String driverfoundID;
    private DatabaseReference driverassigneddataref;
    private DatabaseReference driverlocationref;
    Marker driverMarker;

    //new
    GeoQuery geoQuery;
    Boolean requesttype=false;
    private ValueEventListener driverlocationreflistener;

    //new
    private TextView drivernametext,drivervechiclenotext;
    private CircleImageView profilepic;
    private RelativeLayout relativeLayout;

    //new
    private int load;
    int existingload;
    DatabaseReference assigneddriverloadref;
    private int i = 0;
    Location driverlocation;
    private int index=0;
    private static final int REQUEST_CALL=1;
    private String yourdrivernumber;
    ImageView imagecall;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_users_maps);
        if (requestSinglePermission()) {


            mAuth=FirebaseAuth.getInstance();
            currentuser=mAuth.getCurrentUser();
            custlogoutbtn=(Button)findViewById(R.id.newuserlogoutbutton);
            custsettingsbtn=(Button)findViewById(R.id.newusersettingsbutton);
            callacabbtn=(Button)findViewById(R.id.newusercallacabbutton);
            customerID=FirebaseAuth.getInstance().getCurrentUser().getUid();
            customeerdatabaseref= FirebaseDatabase.getInstance().getReference().child("Customer's Requests");
            driveravailableref=FirebaseDatabase.getInstance().getReference().child("Driver's Available");
            driverlocationref=FirebaseDatabase.getInstance().getReference().child("Driver's Working");
            assigneddriverloadref=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load");

            drivernametext=(TextView)findViewById(R.id.drivernameinusersmapactivity);
            drivervechiclenotext=(TextView)findViewById(R.id.vechiclenoinusermapactitvity);
            profilepic=(CircleImageView)findViewById(R.id.newprofileimageiconforusersinmapsactivity);
            relativeLayout=(RelativeLayout)findViewById(R.id.rell);
            imagecall=(ImageView)findViewById(R.id.phoneiconinusermapactivity);
            starterIntent=getIntent();



            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter your load");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    load = Integer.parseInt(input.getText().toString());
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
            imagecall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makephonecall();
                }
            });

            custsettingsbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i=new Intent(NewUsersMapsActivity.this,NewSettingsActivityforUsers.class);
                    startActivity(i);
                }
            });
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mLocManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            checkLocation();
            custlogoutbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAuth.signOut();
                    Intent i=new Intent(NewUsersMapsActivity.this,MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }
            });
            callacabbtn.setOnClickListener(new View.OnClickListener() {

                @Override

                public void onClick(View v) {
                    index++;
                    if(requesttype)
                    {
                        final String tempid=driverfoundID;
                        String custid=FirebaseAuth.getInstance().getCurrentUser().getUid();
                        //GeoFire geo=new GeoFire(customeerdatabaseref);
                        //geo.removeLocation(custid);
                        if(index%2==0) {
                            DatabaseReference custremovalref = FirebaseDatabase.getInstance().getReference().child("Customer's Requests");
                            custremovalref.removeValue();
                            DatabaseReference custremovalref1 = FirebaseDatabase.getInstance().getReference().child("Customer's Requests Load");
                            custremovalref1.removeValue();
                        }
                        final DatabaseReference driveravailability=FirebaseDatabase.getInstance().getReference().child("Driver's Available");
                        GeoFire geoFireavailable=new GeoFire(driveravailability);
                        final boolean[] j = {false};
                        final int[] getfull = new int[1];

                        final DatabaseReference driverworking=FirebaseDatabase.getInstance().getReference().child("Driver's Working");
                        final GeoFire geoFireworking=new GeoFire(driverworking);
                        DatabaseReference checkworkingref=FirebaseDatabase.getInstance().getReference();
                        checkworkingref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.hasChild("Driver's Working"))
                                {
                                    driverworking.child(tempid).removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                        geoFireavailable.setLocation(tempid, new GeoLocation(driverlocation.getLatitude(), driverlocation.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });
                        requesttype=false;
                        geoQuery.removeAllListeners();
                        driverlocationref.removeEventListener(driverlocationreflistener);

                        if(driverfound!=null)
                        {
                            driverassigneddataref=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverfoundID);
                 //         driverassigneddataref.setValue(true);
                            driverassigneddataref.child("Your Customer ID").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists())
                                    {
                                        driverassigneddataref.child("Your Customer ID").removeValue();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            driverfoundID=null;
                        }
                        driverfound=false;
                        radius=1;

                        if(driverMarker!=null)
                        {
                            driverMarker.remove();
                        }
                        callacabbtn.setText("Call a cab");



                        DatabaseReference checkwhetherwokingisthere=FirebaseDatabase.getInstance().getReference();
                        checkwhetherwokingisthere.child("Driver's Available").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) {
                                    j[0] = true;
                                    DatabaseReference driveravailableload1=FirebaseDatabase.getInstance().getReference().child("Driver's Available Full Load").child(tempid).child("Full Load");
                                    driveravailableload1.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                            {
                                                getfull[0] =Integer.parseInt(dataSnapshot.getValue().toString());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    DatabaseReference driveravailableload = FirebaseDatabase.getInstance().getReference().child("Driver's Available Load").child(tempid).child("Load");
                                    Log.i("Get full=",String.valueOf(getfull[0]));
                                    Log.i("load",String.valueOf(load));
                                    driveravailableload.setValue(getfull[0]-load);
                                }
                                else
                                {
                                    j[0]=false;
                                    final int[] x = new int[1];
                                    DatabaseReference driveravailableload1=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load").child(tempid);
                                    driveravailableload1.child("Load").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                            {
                                                x[0] =Integer.parseInt(dataSnapshot.getValue().toString());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    driveravailableload1.child("Load").setValue(x[0]+load);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        relativeLayout.setVisibility(View.GONE);
                    }
                    else
                    {
                        requesttype=true;
                        GeoFire geoFire=new GeoFire(customeerdatabaseref);
                        geoFire.setLocation(customerID, new GeoLocation(mLoc.getLatitude(), mLoc.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });
                        DatabaseReference customerrequestloadref=FirebaseDatabase.getInstance().getReference().child("Customer's Requests Load").child(customerID).child("Load");
                        customerrequestloadref.setValue(load);
                        customerpickuplocation=new LatLng(mLoc.getLatitude(),mLoc.getLongitude());
                        over=mMap.addMarker(new MarkerOptions().position(customerpickuplocation).title("Your're here").icon(BitmapDescriptorFactory.fromResource(R.drawable.usericon)));
                        GetClosestDriverCab();
                        //callacabbtn.setText("Getting your driver");
                    }
                }
            });

        }

    }
    private void comeout()
    {
        System.exit(1);
    }
    private void makephonecall()
    {
        DatabaseReference phonereference=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverfoundID).child("Phone");
        phonereference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    yourdrivernumber=(String) dataSnapshot.getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        Intent intentcall=new Intent(Intent.ACTION_CALL);
        if(false)
        {
            Toast.makeText(this, "No Number Available", Toast.LENGTH_SHORT).show();
        }
        else
        {
            intentcall.setData(Uri.parse("tel:"+"+91"+yourdrivernumber));
        }
        if(ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "Grant Permission", Toast.LENGTH_SHORT).show();
            requestpermission();
        }
        else {
            startActivity(intentcall);
        }
        /*
        if(yourdrivernumber.length()>0)
        {
            if(ContextCompat.checkSelfPermission(NewUsersMapsActivity.this,
                    Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(NewUsersMapsActivity.this,
                        new String[] {Manifest.permission.CALL_PHONE},REQUEST_CALL);
            }
            else
            {
                String dial="tel:"+yourdrivernumber;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }
        }
        else
        {
            Toast.makeText(this, "No Number Available", Toast.LENGTH_SHORT).show();
        }
        */
    }
    private void requestpermission(){
        ActivityCompat.requestPermissions(NewUsersMapsActivity.this,new String[]{Manifest.permission.CALL_PHONE},1);
    }
    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==REQUEST_CALL){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                makephonecall();
            }
            else
            {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    */

    private void GetClosestDriverCab() {
        GeoFire geoFire=new GeoFire(driveravailableref);
        geoQuery=geoFire.queryAtLocation(new GeoLocation(customerpickuplocation.latitude,customerpickuplocation.longitude),radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverfound&&requesttype)
                {
                    driverfound=true;
                    driverfoundID=key;
                    //to get driver load
                    assigneddriverloadref.child(driverfoundID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists())
                            {

                                existingload=Integer.parseInt(dataSnapshot.child("Load").getValue().toString());
                                if(existingload-load<0)
                                {
                                    Toast.makeText(NewUsersMapsActivity.this, "No drivers with such load available nearby", Toast.LENGTH_SHORT).show();
                                    radius=radius+1;
                                    GetClosestDriverCab();
                                }
                                else {
                                    assigneddriverloadref.child(driverfoundID).child("Load").setValue(existingload - load);
                                    //to get driver load
                                    final ArrayList<String > sb=new ArrayList<>();

                                    driverassigneddataref=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverfoundID).child("Your Customer ID");
                                    driverassigneddataref.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                                            int length = (int) dataSnapshot.getChildrenCount();
                                            String[] sampleString = new String[length];
                                            while(i < length) {
                                                sampleString[i] = iterator.next().getValue().toString();
                                                sb.add(sampleString[i]);
                                                Log.d(Integer.toString(i), sampleString[i]);
                                                i++;
                                            }
                                            Log.i("Length of it",String.valueOf(length));
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    Log.i("All Cust without",sb.toString());
                                    sb.add(customerID);
                                    Log.i("All Cust",sb.toString());
                                    driverassigneddataref.setValue(sb)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful())
                                                    {

                                                    }
                                                }
                                            });
                                    callacabbtn.setText("Getting your driver");
                                    GettingYourClosestDriverLocation();
                                    callacabbtn.setText("Looking for driver...");
                                }

                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

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
                    radius=radius+1;
                    GetClosestDriverCab();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingYourClosestDriverLocation() {
        driverlocationreflistener=driverlocationref.child(driverfoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()&&requesttype)
                        {
                            List<Object> driverlocationmap=(List<Object>)dataSnapshot.getValue();
                            double locationlat=0,locationlng=0;
                            callacabbtn.setText("Driver Found!!");
                            relativeLayout.setVisibility(View.VISIBLE);
                            getassigneddriverinfo();
                            if(driverlocationmap.get(0)!=null)
                            {
                                locationlat=Double.parseDouble(driverlocationmap.get(0).toString());
                            }
                            if(driverlocationmap.get(1)!=null)
                            {
                                locationlng=Double.parseDouble(driverlocationmap.get(1).toString());
                            }
                            LatLng driverlatLng=new LatLng(locationlat,locationlng);
                            if(driverMarker!=null)
                            {
                                driverMarker.remove();
                            }
                            Location location1=new Location("");
                            location1.setLatitude(customerpickuplocation.latitude);
                            location1.setLongitude(customerpickuplocation.longitude);

                            Location location2=new Location("");
                            location2.setLatitude(driverlatLng.latitude);
                            location2.setLongitude(driverlatLng.longitude);
                            driverlocation=location2;
                            float distance=location1.distanceTo(location2);
                            if(distance<90)
                            {
                                callacabbtn.setText("Driver Reached");
                            }
                            else {
                                callacabbtn.setText("Distance:" + String.valueOf(distance));
                            }
                            driverMarker=mMap.addMarker(new MarkerOptions().position(driverlatLng).title("Your Driver is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.lorryicon)));


                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }


    private boolean checkLocation() {
        if (!isLocationEnabled()) {
            showAlert();

        }
        return isLocationEnabled();

    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Please enable Location to use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        dialog.show();

    }

    private boolean isLocationEnabled() {
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    }

    private boolean requestSinglePermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        isPermission = true;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            isPermission = false;
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

        return isPermission;
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (latLng != null) {
            //mMap.addMarker(new MarkerOptions().position(latLng).title("Marker in current location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14F));

        }
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startLocationUpdate();
        mLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLoc == null) {
            startLocationUpdate();
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();

        }

    }

    private void startLocationUpdate() {

        mLocReq = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocReq, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        String msg = "Update Location" +
                Double.toString(location.getLatitude()) + " " +
                Double.toString(location.getLongitude());
        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        mLoc=location;
        latLng = new LatLng(location.getLatitude(), location.getLongitude());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        //mMap.clear();
        mapFragment.getMapAsync(this);



    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    private void getassigneddriverinfo()
    {
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverfoundID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                    {
                        String name=dataSnapshot.child("Name").getValue().toString();
                        drivernametext.setText(name);
                        String car=dataSnapshot.child("Vechicle Number").getValue().toString();
                        drivervechiclenotext.setText(car);
                        if(dataSnapshot.hasChild("Image"))
                        {
                            String image=dataSnapshot.child("Image").getValue().toString();
                            Picasso.get().load(image).into(profilepic);
                        }
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
