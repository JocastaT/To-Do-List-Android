package tp.edu.taskapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tp.edu.taskapp.Adapter.NotesAdapter;
import tp.edu.taskapp.Calendar.CalendarViewWithNotesActivitySDK21;
import tp.edu.taskapp.Model.NoteModel;
import tp.edu.taskapp.Notes.AddNote;
import tp.edu.taskapp.Notes.EditNote;
import tp.edu.taskapp.Notes.NoteDetails;

public class NotesActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private NavigationView nav_view;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FloatingActionButton mFab;
    private FirebaseFirestore firestore;
    private Query query;
    FirestoreRecyclerAdapter<NoteModel,NoteViewHolder> noteAdapter;
    private NotesAdapter notesAdapter;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private List<String>content;
    private List<String>titles;
    private List<String>titlesFilter=new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        firebaseAuth=FirebaseAuth.getInstance();
        user=firebaseAuth.getCurrentUser();

        firestore=FirebaseFirestore.getInstance();
        toolbar=findViewById(R.id.toolbars);
        setSupportActionBar(toolbar);

        drawerLayout=findViewById(R.id.drawer);
        nav_view=findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);


        toggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open,R.string.close);
        drawerLayout.addDrawerListener(toggle);

        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();

        View headerView =nav_view.getHeaderView(0);
        TextView rusername = headerView.findViewById(R.id.usernameid);
        TextView remail = headerView.findViewById(R.id.emailid);

        if (user.isAnonymous()){
            remail.setVisibility(View.GONE);
            rusername.setText("Temporary User");
        }else{
            remail.setText(user.getEmail());
            rusername.setText(user.getDisplayName());
        }

        query = firestore.collection("notes").document(user.getUid()).collection("myNotes").orderBy("title", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<NoteModel> allNotes = new FirestoreRecyclerOptions.Builder<NoteModel>()
                .setQuery(query,NoteModel.class)
                .build();

        recyclerView=findViewById(R.id.recyclerview);
        mFab=findViewById(R.id.floatingActionButton);
        firestore = FirebaseFirestore.getInstance();

        //query notes > uid > mynotes

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), AddNote.class));

            }
        });

        noteAdapter = new FirestoreRecyclerAdapter<NoteModel, NoteViewHolder>(allNotes) {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            protected void onBindViewHolder(@NonNull NoteViewHolder noteViewHolder, final int i, @NonNull final NoteModel note) {
                noteViewHolder.noteTitle.setText(note.getTitle());
                noteViewHolder.noteContent.setText(note.getContent());
                final int code = getRandomColor();
                noteViewHolder.mCardView.setCardBackgroundColor(noteViewHolder.view.getResources().getColor(code,null));
                final String docId = noteAdapter.getSnapshots().getSnapshot(i).getId();

                noteViewHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(v.getContext(), NoteDetails.class);
                        i.putExtra("title",note.getTitle());
                        i.putExtra("content",note.getContent());
                        i.putExtra("code",code);
                        i.putExtra("noteId",docId);
                        v.getContext().startActivity(i);
                    }
                });

                ImageView menuIcon = noteViewHolder.view.findViewById(R.id.menuIcon);
                menuIcon.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(final View v) {
                        final String docId = noteAdapter.getSnapshots().getSnapshot(i).getId();
                        PopupMenu menu = new PopupMenu(v.getContext(),v);
                        menu.setGravity(Gravity.END);
                        menu.getMenu().add("Edit").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                Intent i = new Intent(v.getContext(), EditNote.class);
                                i.putExtra("title",note.getTitle());
                                i.putExtra("content",note.getContent());
                                i.putExtra("noteId",docId);
                                startActivity(i);
                                return false;
                            }
                        });

                        menu.getMenu().add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                DocumentReference docRef = firestore.collection("notes").document(user.getUid()).collection("myNotes").document(docId);
                                docRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // note deleted
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(NotesActivity.this, "Error in Deleting Note.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return false;
                            }
                        });

                        menu.show();

                    }
                });



            }

            @NonNull
            @Override
            public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_view_layout,parent,false);
                return new NoteViewHolder(view);
            }
        };





        recyclerView.setAdapter(noteAdapter);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        switch (item.getItemId()){

            case R.id.tasks:
                startActivity(new Intent (this, MainActivity.class));
                break;

            case R.id.calendar:
                startActivity(new Intent (this, CalendarViewWithNotesActivitySDK21.class));
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
            GoogleSignIn.getClient(NotesActivity.this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                    .signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(NotesActivity.this, "Logout Failed", Toast.LENGTH_SHORT).show();
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



    public class NoteViewHolder extends RecyclerView.ViewHolder{
        TextView noteTitle,noteContent;
        View view;
        CardView mCardView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            noteTitle = itemView.findViewById(R.id.notetitle);
            noteContent = itemView.findViewById(R.id.notecontent);
            mCardView = itemView.findViewById(R.id.noteCardview);
            view = itemView;
        }
    }

    private int getRandomColor() {

        List<Integer> colorCode = new ArrayList<>();
        colorCode.add(R.color.blue);
        colorCode.add(R.color.lightGreen);
        colorCode.add(R.color.gray);
        colorCode.add(R.color.pink);
        colorCode.add(R.color.brown);
        colorCode.add(R.color.yellowy);

        Random randomColor = new Random();
        int number = randomColor.nextInt(colorCode.size());
        return colorCode.get(number);

    }

    @Override
    protected void onStart() {
        super.onStart();
        noteAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (noteAdapter != null) {
            noteAdapter.stopListening();
        }
    }



}

