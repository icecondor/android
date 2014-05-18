package com.icecondor.eaglet.ui.login;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

public class LoginFragment extends Fragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "LoginFragment onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "LoginFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_login,
                               container, false);
        return rootView;
    }
}
