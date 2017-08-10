package com.milburn.mytlc;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import java.util.HashMap;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private Credentials credentials;
    private EditText mUser;
    private TextInputLayout mLayoutUser;
    private EditText mPass;
    private TextInputLayout mLayoutPass;
    private CheckBox mSaveCreds;
    private HashMap<String, String> mUserPassMap;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setTheme(new PrefManager(this).getTheme());
        initLogin();
    }

    private void initLogin() {
        setContentView(R.layout.activity_login);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        credentials = new Credentials(this);
        mUser = (EditText) findViewById(R.id.edit_username);
        mLayoutUser = (TextInputLayout) findViewById(R.id.inputlayout_username);
        mPass = (EditText) findViewById(R.id.edit_password);
        mLayoutPass = (TextInputLayout) findViewById(R.id.inputlayout_password);
        mSaveCreds = (CheckBox) findViewById(R.id.check_savecreds);

        if (!credentials.getUsername().contentEquals("DEFAULT")) {
            mUser.setText(credentials.getUsername());
        }

        Button mLoginButton = (Button) findViewById(R.id.button_login);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                PostLoginAPI loginAPI = new PostLoginAPI(LoginActivity.this, new PostLoginAPI.AsyncResponse() {
                    @Override
                    public void processFinish(List<Shift> shiftList) {
                        if (shiftList != null && !shiftList.isEmpty()) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);

                            if (mSaveCreds.isChecked()) {
                                credentials.setCredentials(mUserPassMap);
                            } else {
                                credentials.setUsername(mUserPassMap.get("Username"));
                                intent.putExtra("Password", mPass.getText().toString());
                            }
                            intent.putExtra("Schedule", credentials.getSerialSchedule(shiftList));

                            startActivity(intent);
                            finish();
                        }
                    }
                });

                if (!errorStatus()) {
                    if (credentials.userExists() && !credentials.getUsername().equals(mUser.getText().toString())) {
                        credentials.logout();
                    }
                    mUserPassMap = new HashMap<>();
                    mUserPassMap.put("Username", mUser.getText().toString());
                    mUserPassMap.put("Password", mPass.getText().toString());
                    loginAPI.execute(mUserPassMap);
                }
            }
        });
    }

    private boolean errorStatus() {
        if (mUser.getText().length() == 0) {
            mLayoutUser.setError("A username is required");
        } else {
            mLayoutUser.setErrorEnabled(false);
        }

        if (mPass.getText().length() == 0) {
            mLayoutPass.setError("A password is required");
        } else {
            mLayoutPass.setErrorEnabled(false);
        }

        return mLayoutUser.isErrorEnabled() || mLayoutPass.isErrorEnabled();
    }

    private void hideKeyBoard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if(inputManager.isAcceptingText()) {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Drawable drawable = mToolbar.getOverflowIcon();
        if (drawable != null) {
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            mToolbar.setOverflowIcon(drawable);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete:
                credentials.setUsername(null);
                mUser.setText("");
                break;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}