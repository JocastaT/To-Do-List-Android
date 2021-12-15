package tp.edu.taskapp.Calendar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tp.edu.taskapp.AddNewTask;
import tp.edu.taskapp.LoginPage;
import tp.edu.taskapp.MainActivity;
import tp.edu.taskapp.Model.ToDoModel;
import tp.edu.taskapp.NotesActivity;
import tp.edu.taskapp.ProfilePg;
import tp.edu.taskapp.R;
import tp.edu.taskapp.RegisterPage;
import tp.edu.taskapp.SplashActivity;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CalendarViewWithNotesActivitySDK21 extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private String[] mShortMonths; //months in short string
    private CalendarView mCalendarView;
    private CalendarDialog mCalendarDialog;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private NavigationView nav_view;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    private List<ToDoModel> mEventList = new ArrayList<>();

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_content);

        toolbar=findViewById(R.id.toolbarsdk21);
        setSupportActionBar(toolbar);

        firebaseAuth= FirebaseAuth.getInstance();
        user=firebaseAuth.getCurrentUser();
        drawerLayout=findViewById(R.id.drawer);
        nav_view=findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);
        toggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar, R.string.open, R.string.close);
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

        mShortMonths = new DateFormatSymbols().getShortMonths();

        mCalendarView = findViewById(R.id.calendarViewSDK);
        mCalendarView.setOnMonthChangedListener(new CalendarView.OnMonthChangedListener(){
            @Override
            public void onMonthChanged(int month, int year) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(mShortMonths[month]);
                    getSupportActionBar().setSubtitle(Integer.toString(year));
                }
            }
        });


        for (ToDoModel e : mEventList) {
            mCalendarView.addCalendarObject(parseCalendarObject(e));
        }

        if (getSupportActionBar() != null) {
            int month = mCalendarView.getCurrentDate().get(Calendar.MONTH);
            int year = mCalendarView.getCurrentDate().get(Calendar.YEAR);
            getSupportActionBar().setTitle(mShortMonths[month]);
            getSupportActionBar().setSubtitle(Integer.toString(year));
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG);
            }
        });

        mCalendarDialog = CalendarDialog.Builder.instance(this)
                .setEventList(mEventList)
                .setOnItemClickListener(new CalendarDialog.OnCalendarDialogListener() {
                    @Override
                    public void onEventClick(ToDoModel event) {
                        onEventSelected(event);
                    }

                    @Override
                    public void onCreateEvent(Calendar calendar) {
                        createEvent(calendar);
                    }
                })
                .create();
    }

    private void onEventSelected(ToDoModel event) {
        AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG);
        overridePendingTransition( R.anim.slide_in_up, R.anim.stay );
    }

    private void createEvent(Calendar selectedDate) {
        AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG);
        overridePendingTransition( R.anim.slide_in_up, R.anim.stay );
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_calendar_view, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_today: {
                mCalendarView.setSelectedDate(Calendar.getInstance());
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }


    public static int diffYMD(Calendar date1, Calendar date2) {
        if (date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH))
            return 0;

        return date1.before(date2) ? -1 : 1;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        switch (item.getItemId()){

            case R.id.tasks:
                startActivity(new Intent (this, MainActivity.class));
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
        if (user.isAnonymous()){
            displayAlert();
        }else{
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), SplashActivity.class));
            finish();
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
                        startActivity(new Intent(getApplicationContext(), RegisterPage.class));
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
                                startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                                finish();
                            }
                        });

                    }
                });
        warning.show();
    }

    private static CalendarView.CalendarObject parseCalendarObject(ToDoModel event) {
        return new CalendarView.CalendarObject(
                event.getTaskId(),
                event.getDate(),
                event.getColor(),
                event.isCompleted() ? android.graphics.Color.TRANSPARENT : android.graphics.Color.RED);
    }


}
