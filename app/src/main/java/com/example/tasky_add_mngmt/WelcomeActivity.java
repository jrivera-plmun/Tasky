package com.example.tasky_add_mngmt;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private float downX;
    private float downY;
    private float minSwipeDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        
        minSwipeDistance = 60 * getResources().getDisplayMetrics().density;
    }

    
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                float deltaX = event.getX() - downX;
                float deltaY = event.getY() - downY;

                
                if (deltaX < -minSwipeDistance && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(new Intent(this, OrganizationActivity.class));
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}