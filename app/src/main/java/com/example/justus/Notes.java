package com.example.justus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class Notes extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FloatingActionButton floatingActionButton;

    private DatabaseReference reference;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String onlineUserID;
    private ProgressDialog loader;

    private String key = "";
    private String note;
    private String desc;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.TODO:
                Intent intent1 = new Intent(getApplicationContext(), Home.class);
                startActivity(intent1);
                finish();
                return true;
            case R.id.Seznam:
                Intent intent2 = new Intent(getApplicationContext(), Notes.class);
                startActivity(intent2);
                finish();
                return true;

            case R.id.main_page:
                Intent intent3 = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent3);
                finish();
                return true;
        }


        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recyclerView = findViewById(R.id.recylclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        loader = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        onlineUserID = mUser.getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("notes").child(onlineUserID);

        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });
    }

    private void addTask(){
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        View myView = inflater.inflate(R.layout.input_notes, null);
        myDialog.setView(myView);

        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        final EditText note = myView.findViewById(R.id.note_name);
        final EditText description = myView.findViewById(R.id.description_notes);
        Button save = myView.findViewById(R.id.btn_save_note);
        Button cancel = myView.findViewById(R.id.btn_cancel_note);

        save.setOnClickListener(view -> {
            String mTask = note.getText().toString().trim();
            String mDescription = description.getText().toString().trim();
            String id = reference.push().getKey();
            String date = DateFormat.getDateInstance().format(new Date());
            if (TextUtils.isEmpty(mTask)){
                note.setError("Note required");
                return;
            }
            if(TextUtils.isEmpty(mDescription)){
                note.setError("Description required");
                return;
            } else{
                loader.setMessage("Adding the data");
                loader.setCanceledOnTouchOutside(false);
                loader.show();

                Model model = new Model(mTask, mDescription, id, date);

                reference.child(id).setValue(model).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task1) {
                        if (task1.isSuccessful()){
                            Toast.makeText(Notes.this, "Insert successful", Toast.LENGTH_SHORT).show();
                            loader.dismiss();
                        }
                        else {
                            String error = task1.getException().toString();
                            Toast.makeText(Notes.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                            loader.dismiss();
                        }
                    }
                });

            }
            dialog.dismiss();
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Model> options = new FirebaseRecyclerOptions.Builder<Model>().setQuery(reference, Model.class).build();

        FirebaseRecyclerAdapter<Model, MyViewHolder> adapter = new FirebaseRecyclerAdapter<Model, MyViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position, @NonNull Model model) {

                holder.setDate(model.getDate());
                holder.setDesc(model.getDescription());
                holder.setTask(model.getTask());

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        key = getRef(position).getKey();
                        note = model.getTask();
                        desc = model.getDescription();

                        updateTask();

                    }
                });
            }

            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.get_note, parent, false);
                return new MyViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
    public static class MyViewHolder extends RecyclerView.ViewHolder{
        View mView;

        public MyViewHolder(@NonNull View itemView){
            super(itemView);
            mView = itemView;

        }
        public void setTask(String task){
            TextView taskTextView = mView.findViewById(R.id.noteShow);
            taskTextView.setText(task);

        }
        public void setDesc(String description){
            TextView descTextView = mView.findViewById(R.id.noteTAB);
            descTextView.setText(description);

        }
        public void setDate(String date){
            TextView dateTextView = mView.findViewById(R.id.dateTab);
            dateTextView.setText(date);
        }
    }
    private void updateTask(){

        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.update_note, null);
        myDialog.setView(view);

        AlertDialog dialog = myDialog.create();

        EditText mTask = view.findViewById(R.id.editNote_name);
        EditText mDescription = view.findViewById(R.id.editNote);

        mTask.setText(note);
        mTask.setSelection(note.length());

        mDescription.setText(desc);
        mDescription.setSelection(desc.length());

        Button deleteBTN = view.findViewById(R.id.btn_delete_note);
        Button updateBTN = view.findViewById(R.id.btn_update_note);

        updateBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                note = mTask.getText().toString().trim();
                desc = mDescription.getText().toString().trim();

                String date = DateFormat.getDateInstance().format(new Date());

                Model model = new Model(note, desc, key, date);

                reference.child(key).setValue(model).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()){

                            Toast.makeText(Notes.this, "Update successful", Toast.LENGTH_SHORT).show();
                        }else{
                            String error = task.getException().toString();
                            Toast.makeText(Notes.this, "Update failed" + error, Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                dialog.dismiss();

            }
        });

        deleteBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                reference.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful()){
                            Toast.makeText(Notes.this, "Delete successful", Toast.LENGTH_SHORT).show();
                        }else{
                            String error = task.getException().toString();
                            Toast.makeText(Notes.this, "Delete failed" + error, Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                dialog.dismiss();

            }
        });


        dialog.show();




    }
}