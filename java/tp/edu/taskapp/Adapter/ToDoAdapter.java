package tp.edu.taskapp.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import tp.edu.taskapp.AddNewTask;

import tp.edu.taskapp.MainActivity;
import tp.edu.taskapp.Model.ToDoModel;
import tp.edu.taskapp.OnDialogCloseListener;
import tp.edu.taskapp.R;


public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.MyViewHolder> implements Filterable {

    public List<ToDoModel> todoList;
    private List<ToDoModel> todoFilter;
    private MainActivity activity;
    private FirebaseFirestore firestore;
    private FirebaseUser user;
    OnDialogCloseListener onDialogCloseListener;

    public ToDoAdapter(MainActivity mainActivity, List<ToDoModel>todoList){
        this.todoList=todoList;
        activity=mainActivity;
        todoFilter= new ArrayList<>(todoList);
        this.onDialogCloseListener=onDialogCloseListener;




    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.each_task,parent,false);
        user = FirebaseAuth.getInstance().getCurrentUser();
        firestore=FirebaseFirestore.getInstance();



        return new MyViewHolder(view);
    }


    public void deleteTask(int position){
        //get user and task and delete off from user
        ToDoModel toDoModel = todoList.get(position);
        firestore.collection("task").document(user.getUid()).collection("myTasks").document(toDoModel.TaskId).delete();
        todoList.remove(position);
        notifyItemRemoved(position);
    }

    public Context getContext(){
        return activity;
    }

    public void editTask(int position){

        //getting task info to edit
        ToDoModel toDoModel = todoList.get(position);

        Bundle bundle=new Bundle();
        bundle.putString("task",toDoModel.getTask());
        bundle.putString("due",toDoModel.getDue());
        bundle.putString("notes",toDoModel.getNotes());
        bundle.putInt("priority",toDoModel.getPriority());
        bundle.putString("id",toDoModel.TaskId);


        AddNewTask addNewTask = new AddNewTask();
        addNewTask.setArguments(bundle);
        addNewTask.show(activity.getSupportFragmentManager(),addNewTask.getTag());
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        //extract content from list and display content
        ToDoModel toDoModel = todoList.get(position);
        holder.mCheckBox.setText(toDoModel.getTask());
        holder.mDueDateTv.setText("Due On " + toDoModel.getDue());
        holder.mNumberPicker.setText(Integer.toString(toDoModel.getPriority()));

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
    //filter the results

    private Filter filter = new Filter() {

        //run on background thread
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            //create new arraylist
            if (todoFilter.size() == 0) todoFilter = new ArrayList<>(todoList);
            List<ToDoModel> filteredList = new ArrayList<>();

            //letter filter
            if (charSequence == null || charSequence.length()==0){
                filteredList.addAll(todoFilter);//adding filtered to new list
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

            //show list
            todoList.clear();
            todoList.addAll((List)results.values);
            notifyDataSetChanged();

        }
    };
    public class MyViewHolder extends RecyclerView.ViewHolder{
        //a wrapper around a View that contains the layout for an individual item in the list
        TextView mDueDateTv;
        CheckBox mCheckBox;
        TextView mNumberPicker;

        public MyViewHolder(@NonNull View itemView){
            super (itemView);

            mDueDateTv = itemView.findViewById(R.id.due_date_tv);

            mCheckBox = itemView.findViewById(R.id.mcheckbox);
            mNumberPicker=itemView.findViewById(R.id.textView);

        }
    }


}
