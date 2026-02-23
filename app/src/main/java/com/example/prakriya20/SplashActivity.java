package com.example.prakriya20;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView logo;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.logoBig);

        // When the logo is clicked, play a small shrink/fade animation then start MainActivity
        logo.setOnClickListener(v -> {
            if (isAnimating) return;
            isAnimating = true;
            playClickAnimationAndOpenNext();
        });
    }

    private void playClickAnimationAndOpenNext() {
        // Scale down slightly and fade out
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 0.75f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 0.75f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logo, "alpha", 1f, 0f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(logo, "translationY", 0f, -80f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha, translateY);
        set.setDuration(380);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Start next activity after animation ends
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(i);
                // apply cross-fade transition for smoother effect
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                // finish splash so user can't go back to it
                finish();
            }
        });
        set.start();
    }
}
