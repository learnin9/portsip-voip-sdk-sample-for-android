package com.portsip.sipsample.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;


public class BaseFragment extends Fragment implements View.OnTouchListener{
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }
}
