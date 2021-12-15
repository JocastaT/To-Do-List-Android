package tp.edu.taskapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import tp.edu.taskapp.Calendar.CalendarViewWithNotesActivitySDK21;


public class ProfilePg extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    private TextView changeProfileImage;
    private ImageView profilePic;
    private TextView userName, userEmail;
    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String userId;
    private FirebaseUser user;
    private ListView listView;
    private NavigationView nav_view;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);



        userName=findViewById(R.id.usernameprofile);
        userEmail=findViewById(R.id.emailprofile);

        toolbar=findViewById(R.id.toolbarpfp);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer);
        nav_view = findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);


        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);

        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();


        firebaseAuth=FirebaseAuth.getInstance();
        firestore=FirebaseFirestore.getInstance();

        user = firebaseAuth.getCurrentUser();

        View headerView = nav_view.getHeaderView(0);
        TextView rusername = headerView.findViewById(R.id.usernameid);
        TextView remail = headerView.findViewById(R.id.emailid);

        if (user.isAnonymous()) {
            remail.setVisibility(View.GONE);
            rusername.setText("Temporary User");
        } else {
            remail.setText(user.getEmail());
            rusername.setText(user.getDisplayName());
        }

        profilePic=findViewById(R.id.pfp);
        changeProfileImage=findViewById(R.id.changeProfile);

        listView=(ListView)findViewById(R.id.listview);

        final ArrayList<String>arrayList=new ArrayList<>();

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount !=null) {
            arrayList.add("My Tasks");
            arrayList.add("My Notes");
            arrayList.add("Logout");
        }else{
            arrayList.add("My Tasks");
            arrayList.add("My Notes");
            arrayList.add("Logout");
            arrayList.add("Change Password");
            arrayList.add("Delete Account");
        }

        ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        //onclick diff position of the listview
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String item = (String) listView.getItemAtPosition(position);
                Toast.makeText(ProfilePg.this, "You selected : "+item, Toast.LENGTH_SHORT).show();

                if (position==0){
                    startActivity(new Intent(ProfilePg.this,MainActivity.class));
                }
                else if (position==1){
                    startActivity(new Intent(ProfilePg.this,NotesActivity.class));
                } else if (position == 2) {
                    logout();
                }
                else if(position==3){
                    changePassword();
                }
                else if (position==4){
                    deleteaccount();
                }

            }
        });

        userId=user.getUid();

        DocumentReference documentReference = firestore.collection("users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                    userEmail.setText(documentSnapshot.getString("email"));
                    userName.setText(documentSnapshot.getString("username"));

            }
        });

        storageReference= FirebaseStorage.getInstance().getReference();

        StorageReference profileRef = storageReference.child("users/"+firebaseAuth.getCurrentUser().getUid()+"/profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(profilePic);
            }
        });

        changeProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open gallery
                Intent i = new Intent(view.getContext(),EditProfile.class);
                i.putExtra("email", userEmail.getText().toString());
                i.putExtra("username",userName.getText().toString());
                startActivity(i);
           //     Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
             //   startActivityForResult(openGalleryIntent,1000);
            }
        });

    }

    private void logout() {
        //if user is real or not - real email / anon user
        if (user.isAnonymous()) {
            displayAlert();
        } else {

            FirebaseAuth.getInstance().signOut();
            GoogleSignIn.getClient(ProfilePg.this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                    .signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ProfilePg.this, "Logout Failed", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void changePassword(){
            final EditText resetPass = new EditText(ProfilePg.this);

            final AlertDialog.Builder passwordChangeDialog = new AlertDialog.Builder(ProfilePg.this);
            passwordChangeDialog.setTitle("Reset Password ?");
            passwordChangeDialog.setMessage("Enter New Password > 6 Characters long.");
            passwordChangeDialog.setView(resetPass);

            passwordChangeDialog.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String newPass = resetPass.getText().toString();
                    user.updatePassword(newPass).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ProfilePg.this, "Password Reset Successfully.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfilePg.this, "Password Reset Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            passwordChangeDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            passwordChangeDialog.create().show();
        }

        private void deleteaccount(){
            AlertDialog.Builder dialog = new AlertDialog.Builder(ProfilePg.this);
            dialog.setTitle("Delete Account Permanently?");
            dialog.setMessage("Are you sure?");
            dialog.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    DocumentReference documentReference = firestore.collection("users").document(userId);
                    documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                user.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(ProfilePg.this, "Account deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                Toast.makeText(ProfilePg.this, "Deleted from DB", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ProfilePg.this, SplashActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(ProfilePg.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog alertDialog = dialog.create();
            alertDialog.show();
        }



    private void displayAlert() {
        AlertDialog.Builder warning = new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("You are logged in with a Temporary Account. Logging out will Delete ALL Tasks.")
                .setPositiveButton("Sync Task", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //sync note
                        startActivity(new Intent(getApplicationContext(),RegisterPage.class));
                        finish(); //user cannot comeback to this page
                    }
                }).setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //ToDO:delete all the notes created by the anon user

                        //ToDO:delete the anon user
                        user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                startActivity(new Intent(getApplicationContext(),SplashActivity.class));
                                finish();
                            }
                        });

                    }
                });
        warning.show();
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        switch (item.getItemId()){

            case R.id.tasks:
                startActivity(new Intent (this, MainActivity.class));

            case R.id.calendar:
                startActivity(new Intent (this, CalendarViewWithNotesActivitySDK21.class));
                break;

            case R.id.notes:
                startActivity(new Intent (this, NotesActivity.class));
                break;

            case R.id.sync:
                if (user.isAnonymous()) {
                    startActivity(new Intent(this, LoginPage.class ));
                }else{
                    Toast.makeText(this, "You Are Connected.", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.logoutbtn:
                checkUser();
                break;
            default:
                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    private void checkUser() {
        //if user is real or not - real email / anon user
        if (user.isAnonymous()) {
            displayAlert();
        } else {

            FirebaseAuth.getInstance().signOut();
            GoogleSignIn.getClient(ProfilePg.this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                    .signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ProfilePg.this, "Logout Failed", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }



}

