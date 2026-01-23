package com.example.proyectointegrador;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectointegrador.LoginActivity;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.MainActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imgLogo = findViewById(R.id.imgLogo);
        progressBar = findViewById(R.id.progressBar);

        animateLogo();
        animateProgressAndGoNext();
    }

    private void animateLogo() {
        // alpha 0 -> 1
        ObjectAnimator fade = ObjectAnimator.ofFloat(imgLogo, View.ALPHA, 0f, 1f);
        fade.setDuration(700);

        ObjectAnimator rise = ObjectAnimator.ofFloat(imgLogo, View.TRANSLATION_Y, imgLogo.getTranslationY(), 0f);
        rise.setDuration(900);

        // scale 0.92 -> 1
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(imgLogo, View.SCALE_X, 0.92f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(imgLogo, View.SCALE_Y, 0.92f, 1f);
        scaleX.setDuration(900);
        scaleY.setDuration(900);

        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.playTogether(fade, rise, scaleX, scaleY);
        set.start();
    }

    private void animateProgressAndGoNext() {
        ObjectAnimator progress = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progress.setDuration(1600);
        progress.start();

        progress.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
