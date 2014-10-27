package com.icecondor.nest.ui.login;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.icecondor.nest.Constants;
import com.icecondor.nest.R;

public class UserEditFragment extends Fragment {
    private EditText usernameEdit;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.APP_TAG, "UserEditFragment onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(Constants.APP_TAG, "UserEditFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_user_edit,
                               container, false);
        usernameEdit = (EditText)rootView.findViewById(R.id.login_user_username);
        usernameEdit.setOnEditorActionListener((Main)getActivity());

        return rootView;
    }

    public void setUsernameText(String text) {
        // this can be called before statusView exists
        if(usernameEdit != null) {
            usernameEdit.setText(text);
        }
    }

    public String getUsernameText(String text) {
        String username = null;
        if(usernameEdit != null) {
            username = usernameEdit.getText().toString();
        }
        return username;
    }
}
