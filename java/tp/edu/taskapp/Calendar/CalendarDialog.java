package tp.edu.taskapp.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import tp.edu.taskapp.AddNewTask;
import tp.edu.taskapp.MainActivity;
import tp.edu.taskapp.Model.ToDoModel;
import tp.edu.taskapp.OnDialogCloseListener;
import tp.edu.taskapp.R;

public class CalendarDialog {

    @SuppressWarnings("unused")
    private static final String TAG = CalendarDialog.class.getSimpleName();

    private final static Calendar sToday = Calendar.getInstance();

    private static final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private static final float MIN_OFFSET = 0f;
    private static final float MAX_OFFSET = 0.5f;

    private static final float MIN_ALPHA = 0.5f;
    private static final float MIN_SCALE = 0.8f;
    private static Context mContext;


    private Calendar mSelectedDate = sToday;

    private List<ToDoModel> mEventList = new ArrayList<>();
    private OnCalendarDialogListener mListener;



    public AlertDialog mAlertDialog;
    private View mView;
    private ViewPager mViewPager;
    //allows the user to swipe left or right to see an entirely new screen. In a sense
    private ViewPagerAdapter mViewPagerAdapter;

    private CalendarDialog calendarDialog;
    private Handler mHandler;

    CalendarDialog(Context context) {
        mContext = context;
        mHandler = new Handler();

        buildView();
    }


    public void setSelectedDate(Calendar selectedDate) {
        mSelectedDate = selectedDate;
        mViewPagerAdapter.setSelectedDate(mSelectedDate);
        mViewPager.setCurrentItem(mViewPagerAdapter.initialPageAndDay.first);
    }

    public void setEventList(List<ToDoModel> eventList) {
        mEventList = eventList;
        mViewPagerAdapter.notifyDataSetChanged();
    }

    void setOnCalendarDialogListener(OnCalendarDialogListener listener) {
        mListener = listener;
    }

