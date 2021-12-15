package tp.edu.taskapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        firebaseAuth = FirebaseAuth.getInstance();



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (firebaseAuth.getCurrentUser() != null) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();//exit from that page

                } else {
                    //create new anon acc
                    firebaseAuth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Toast.makeText(SplashActivity.this, "Logged In With Temporary Account", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SplashActivity.this, "Error" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            //check if user is logged on

        },4000);
    }
}