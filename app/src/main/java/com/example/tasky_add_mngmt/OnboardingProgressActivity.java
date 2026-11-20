package com.example.tasky_add_mngmt;   

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OnboardingProgressActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_progress);   

        View btnGetStarted = findViewById(R.id.btnGetStarted);
        View tvSkip = findViewById(R.id.tvSkip);

        btnGetStarted.setOnClickListener(v -> goHome());
        tvSkip.setOnClickListener(v -> goHome());
    }

    
    
    
    private void goHome() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
