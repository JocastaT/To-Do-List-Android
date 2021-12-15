package tp.edu.taskapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import tp.edu.taskapp.Adapter.ToDoAdapter;
import tp.edu.taskapp.Calendar.CalendarViewWithNotesActivitySDK21;
import tp.edu.taskapp.Model.ToDoModel;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        OnDialogCloseListener {

    private NavigationView nav_view;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FloatingActionButton mFab;
    private FirebaseFirestore firestore;
    private ToDoAdapter adapter;
    private List<ToDoModel> mList;
    private Query query;
    private ListenerRegistration listenerRegistration;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Calendar calendar = Calendar.getInstance();
        String currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime());

        TextView textViewDate = findViewById(R.id.text_date);
        textViewDate.setText(currentDate);
        textViewDate.setAllCaps(true);


        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        toolbar = findViewById(R.id.toolbars);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer);
        nav_view = findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);


        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);

        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();

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


        recyclerView = findViewById(R.id.recyclerview);
        mFab = findViewById(R.id.floatingActionButton);
        firestore = FirebaseFirestore.getInstance();

        //query notes > uid > mynotes

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG);

            }
        });

        mList = new ArrayList<>();
        adapter = new ToDoAdapter(MainActivity.this, mList);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        showData();
        recyclerView.setAdapter(adapter);

    }




    //display data
    private void showData(){

        query=firestore.collection("task").document(user.getUid()).collection("myTasks").orderBy("priority", Query.Direction.DESCENDING);
        query=firestore.collection("task").document(user.getUid()).collection("myTasks").orderBy("status", Query.Direction.ASCENDING);

        listenerRegistration=query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                for (DocumentChange documentChange: value.getDocumentChanges()){
                    if(documentChange.getType()== DocumentChange.Type.ADDED){
                        //represents a change to the documents matching a query
                        String id = documentChange.getDocument().getId();
                        ToDoModel toDoModel = documentChange.getDocument().toObject(ToDoModel.class).withId(id);
                        mList.add(toDoModel);
                        adapter.notifyDataSetChanged();
                    }

                    }
                listenerRegistration.remove();

            }
        });

    }

    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        mList.clear();
        showData(); //loads data 2 times
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        switch (item.getItemId()){

            case R.id.calendar:
                startActivity(new Intent (this, CalendarViewWithNotesActivitySDK21.class));
                break;

            case R.id.notes:
                startActivity(new Intent (this, NotesActivity.class));
                break;

            case R.id.profile:
                if (user.isAnonymous()){
                    Toast.makeText(this, "Please Login", Toast.LENGTH_SHORT).show();
                }else {
                    startActivity(new Intent(this, ProfilePg.class));
                }
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
            GoogleSignIn.getClient(MainActivity.this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                    .signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, "Logout Failed", Toast.LENGTH_SHORT).show();
                }
            });

        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menusearch,menu);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView= (SearchView) item.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }


}

