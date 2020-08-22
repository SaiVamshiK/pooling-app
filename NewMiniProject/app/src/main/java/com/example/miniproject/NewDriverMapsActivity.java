package com.example.miniproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewDriverMapsActivity extends FragmentActivity implements OnMapReadyCallback,
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

    //new
    private Button driverlogoutbtn,settingsdriverbtn;
    private FirebaseAuth mAuth;
    private FirebaseUser currentuser;
    private boolean currentlogoutdriverstatus=false;
    private DatabaseReference assignedcustomerref,assignedcustomerpickupref;
    private String driverID,customerID="";
    private Marker mymarker,pickupmarker;
    private Location initial=null;
    //new
    private ValueEventListener assignedpickupreflistener;


    //new
    private TextView usernametext,userphonenotext;
    private CircleImageView profilepic;
    private RelativeLayout relativeLayout;

    //new
    private int load=200;
    private List<String > assignedcustomers;
    DatabaseReference assigneddriverloadref;
    final int[] check = new int[1];


    String[] allcustomers=new String[100];
    private int customerindex=0;
    DatabaseReference allcustomersdataref,verifywhetherworking;
    ArrayList<DataList> arrayList;
    Marker[] usermarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_driver_maps);

        if (requestSinglePermission()) {

            mAuth=FirebaseAuth.getInstance();
            currentuser=mAuth.getCurrentUser();
            driverlogoutbtn=(Button)findViewById(R.id.newdriverlogoutbutton);
            settingsdriverbtn=(Button)findViewById(R.id.newdriversettingsbutton);
            driverID=mAuth.getCurrentUser().getUid();

            usernametext=(TextView)findViewById(R.id.usernameindriversmapactivity);
            userphonenotext=(TextView)findViewById(R.id.phonenoindriversmapactitvity);
            profilepic=(CircleImageView)findViewById(R.id.newprofileimageiconforuserinmapsactivity);
            relativeLayout=(RelativeLayout)findViewById(R.id.rell1);
            assigneddriverloadref=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load");
            check[0]=load;


            allcustomersdataref=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverID);
            arrayList= new ArrayList<DataList>();
            verifywhetherworking=FirebaseDatabase.getInstance().getReference().child("Driver's Working").child(driverID);


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
                    DatabaseReference driveravailableload=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load").child(driverID).child("Load");
                    driveravailableload.setValue(load);
                    DatabaseReference driveravailableload1=FirebaseDatabase.getInstance().getReference().child("Driver's Available Full Load").child(driverID).child("Full Load");
                    driveravailableload1.setValue(load);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();



            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mLocManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            checkLocation();


            verifywhetherworking.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child("New Customers").exists()) {
                        verifywhetherworking.child("New Customers").removeValue();

                    }else
                    {
                        Set<DataList> set = new LinkedHashSet<>();
                        set.addAll(arrayList);

                        // Clear the list
                        arrayList.clear();
                        arrayList.addAll(set);
                        allcustomersdataref.child("New Customers").setValue(arrayList);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


            settingsdriverbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i=new Intent(NewDriverMapsActivity.this,NewSettingsActivity.class);
                    i.putExtra("type","Riders");
                    startActivity(i);
                }
            });
            driverlogoutbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentlogoutdriverstatus=true;
                    DisconnecttheDriver();
                    mAuth.signOut();
                    Intent i=new Intent(NewDriverMapsActivity.this,MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }
            });
            DatabaseReference alertingdrivertostart=FirebaseDatabase.getInstance().getReference();
            alertingdrivertostart.child("Driver's Working").child(driverID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                            //show dialog
                        /*
                            AlertDialog alertDialog = new AlertDialog.Builder(NewDriverMapsActivity.this).create();
                            alertDialog.setTitle("You are set to go!!!");
                            alertDialog.setMessage("You have your assigned customers");
                            alertDialog.setIcon(R.drawable.welcome);

                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
                                }
                            });

                            */
                            buildalertdialog();
                     //       alertDialog.show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            GettingAssignedCustomerRequest();
        }

    }

    private void buildalertdialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You are all set!!!");
        builder.setMessage("You have your assigned customers");
        builder.setIcon(R.drawable.welcome);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(NewDriverMapsActivity.this, "START YOUR RIDE!!!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
/*
        NotificationCompat.Builder builder1=new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.welcome)
                .setContentTitle("You are all set!!!")
                .setContentText("You have your assigned customers")
                ;
        Intent notiin=new Intent(this,NewDriverMapsActivity.class);
        PendingIntent contentintent=PendingIntent.getActivity(this,0,notiin,PendingIntent.FLAG_UPDATE_CURRENT);
        builder1.setContentIntent(contentintent);
        NotificationManager notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0,builder1.build());
*/
    }

    private void GettingAssignedCustomerRequest() {

        assignedcustomerref=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverID).child("Your Customer ID");

        assignedcustomerref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    //assignedcustomers.clear();
                 //   for(DataSnapshot dss:dataSnapshot.getChildren())
                   // {
                     //   String te=dss.getValue(String.class);
                       // assignedcustomers.add(te);
                   // }
                    assignedcustomers=(ArrayList<String >)dataSnapshot.getValue();
                    allcustomers[customerindex++]=assignedcustomers.get(0);
                    GettingAssignedPickUpLocation();
                    //relativeLayout.setVisibility(View.VISIBLE);
                    //getassigneduserinfo();
                }
                else
                {
                    customerID="";
                    if(pickupmarker!=null)
                    {
                        pickupmarker.remove();
                    }
                    if(assignedpickupreflistener!=null)
                    {
                        assignedcustomerpickupref.removeEventListener(assignedpickupreflistener);
                    }
                        relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GettingAssignedPickUpLocation() {
        for(String temp:assignedcustomers) {
            assignedcustomerpickupref = FirebaseDatabase.getInstance().getReference().child("Customer's Requests").child(temp).child("l");
            assignedpickupreflistener = assignedcustomerpickupref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<Object> customerlocationmap = (List<Object>) dataSnapshot.getValue();
                        double locationlat = 0, locationlng = 0;
                        if (customerlocationmap.get(0) != null) {
                            locationlat = Double.parseDouble(customerlocationmap.get(0).toString());
                        }
                        if (customerlocationmap.get(1) != null) {
                            locationlng = Double.parseDouble(customerlocationmap.get(1).toString());
                        }
                        LatLng driverlatLng = new LatLng(locationlat, locationlng);
                        pickupmarker = mMap.addMarker(new MarkerOptions().position(driverlatLng).title("Your Customer is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.usericon)));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        int j=0;
        while(j<allcustomers.length)
        {
            DataList dataList=new DataList(allcustomers[j]);
            arrayList.add(dataList);
            j++;
        }
        Log.i("Sample Customers=", Arrays.toString(allcustomers));
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
            mymarker=mMap.addMarker(new MarkerOptions().position(latLng).title("Your're here").icon(BitmapDescriptorFactory.fromResource(R.drawable.lorryicon)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14F));
           // initial.setLongitude(latLng.longitude);
            //initial.setLatitude(latLng.latitude);
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
    public void onLocationChanged(final Location location) {
        if(getApplicationContext()!=null) {

            String msg = "Update Location" +
                    Double.toString(location.getLatitude()) + " " +
                    Double.toString(location.getLongitude());
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            //mMap.clear();

            mapFragment.getMapAsync(this);
         /*   if(initial==null)
            {
                mymarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Your're here"));
                initial=location;
            }
            else
            {
                if(initial!=location)
                {
                    mymarker.remove();
                    initial=location;
                }
                if(initial==location)
                {
                    mymarker=mMap.addMarker(new MarkerOptions().position(latLng).title("Your're here"));
                }
            }
          */

            if(mymarker!=null)
            {
                mymarker.remove();
            }
            mymarker=mMap.addMarker(new MarkerOptions().position(latLng).title("Your're here").icon(BitmapDescriptorFactory.fromResource(R.drawable.lorryicon)));

            final String driverid=FirebaseAuth.getInstance().getCurrentUser().getUid();
            final DatabaseReference driveravailability=FirebaseDatabase.getInstance().getReference().child("Driver's Available");
            GeoFire geoFireavailable=new GeoFire(driveravailability);

            final DatabaseReference driverworking=FirebaseDatabase.getInstance().getReference().child("Driver's Working");
            final GeoFire geoFireworking=new GeoFire(driverworking);

            //final Integer[] lo = new Integer[1];
            //final int li;

            assigneddriverloadref.child(driverid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                        check[0] =Integer.parseInt(dataSnapshot.child("Load").getValue().toString());
                       // li =Integer.parseInt(dataSnapshot.getValue().toString());
                        if(Integer.parseInt(dataSnapshot.child("Load").getValue().toString())==0)
                        {
                            /*
                            DatabaseReference checkavailableref=FirebaseDatabase.getInstance().getReference();
                            checkavailableref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild("Driver's Available"))
                                    {
                                        driveravailability.removeValue();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            */
                            DatabaseReference driveravailableloadremovalref=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load").child(driverID);
                            driveravailableloadremovalref.removeValue();
                            driveravailability.removeValue();
                            geoFireworking.setLocation(driverID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {

                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            if(check[0]!=0)
            {
                DatabaseReference checkworkingref=FirebaseDatabase.getInstance().getReference();
                checkworkingref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild("Driver's Working"))
                        {
                            driverworking.removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                geoFireavailable.setLocation(driverid, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });
            }
            else
            {
                geoFireworking.setLocation(driverID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });
            }
            /*else
            {
                DatabaseReference checkavailableref=FirebaseDatabase.getInstance().getReference();
                checkavailableref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild("Driver's Available"))
                        {
                            driveravailability.removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                DatabaseReference driveravailableloadremovalref=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load").child(driverid);
                driveravailableloadremovalref.removeValue();

                geoFireworking.setLocation(driverid, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

            }
            */
          /*  if(initial==location){
                mymarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Your're here"));
            }
            else
            {
                mymarker.remove();
                mymarker=mMap.addMarker(new MarkerOptions().position(latLng).title("Your're here"));
                initial=location;
            }

           */
        }

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
        if(!currentlogoutdriverstatus)
        {
            DisconnecttheDriver();
        }


        if (mGoogleApiClient.isConnected()) {

            mGoogleApiClient.disconnect();
            String did=FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference remo=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(did).child("Your Customer ID");
            remo.removeValue();

        }
    }

    private void DisconnecttheDriver() {
        String driverid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driveravailability=FirebaseDatabase.getInstance().getReference().child("Driver's Available").child(driverid);
        driveravailability.removeValue();
        DatabaseReference driveravailableloadremovalref=FirebaseDatabase.getInstance().getReference().child("Driver's Available Load").child(driverid);
        driveravailableloadremovalref.removeValue();

        DatabaseReference driveravailableloadremovalref1=FirebaseDatabase.getInstance().getReference().child("Driver's Available Full Load").child(driverid);
        driveravailableloadremovalref1.removeValue();
    }
    private void getassigneduserinfo()
    {
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                {
                    String name=dataSnapshot.child("Name").getValue().toString();
                    usernametext.setText(name);
                    String phone=dataSnapshot.child("Phone").getValue().toString();
                    userphonenotext.setText(phone);
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
