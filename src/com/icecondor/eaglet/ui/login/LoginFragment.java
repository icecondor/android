package com.icecondor.eaglet.ui.login;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

public class LoginFragment extends Fragment implements OnEditorActionListener {
    private EditText emailField;
    private TextView statusView;

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
        statusView = (TextView)rootView.findViewById(R.id.login_status_msg);
        emailField = (EditText)rootView.findViewById(R.id.login_email_field);
        emailField.setOnEditorActionListener(this);
        return rootView;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(v.getId() == R.id.login_email_field) {
            if(actionId == EditorInfo.IME_ACTION_SEND) {
                Log.d(Constants.APP_TAG, "LoginFragment: action: "+actionId+" emailField "+v.getText());
            }
        }
        return false;
    }

    public void setStatusText(String text) {
        statusView.setText(text);
    }
}