    public void show() {
        long delayMillis = 100L;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayedShow();
            }
        }, delayMillis);
    }

    private void buildView() {
        mView = View.inflate(mContext, R.layout.dialog_calendar, null);
        mViewPager = mView.findViewById(R.id.viewPager_calendar);
        // Disable clip to padding
        mViewPager.setClipToPadding(false);
        // set padding manually, the more you set the padding the more you see of prev & next page
        mViewPager.setPadding(160, 0, 160, 0);
        mViewPager.setPageMargin(60);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                updatePager(mViewPager.findViewWithTag(position), 1f - positionOffset);
                updatePager(mViewPager.findViewWithTag(position + 1), positionOffset);
                updatePager(mViewPager.findViewWithTag(position + 2), 0);
                updatePager(mViewPager.findViewWithTag(position - 1), 0);
            }
        });
        mViewPagerAdapter = new ViewPagerAdapter(mSelectedDate, mEventList);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setCurrentItem(mViewPagerAdapter.initialPageAndDay.first);

        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dismissDialog();
                return false;
            }
        });

        mAlertDialog = new AlertDialog.Builder(mContext).create();
    }

    private void delayedShow() {
        if (mAlertDialog.getWindow() != null)
            mAlertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        mAlertDialog.setCanceledOnTouchOutside(true);

        //alert.setContentView(view);
        mAlertDialog.show();
        mAlertDialog.setContentView(mView);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = mAlertDialog.getWindow();

        lp.copyFrom(window.getAttributes());
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);
    }

    private void dismissDialog() {
        mAlertDialog.dismiss();
    }

    private void updatePager(View view, float offset) {
        if (view == null)
            return;

        float adjustedOffset = (1.0f - 0.0f) * (offset - MIN_OFFSET) / (MAX_OFFSET - MIN_OFFSET) + 0.0f;
        adjustedOffset = adjustedOffset > 1f ? 1f : adjustedOffset;
        adjustedOffset = adjustedOffset < 0f ? 0f : adjustedOffset;

        float alpha = adjustedOffset * (1f - MIN_ALPHA) + MIN_ALPHA;
        float scale = adjustedOffset * (1f - MIN_SCALE) + MIN_SCALE;

        view.setAlpha(alpha);
        view.setScaleY(scale);
    }

    private class ViewPagerAdapter extends PagerAdapter {

        private static final String DEFAULT_MIN_DATE = "01/01/1992";
        private static final String DEFAULT_MAX_DATE = "01/01/2100";

        private Calendar mMinDate = getCalendarObjectForLocale(DEFAULT_MIN_DATE, Locale.getDefault());
        private Calendar mMaxDate = getCalendarObjectForLocale(DEFAULT_MAX_DATE, Locale.getDefault());

        private Pair<Integer, Calendar> initialPageAndDay;

        private Query query;
        private FirebaseFirestore firestore;
        private ToDoAdapter adapter;


        private int TOTAL_COUNT;

        ViewPagerAdapter(Calendar selectedDate, List<ToDoModel> eventList) {
            mEventList = eventList;

            // Total number of pages (between min and max date)
            TOTAL_COUNT = (int) TimeUnit.MILLISECONDS.toDays(Math.abs(mMaxDate.getTimeInMillis() - mMinDate.getTimeInMillis()));

            int initialPosition = (int) TimeUnit.MILLISECONDS.toDays(Math.abs(selectedDate.getTimeInMillis() - mMinDate.getTimeInMillis()));

            initialPageAndDay = new Pair<>(initialPosition, selectedDate);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {

            final Calendar day = (Calendar) initialPageAndDay.second.clone();
            day.add(Calendar.DAY_OF_MONTH, position - initialPageAndDay.first);

            LayoutInflater inflater = LayoutInflater.from(collection.getContext());
            View view = inflater.inflate(R.layout.pager_calendar_day, collection, false);
            view.setTag(position);

            TextView tvDay = view.findViewById(R.id.tv_calendar_day);
            TextView tvDayOfWeek = view.findViewById(R.id.tv_calendar_day_of_week);
            RecyclerView rvDay = view.findViewById(R.id.rv_calendar_events);
            View rlNoAlerts = view.findViewById(R.id.rl_no_events);
            View fabCreate = view.findViewById(R.id.fab_create_event);


            List<ToDoModel> eventList = getCalendarEventsOfDay(day);
            List<ToDoModel> mList  = new ArrayList<>();




            if (diffYMD(day, sToday) == -1) {
                fabCreate.setVisibility(View.INVISIBLE);
                fabCreate.setOnClickListener(null);
            }
            else {
                fabCreate.setVisibility(View.VISIBLE);
                fabCreate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null)
                            mListener.onCreateEvent(day);
                    }
                });
            }

            tvDay.setText(new SimpleDateFormat("d", Locale.getDefault()).format(day.getTime()));
            tvDayOfWeek.setText(new SimpleDateFormat("EEEE", Locale.getDefault()).format(day.getTime()));

            rvDay.setLayoutManager(new LinearLayoutManager(collection.getContext(), LinearLayoutManager.VERTICAL, false));
            rvDay.setAdapter(new ToDoAdapter(calendarDialog,eventList));
            rvDay.setVisibility(eventList.size() == 0? View.GONE : View.VISIBLE);

            firestore = FirebaseFirestore.getInstance();


            rlNoAlerts.setVisibility(eventList.size() == 0? View.VISIBLE : View.GONE);

            collection.addView(view);

            return new ViewHolder(view);


        }



        @Override
        public int getCount() {
            return TOTAL_COUNT;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            final ViewHolder holder = (ViewHolder) object;
            return view == holder.container;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(((ViewHolder) object).container);
        }

        private void setSelectedDate(Calendar selectedDate) {
            int position = (int) TimeUnit.MILLISECONDS.toDays(Math.abs(selectedDate.getTimeInMillis() - mMinDate.getTimeInMillis()));

            initialPageAndDay = new Pair<>(position, selectedDate);
        }

        private class ViewHolder {
            final View container;

            ViewHolder(View container) {
                this.container = container;
            }
        }

        private List<ToDoModel> getCalendarEventsOfDay(Calendar day) {
            List<ToDoModel> eventList = new ArrayList<>();
            for (ToDoModel e : mEventList) {
                if (diffYMD(e.getDate(), day) == 0)
                    eventList.add(e);
            }
            return eventList;
        }

        private Calendar getCalendarObjectForLocale(String date, Locale locale) {
            Calendar calendar = Calendar.getInstance(locale);
            DateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            if (date == null || date.isEmpty()) {
                return calendar;
            }

            try {
                final Date parsedDate = DATE_FORMATTER.parse(date);
                if (calendar == null)
                    calendar = Calendar.getInstance();
                calendar.setTime(parsedDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return calendar;
        }

    }








    public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.MyViewHolder> implements Filterable {

        private List<ToDoModel> todoList;
        private List<ToDoModel> todoFilter;
        private CalendarDialog activity;
        private FirebaseFirestore firestore;
        private FirebaseUser user;
        OnDialogCloseListener onDialogCloseListener;
        private List<ToDoModel> filteredList= new ArrayList<ToDoModel>();

        //filters
        private List<String> filters = new ArrayList<String>();


        public ToDoAdapter(CalendarDialog mainActivity, List<ToDoModel>todoList){
            this.todoList=todoList;
            activity=mainActivity;
            todoFilter= new ArrayList<>(todoList);
            this.onDialogCloseListener=onDialogCloseListener;




        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.each_task,parent,false);
            user = FirebaseAuth.getInstance().getCurrentUser();
            firestore=FirebaseFirestore.getInstance();
            return new MyViewHolder(view);
        }


        public void deleteTask(int position){
            ToDoModel toDoModel = todoList.get(position);
            firestore.collection("task").document(user.getUid()).collection("myTasks").document(toDoModel.TaskId).delete();
            todoList.remove(position);
            notifyItemRemoved(position);
        }

        public Context getContext(){
            return getContext();
        }




        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            ToDoModel toDoModel = todoList.get(position);
            holder.mCheckBox.setText(toDoModel.getTask());
            holder.mDueDateTv.setText("Due On " + toDoModel.getDue());
            holder.mNumberPicker.setText(Integer.toString(toDoModel.getPriority()));
            holder.mNotes.setText(toDoModel.getNotes());

            holder.mCheckBox.setChecked(toBoolean(toDoModel.getStatus()));

            holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        firestore.collection("task").document(user.getUid()).collection("myTasks").document(toDoModel.TaskId).update("status", 1);
                    } else {
                        firestore.collection("task").document(user.getUid()).collection("myTasks").document(toDoModel.TaskId).update("status", 0);

                    }
                }
            });

        }

        private boolean toBoolean(int status){
            return status !=0;
        }

        @Override
        public int getItemCount() {

            return todoList.size();
        }

        @Override
        public Filter getFilter() {
            return filter;
        }


        private Filter filter = new Filter() {

            //run on background thread
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                if (todoFilter.size() == 0) todoFilter = new ArrayList<>(todoList);
                List<ToDoModel> filteredList = new ArrayList<>();
                if (charSequence == null || charSequence.length()==0){
                    filteredList.addAll(todoFilter);
                }else{
                    String filterPattern = charSequence.toString().toLowerCase().trim();

                    for (ToDoModel item : todoFilter){
                        if (item.getTask().toLowerCase().contains(filterPattern)){
                            filteredList.add(item);
                        }
                    }
                }

                FilterResults results=new FilterResults();
                results.values=filteredList;

                return results;
            }

            //runs on ui thread
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults results) {
                todoList.clear();
                todoList.addAll((List)results.values);
                notifyDataSetChanged();

            }
        };
        public class MyViewHolder extends RecyclerView.ViewHolder{

            TextView mDueDateTv;
            CheckBox mCheckBox;
            TextView mNumberPicker;
            TextView mNotes;
            public MyViewHolder(@NonNull View itemView){
                super (itemView);

                mDueDateTv = itemView.findViewById(R.id.due_date_tv);
                mCheckBox = itemView.findViewById(R.id.mcheckbox);
                mNumberPicker=itemView.findViewById(R.id.textView);

            }
        }


    }




    public interface OnCalendarDialogListener {
        void onEventClick(ToDoModel event);
        void onCreateEvent(Calendar calendar);
    }

    private static int diffYMD(Calendar date1, Calendar date2) {
        if (date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH))
            return 0;

        return date1.before(date2) ? -1 : 1;
    }

    public static class Builder  {


        private final CalendarDialogParams P;

        public static Builder instance(Context context) {
            return new Builder(context);
        }

        private Builder(Context context) {
            P = new CalendarDialogParams(context);
        }

        public Builder setEventList(List<ToDoModel> calendarEventList) {
            P.mEventList = calendarEventList;
            return this;
        }

        public Builder setSelectedDate(Calendar selectedDate) {
            P.mSelectedDate = selectedDate;
            return this;
        }

        public Builder setOnItemClickListener(OnCalendarDialogListener listener) {
            P.mOnCalendarDialogListener = listener;
            return this;
        }

        public CalendarDialog create() {
            CalendarDialog calendarDialog1;
            calendarDialog1 = new CalendarDialog(P.mContext);

            P.apply(calendarDialog1);

            return calendarDialog1;
        }
    }

    public static CalendarDialog createx(){
        return new CalendarDialog(mContext);
    }

    private static class CalendarDialogParams {

        Context mContext;

        Calendar mSelectedDate = sToday;
        List<ToDoModel> mEventList = new ArrayList<>();

        OnCalendarDialogListener mOnCalendarDialogListener;

        CalendarDialogParams(Context context) {
            mContext = context;
        }


        public void apply(CalendarDialog calendarDialog1) {
            calendarDialog1.setSelectedDate(mSelectedDate);
            calendarDialog1.setEventList(mEventList);
            calendarDialog1.setOnCalendarDialogListener(mOnCalendarDialogListener);
        }


    }
}