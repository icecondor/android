package com.icecondor.eaglet.ui.login;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

public class LoginPassFragment extends Fragment {
    private EditText passwordField;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "LoginPassFragment onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "LoginPassFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_login_password,
                               container, false);
        passwordField = (EditText)rootView.findViewById(R.id.login_password_field);
        passwordField.setOnEditorActionListener((Main)getActivity());
        passwordField.requestFocus();
        return rootView;
    }

}
