package com.example.gopzchat.Adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gopzchat.Activities.MainActivity;
import com.example.gopzchat.Models.Status;
import com.example.gopzchat.Models.UserStatus;
import com.example.gopzchat.R;
import com.example.gopzchat.databinding.DeleteDialogBinding;
import com.example.gopzchat.databinding.DeleteStatusBinding;
import com.example.gopzchat.databinding.ItemStatusBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import omari.hamza.storyview.StoryView;
import omari.hamza.storyview.callback.StoryClickListeners;
import omari.hamza.storyview.model.MyStory;

public class TopStatusAdapter extends RecyclerView.Adapter<TopStatusAdapter.TopStatusViewHolder> {

    Context context;
    ArrayList<UserStatus> userStatuses;
    public static final String MyPREFERENCES = "gopzchat" ;
    FirebaseStorage storage;
    ProgressDialog dialog;

    public TopStatusAdapter(Context context, ArrayList<UserStatus> userStatuses)
    {
        this.context = context;
        this.userStatuses = userStatuses;
    }

    @NonNull
    @Override
    public TopStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_status, parent, false);
        return new TopStatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopStatusViewHolder holder, int position) {

        UserStatus userStatus = userStatuses.get(position);
        int statusCount = userStatus.getStatuses().size();
        Status lastStatus = userStatus.getStatuses().get(statusCount-1);
        String statusUserNumber = userStatus.getPhoneNumber();
        Glide.with(context).load(lastStatus.getImageUrl()).into(holder.binding.image);
        holder.binding.circularStatusView.setPortionsCount(statusCount);

        holder.binding.circularStatusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<MyStory> myStories = new ArrayList<>();
                int index = 0;
                for(Status status: userStatus.getStatuses()){
                    myStories.add(new MyStory(status.getImageUrl()));
                    holder.binding.circularStatusView.setPortionColorForIndex(index, Color.parseColor("#707070"));
                    index++;
                }

                new StoryView.Builder(((MainActivity)context).getSupportFragmentManager())
                        .setStoriesList(myStories) // Required
                        .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
                        .setTitleText(userStatus.getName()) // Default is Hidden
                        .setSubtitleText("") // Default is Hidden
                        .setTitleLogoUrl(userStatus.getProfileImage()) // Default is Hidden
                        .setStoryClickListeners(new StoryClickListeners() {
                            @Override
                            public void onDescriptionClickListener(int position) {
                                //your action
                            }

                            @Override
                            public void onTitleIconClickListener(int position) {
                                //your action
                            }
                        }) // Optional Listeners
                        .build() // Must be called before calling show method
                        .show();
            }
        });

        holder.binding.circularStatusView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                SharedPreferences sharedpreferences = context.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                String currentUserNumber = sharedpreferences.getString("currentUserNumber", "");

                if(!currentUserNumber.equals(statusUserNumber))
                {
                    return false;
                }

                View view = LayoutInflater.from(context).inflate(R.layout.delete_status, null);
                DeleteStatusBinding binding = DeleteStatusBinding.bind(view);
                AlertDialog aDialog = new AlertDialog.Builder(context)
                        .setView(binding.getRoot())
                        .create();

                aDialog.show();



                binding.statusDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                        Query statusQuery = ref.child("stories").orderByChild("phoneNumber").equalTo(userStatus.getPhoneNumber());

                        statusQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot appleSnapshot: dataSnapshot.getChildren()) {
                                    appleSnapshot.getRef().removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        storage = FirebaseStorage.getInstance();
                        for(Status status: userStatus.getStatuses()){
                            StorageReference photoRef = storage.getReferenceFromUrl(status.getImageUrl());
                            photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // File deleted successfully
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Uh-oh, an error occurred!
                                }
                            });
                        }


                        aDialog.dismiss();
                    }
                });

                binding.cancelStatusDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        aDialog.dismiss();
                    }
                });

                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return userStatuses.size();
    }

    public class TopStatusViewHolder extends RecyclerView.ViewHolder{

        ItemStatusBinding binding;

        public TopStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemStatusBinding.bind(itemView);
        }
    }
}
