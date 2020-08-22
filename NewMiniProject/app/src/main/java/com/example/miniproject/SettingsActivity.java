package com.example.miniproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView profileimageview;
    private EditText nameedittext,phoneedittext,carnumberedittext;
    private ImageView closebutton,savebutton;
    private TextView profilechangebtn;
    private String gettype;
    private TextView vechiclenotextview;
    private String checker="";
    private Uri imageuri;
    private String myurl="";
    private StorageTask uploadtask;
    private StorageReference storageprofilepicReference;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        gettype=getIntent().getStringExtra("type");

        Toast.makeText(this, gettype, Toast.LENGTH_SHORT).show();
        mAuth=FirebaseAuth.getInstance();
        databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child(gettype);
        storageprofilepicReference= FirebaseStorage.getInstance().getReference().child("Profile Pictures");

        profileimageview=(CircleImageView)findViewById(R.id.profile_image);
        nameedittext=(EditText)findViewById(R.id.usernameedittextID);
        phoneedittext=(EditText)findViewById(R.id.phonenumberedittextID);
        carnumberedittext=(EditText)findViewById(R.id.vehiclenumberedittextID);
        closebutton=(ImageView) findViewById(R.id.crossbtn);
        savebutton=(ImageView)findViewById(R.id.tickbtn);
        profilechangebtn=(TextView)findViewById(R.id.uploadpictureID);
        vechiclenotextview=(TextView)findViewById(R.id.editText41);
        if(gettype.equals("Customers"))
        {
           vechiclenotextview.setVisibility(View.INVISIBLE);
         carnumberedittext.setVisibility(View.INVISIBLE);
        }
        closebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(gettype.equals("Riders"))
                {
                    startActivity(new Intent(SettingsActivity.this,NewDriverMapsActivity.class));
                }
                else
                {
                    startActivity(new Intent(SettingsActivity.this,NewUsersMapsActivity.class));
                }
            }
        });
        savebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checker.equals("Clicked"))
                {
                    validatecontrollers();
                }
                else
                {
                    validateandsaveonlyinformation();
                }
            }
        });

        profilechangebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker="Clicked";
                CropImage.activity()
                        .setAspectRatio(1,1)
                        .start(SettingsActivity.this);
            }
        });
        getuserinformation();
    }

    private void validateandsaveonlyinformation() {

        if(TextUtils.isEmpty(nameedittext.getText().toString())){
            Toast.makeText(this, "Please provide name", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phoneedittext.getText().toString())){
            Toast.makeText(this, "Please provide phone number", Toast.LENGTH_SHORT).show();
        }
        else if(gettype.equals("Riders")&&TextUtils.isEmpty(carnumberedittext.getText().toString())){
            Toast.makeText(this, "Please provide Vechicle number", Toast.LENGTH_SHORT).show();
        }
        else {
            HashMap<String, Object> usermap = new HashMap<>();
            usermap.put("Uid", mAuth.getCurrentUser().getUid());
            usermap.put("Name", nameedittext.getText().toString());
            usermap.put("Phone", phoneedittext.getText().toString());
            if (gettype.equals("Riders")) {
                usermap.put("Vechicle Number", carnumberedittext.getText().toString());
            }
            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(usermap);

            if (gettype.equals("Riders")) {
                startActivity(new Intent(SettingsActivity.this, NewDriverMapsActivity.class));
            } else {
                startActivity(new Intent(SettingsActivity.this, NewUsersMapsActivity.class));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE&&resultCode==RESULT_OK&&data!=null)
        {
            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            imageuri=result.getUri();
            profileimageview.setImageURI(imageuri);
        }
        else {
            if(gettype.equals("Riders"))
            {
                startActivity(new Intent(SettingsActivity.this, NewDriverMapsActivity.class));
            }
            else {
                startActivity(new Intent(SettingsActivity.this, NewUsersMapsActivity.class));
            }
            Toast.makeText(this, "Error!!!Try Again.", Toast.LENGTH_SHORT).show();
        }
    }
    private void validatecontrollers()
    {
        if(TextUtils.isEmpty(nameedittext.getText().toString())){
            Toast.makeText(this, "Please provide name", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phoneedittext.getText().toString())){
            Toast.makeText(this, "Please provide phone number", Toast.LENGTH_SHORT).show();
        }
        else if(gettype.equals("Riders")&&TextUtils.isEmpty(carnumberedittext.getText().toString())){
            Toast.makeText(this, "Please provide Vechicle number", Toast.LENGTH_SHORT).show();
        }
        else if(checker.equals("Clicked"))
        {
            UploadProfilePicture();
        }
    }

    private void UploadProfilePicture() {
        final ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Info!!");
        progressDialog.setMessage("Please wait,while we are setting your account information");
        progressDialog.show();
        if(imageuri!=null)
        {
            final StorageReference fileref=storageprofilepicReference
                    .child(mAuth.getCurrentUser().getUid()+".jpg");
            uploadtask=fileref.putFile(imageuri);
            uploadtask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                if(!task.isSuccessful())
                {
                    throw task.getException();
                }
                    return fileref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful())
                    {
                        Uri downloadurl=task.getResult();
                        myurl=downloadurl.toString();
                        HashMap<String ,Object> usermap=new HashMap<>();
                        usermap.put("Uid",mAuth.getCurrentUser().getUid());
                        usermap.put("Name",nameedittext.getText().toString());
                        usermap.put("Phone",phoneedittext.getText().toString());
                        usermap.put("Image",myurl);
                        if(gettype.equals("Riders"))
                        {
                            usermap.put("Vechicle Number",carnumberedittext.getText().toString());
                        }
                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(usermap);
                        progressDialog.dismiss();

                        if(gettype.equals("Riders"))
                        {
                            startActivity(new Intent(SettingsActivity.this,NewDriverMapsActivity.class));
                        }
                        else {
                            startActivity(new Intent(SettingsActivity.this,NewUsersMapsActivity.class));
                        }
                    }
                }
            });

        }
        else
        {
            Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
        }
    }
    private void getuserinformation()
    {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                {
                    String name=dataSnapshot.child("Name").getValue().toString();
                    String phone=dataSnapshot.child("Phone").getValue().toString();
                    nameedittext.setText(name);
                    phoneedittext.setText(phone);

                    if(gettype.equals("Riders"))
                    {
                        String car=dataSnapshot.child("Vechicle Number").getValue().toString();
                        carnumberedittext.setText(car);
                    }

                    if(dataSnapshot.hasChild("Image")) {
                        String image = dataSnapshot.child("Image").toString();
                        Picasso.get().load(image).into(profileimageview);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
