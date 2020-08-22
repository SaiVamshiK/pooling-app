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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class NewSettingsActivityforUsers extends AppCompatActivity {

    private CircleImageView profileimageview;
    private EditText nameedittext,phoneedittext;
    private ImageView closebutton;
    private Button savebutton;

    private String gettype;
    private String checker="";

    private StorageReference storageprofilepicReference;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;


    private Uri imageuri;
    private String myurl="";
    private StorageTask uploadtask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_settings_activityfor_users);
        Toast.makeText(this, "Customers", Toast.LENGTH_SHORT).show();
        mAuth=FirebaseAuth.getInstance();
        databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers");
        storageprofilepicReference= FirebaseStorage.getInstance().getReference().child("Profile Pictures");

        profileimageview=(CircleImageView)findViewById(R.id.newprofileimageiconforusers);
        nameedittext=(EditText)findViewById(R.id.newusernameeditTextforusers);
        phoneedittext=(EditText)findViewById(R.id.newphonenumbereditTextforusers);
        closebutton=(ImageView)findViewById(R.id.newcrossbtnimageviewforUsersID);
        savebutton=(Button)findViewById(R.id.newsavechangesbuttonforusers);

        getuserinformation();

        closebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    startActivity(new Intent(NewSettingsActivityforUsers.this,NewUsersMapsActivity.class));
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
        profileimageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker="Clicked";
                CropImage.activity()
                        .setAspectRatio(1,1)
                        .start(NewSettingsActivityforUsers.this);
            }
        });



    }
    private void validateandsaveonlyinformation() {

        if(TextUtils.isEmpty(nameedittext.getText().toString())){
            Toast.makeText(this, "Please provide name", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phoneedittext.getText().toString())){
            Toast.makeText(this, "Please provide phone number", Toast.LENGTH_SHORT).show();
        }
        else {
            HashMap<String, Object> usermap = new HashMap<>();
            usermap.put("Uid", mAuth.getCurrentUser().getUid());
            usermap.put("Name", nameedittext.getText().toString());
            usermap.put("Phone", phoneedittext.getText().toString());

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(usermap);


            startActivity(new Intent(NewSettingsActivityforUsers.this, NewUsersMapsActivity.class));

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

                startActivity(new Intent(NewSettingsActivityforUsers.this, NewUsersMapsActivity.class));
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

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(usermap);
                        progressDialog.dismiss();

                        startActivity(new Intent(NewSettingsActivityforUsers.this,NewUsersMapsActivity.class));

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
