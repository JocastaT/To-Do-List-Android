package tp.edu.taskapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginPage extends AppCompatActivity {

    public static final int GOOGLE_SIGN_IN_CODE = 10005;
    private SignInButton signIn;
    private GoogleSignInClient googleSignInClient;
    private GoogleSignInOptions gso;
    
    private static final String TAG = "TAG";

    private EditText logEmail, logPass;
    private Button loginBtn;
    private TextView forgetPw, loginTxt;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ProgressBar progressBar;
    private FirebaseUser user;
    private String userID;

    CallbackManager mCallbackManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);


        //google options popup --> shows your gmail signed in account
        signIn=findViewById(R.id.signInBtn);
        gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("638726449275-1m0oal3ap6jq99vi65qfm2t8b43leppm.apps.googleusercontent.com")
                .requestEmail()
                .build();

        //specify commonly-used sign-in providers
        googleSignInClient=GoogleSignIn.getClient(this,gso);

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        //get google account signed in
        if (signInAccount !=null){
            startActivity(new Intent(this,MainActivity.class));
        }


        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sign = googleSignInClient.getSignInIntent();
                startActivityForResult(sign,GOOGLE_SIGN_IN_CODE);
            }
        });


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Login to ToDo.txt");

        logEmail = findViewById(R.id.email_login);
        logPass=findViewById(R.id.password_login);
        loginBtn=findViewById(R.id.loginbtn);

        firestore = FirebaseFirestore.getInstance();
        forgetPw=findViewById(R.id.forgetPass);
        loginTxt=findViewById(R.id.logintxt);

        firebaseAuth=FirebaseAuth.getInstance();
        progressBar=findViewById(R.id.progressBarLogin);
        user = firebaseAuth.getCurrentUser();


        showWarning();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mEmail = logEmail.getText().toString();
                String mPassword = logPass.getText().toString();

                if (mEmail.isEmpty() || mPassword.isEmpty()){
                    Toast.makeText(LoginPage.this, "Fields Are Required", Toast.LENGTH_SHORT).show();
                    return;
                }

                //delete notes first of anon user
                progressBar.setVisibility(View.VISIBLE);

                if (firebaseAuth.getCurrentUser().isAnonymous()){
                    FirebaseUser user1 = firebaseAuth.getCurrentUser();

                    firestore.collection("task").document(user1.getUid()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(LoginPage.this, "All Temp Notes are Deleted", Toast.LENGTH_SHORT).show();   
                        }
                    });
                    
                    //delete Temp user
                    
                    user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(LoginPage.this, "Temp User Deleted.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                firebaseAuth.signInWithEmailAndPassword(mEmail,mPassword).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Toast.makeText(LoginPage.this, "Success !", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LoginPage.this, "Login Failed. " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        //forget password
        forgetPw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText forgetPassw = new EditText(view.getContext());

                final AlertDialog.Builder passwordForgetDialog = new AlertDialog.Builder(view.getContext());
                //popup
                passwordForgetDialog.setTitle("Reset Password ?");
                passwordForgetDialog.setMessage("Enter Your Email To Received Reset Link");
                passwordForgetDialog.setView(forgetPassw);

                passwordForgetDialog.setPositiveButton("Send Email", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String email = forgetPassw.getText().toString();
                        //send email to reset pass
                        firebaseAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(LoginPage.this, "Reset Link Sent To Your Email.", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(LoginPage.this, "Error: Reset Link Not Sent"+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                passwordForgetDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                passwordForgetDialog.create().show();
            }
        });

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String rEmail = user.getEmail();
        String rUsername = user.getDisplayName();

        if (requestCode ==GOOGLE_SIGN_IN_CODE){
            Task<GoogleSignInAccount> signInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            //get google account

            try {
                GoogleSignInAccount signInAccount = signInTask.getResult(ApiException.class);

                //delete anon user
                if (firebaseAuth.getCurrentUser().isAnonymous()){
                    FirebaseUser user1 = firebaseAuth.getCurrentUser();
                    firestore.collection("task").document(user1.getUid()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(LoginPage.this, "All Temp Notes are Deleted", Toast.LENGTH_SHORT).show();
                        }
                    });

                    //delete Temp user

                    user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(LoginPage.this, "Temp User Deleted.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }


                //authenticate with firebase and get id
                AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(),null);
                firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Toast.makeText(getApplicationContext(), "Your Google Account is Connected to Our Application", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));

                        userID=firebaseAuth.getCurrentUser().getUid();
                        DocumentReference documentReference = firestore.collection("users").document(userID);

                        Map<String,Object> user=new HashMap<>();
                        user.put("email",firebaseAuth.getCurrentUser().getEmail());
                        user.put("username",firebaseAuth.getCurrentUser().getDisplayName());

                        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(LoginPage.this, "Successfully logged into DB", Toast.LENGTH_SHORT).show();
                            }
                        });

                        FirebaseUser usr = firebaseAuth.getCurrentUser();
                        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                .setDisplayName(usr.getDisplayName())
                                .build();
                        usr.updateProfile(request);





                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

            } catch (ApiException e) {
                e.printStackTrace();
            }
        }


    }

    private void showWarning() {
        AlertDialog.Builder warning = new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("Linking Existing Account will delete ALL the temp tasks. Create New Account To Save them.")
                .setPositiveButton("Save Tasks", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //sync note
                        startActivity(new Intent(getApplicationContext(),RegisterPage.class));
                        finish(); //user cannot comeback to this page
                    }
                }).setNegativeButton("Its Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        //do nothing

                    }
                });
        warning.show();
    }
}

