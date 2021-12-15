package tp.edu.taskapp.Adapter;

import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tp.edu.taskapp.Model.NoteModel;
import tp.edu.taskapp.Notes.NoteDetails;
import tp.edu.taskapp.R;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
    List<String> titles;
    List<String> content;

    //set item
    public NotesAdapter(List<String> title, List<String> content){
        this.titles = title;
        this.content = content;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //get each item list
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_view_layout,parent,false);
        return new ViewHolder(view);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        //extract content from list and display content
            holder.noteTitle.setText(titles.get(position));
            holder.noteContent.setText(content.get(position));
            final int code = getRandomColor();
            holder.mCardView.setCardBackgroundColor(holder.view.getResources().getColor(code,null));

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(v.getContext(), NoteDetails.class);
                    i.putExtra("title",titles.get(position));
                    i.putExtra("content",content.get(position));
                    i.putExtra("code",code);
                    v.getContext().startActivity(i);
                }
            });
    }

    private int getRandomColor() {
        //randomise color
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
    public int getItemCount() {
        return titles.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder {
        //a wrapper around a View that contains the layout for an individual item in the list
        TextView noteTitle,noteContent;
        View view;
        CardView mCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.notetitle);
            noteContent = itemView.findViewById(R.id.notecontent);
            mCardView = itemView.findViewById(R.id.noteCardview);
            view = itemView;
        }
    }
}
