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

public class LoginEmailFragment extends Fragment {
    private EditText emailField;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "LoginFragmentEmail onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "LoginFragmentEmail onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_login_prompt,
                               container, false);
        emailField = (EditText)rootView.findViewById(R.id.login_email_field);
        emailField.setOnEditorActionListener((Main)getActivity());
        emailField.requestFocus();
        return rootView;
    }

    public void enableLoginField() {
        if(emailField != null) {
            emailField.setEnabled(true);
        }
    }

    public void disableLoginField() {
        //emailField.setEnabled(false);
        emailField.setText("");
    }
}
