package com.icecondor.eaglet.ui.login;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.icecondor.eaglet.Constants;
import com.icecondor.eaglet.R;

public class TokenValidateFragment extends Fragment {
    private TextView status;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "TokenValidateFragment onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "TokenValidateFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_login_token,
                               container, false);
        status = (TextView)rootView.findViewById(R.id.login_token_status);

        return rootView;
    }

    public void indicateProcessToken() {
        status.setText("Validating Token...");
    }

    public void indicateSuccess() {
        status.setText("Token Accepted!");
    }

    public void indicateFail() {
        status.setText("Token Denied.");
    }

    public void indicateCommErr() {
        status.setText("Network timeout. Please try again.");
    }

    public void indicateUserDetailFetch() {
        status.setText("Gathering user details...");
    }
}
