package com.example.gopzchat.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gopzchat.R;
import com.example.gopzchat.databinding.ActivityIntroBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

public class IntroActivity extends AppCompatActivity {

    ActivityIntroBinding binding;

    Animation topAnim, bottomAnim;
    ImageView image;
    TextView logo;
    private static final int SPLASH_SCREEN = 2000;
    public static final String MyPREFERENCES = "gopzchat" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // To check if intro enabled
        SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        boolean intro = sharedpreferences.getBoolean("intro", true);
        if(!intro){
            Intent intent = new Intent(IntroActivity.this, PhoneNumberActivity.class);
            startActivity(intent);
            finish();
        }

        // To remove top bar
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        // Animations
        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this,R.anim.bottom_animation);

        // Hooks
        image = findViewById(R.id.imageView);
        logo = findViewById(R.id.textView);

        image.setAnimation(topAnim);
        logo.setAnimation(bottomAnim);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(IntroActivity.this, PhoneNumberActivity.class);
//                startActivity(intent);
                finish();
                Pair[] pairs = new Pair[2];
                pairs[0] = new Pair<View, String>(image, "logo_image");
                pairs[1] = new Pair<View, String>(logo, "logo_text");

                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(IntroActivity.this, pairs);
                startActivity(intent, options.toBundle());

            }
        }, SPLASH_SCREEN);


    }

}