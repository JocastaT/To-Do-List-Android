package tp.edu.taskapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

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
import android.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity {

    public static final String TAG = "TAG";

    private EditText regUsername, regUserEmail, regUserPass, regCfmPass;
    private Button regBtn;
    private TextView regTxt;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);



        getSupportActionBar().setTitle("Connect to ToDo.txt");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        regUsername=findViewById(R.id.username_reg);
        regUserEmail=findViewById(R.id.email_reg);
        regUserPass=findViewById(R.id.password_reg);
        regCfmPass=findViewById(R.id.cfmpassword_reg);

        regBtn=findViewById(R.id.regbtn);
        regTxt=findViewById(R.id.regtxt);
        progressBar=findViewById(R.id.rprogressBar);

        regTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),LoginPage.class));
            }
        });

        firebaseAuth=FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String rUsername = regUsername.getText().toString();
                String rEmail = regUserEmail.getText().toString();
                String rPass = regUserPass.getText().toString();
                String rConfPass = regCfmPass.getText().toString();

                if (rEmail.isEmpty() || rUsername.isEmpty() || rPass.isEmpty() || rConfPass.isEmpty()){
                    Toast.makeText(RegisterPage.this, "All Fields Are Required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!rPass.equals(rConfPass)){
                    regCfmPass.setError("Passwords Do Not Match.");

                }

                progressBar.setVisibility(View.VISIBLE);

                AuthCredential credential = EmailAuthProvider.getCredential(rEmail,rPass);
                firebaseAuth.getCurrentUser().linkWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Toast.makeText(RegisterPage.this, "Tasks are Synced", Toast.LENGTH_SHORT).show();
                        userID=firebaseAuth.getCurrentUser().getUid();
                        DocumentReference documentReference = firestore.collection("users").document(userID);
                        Map<String,Object>user=new HashMap<>();
                        user.put("username",rUsername);
                        user.put("email",rEmail);
                        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "onSuccess: User Profile is created for "+ userID);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG,"onFailure: "+e.toString());
                            }
                        });
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));

                        FirebaseUser usr = firebaseAuth.getCurrentUser();
                        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                .setDisplayName(rUsername)
                                .build();
                        usr.updateProfile(request);

                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RegisterPage.this, "Failed to Connect. Try Again.", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });

            }
        });


    }




    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        return super.onOptionsItemSelected(item);
    }
}