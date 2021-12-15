package tp.edu.taskapp.Model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;


public class ToDoModel extends tp.edu.taskapp.Model.TaskId implements Parcelable {

    private String task, due,notes;
    private boolean isCompleted;
    private int status, priority,mColor;
    private Calendar mDate;

    public ToDoModel(String task, String due, String notes, int status, int priority, boolean isCompleted, Calendar date, int color, String id){
        this.task =task;
        this.due=due;
        this.notes=notes;
        this.status=status;
        this.priority =priority;
        this.isCompleted=isCompleted;
        this.mDate=date;
        this.mColor=color;
        this.TaskId=id;
    }

    public ToDoModel(){

    }

    protected ToDoModel(Parcel in) {
        task = in.readString();
        due = in.readString();
        notes = in.readString();
        isCompleted = in.readByte() != 0;
        mDate=(Calendar) in.readSerializable();
        status = in.readInt();
        priority = in.readInt();
        mColor=in.readInt();
        TaskId=in.readString();

    }

    public static final Creator<ToDoModel> CREATOR = new Creator<ToDoModel>() {
        @Override
        public ToDoModel createFromParcel(Parcel in) {
            return new ToDoModel(in);
        }

        @Override
        public ToDoModel[] newArray(int size) {
            return new ToDoModel[size];
        }
    };

    public String getTaskId(){
        return TaskId;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task){
        this.task=task;
    }

    public String getDue() {
        return due;
    }

    public String getNotes(){
        return notes;
    }

    public int getStatus() {
        return status;
    }

    public int getPriority(){
        return priority;
    }

    public boolean isCompleted(){
        return isCompleted;
    }

    public Calendar getDate(){
        return mDate;
    }

    public int getColor() {
        return mColor;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(task);
        parcel.writeString(due);
        parcel.writeString(notes);
        parcel.writeByte((byte) (isCompleted ? 1 : 0));
        parcel.writeInt(status);
        parcel.writeInt(priority);
        parcel.writeSerializable(mDate);
        parcel.writeInt(mColor);
        parcel.writeString(TaskId);
        //save the state of an object in order to be able to recreate it when needed.
    }
}
