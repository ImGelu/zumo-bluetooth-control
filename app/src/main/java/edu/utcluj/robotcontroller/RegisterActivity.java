package edu.utcluj.robotcontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import edu.utcluj.robotcontroller.room.User;
import edu.utcluj.robotcontroller.room.UserDao;
import edu.utcluj.robotcontroller.room.UserDataBase;

public class RegisterActivity extends AppCompatActivity {

    private EditText userEmail, userPassword, userPasswordVerification;
    private TextInputLayout userEmailLayout;
    private TextInputLayout userPasswordLayout;
    private TextInputLayout userPasswordVerificationLayout;
    private LinearLayout linearLayout;

    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();

        setContentView(R.layout.activity_register);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        UserDataBase userDataBase = Room.databaseBuilder(this, UserDataBase.class, GlobalData.USERS_DB_NAME).allowMainThreadQueries().build();
        userDao = userDataBase.getUserDao();

        userEmail = findViewById(R.id.text_field_email_value);
        userPassword = findViewById(R.id.text_field_password_value);
        userPasswordVerification = findViewById(R.id.text_field_password_verification_value);

        userEmailLayout = findViewById(R.id.text_field_email);
        userPasswordLayout = findViewById(R.id.text_field_password);
        userPasswordVerificationLayout = findViewById(R.id.text_field_password_verification);

        linearLayout = findViewById(R.id.linear_layout_login);

        if(preferences.getBoolean(GlobalData.DARK_MODE, false)){
            linearLayout.setBackgroundTintList(ColorStateList.valueOf(0xff303030));
            userEmail.setTextColor(Color.WHITE);
            userPassword.setTextColor(Color.WHITE);
            userPasswordVerification.setTextColor(Color.WHITE);
        }

        userEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!userEmail.getText().toString().isEmpty()) userEmailLayout.setError(null);
                else userEmailLayout.setError(getString(R.string.error_required));

                if (!TextUtils.isEmpty(s) && !Patterns.EMAIL_ADDRESS.matcher(s).matches())
                    userEmailLayout.setError(getString(R.string.error_email_pattern));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        userPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!userPassword.getText().toString().isEmpty()) {
                    if (!userPasswordVerification.getText().toString().isEmpty() && !userPasswordVerification.getText().toString().equals(userPassword.getText().toString()))
                        userPasswordVerificationLayout.setError(getString(R.string.error_password_verification));
                    else userPasswordVerificationLayout.setError(null);

                    userPasswordLayout.setError(null);
                } else userPasswordLayout.setError(getString(R.string.error_required_password));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        userPasswordVerification.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!userPasswordVerification.getText().toString().isEmpty())
                    userPasswordVerificationLayout.setError(null);

                if (!userPasswordVerification.getText().toString().equals(userPassword.getText().toString()))
                    userPasswordVerificationLayout.setError(getString(R.string.error_password_verification));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    public void createAccount(View view) {
        String email = userEmail.getText().toString().trim();
        String password = userPassword.getText().toString().trim();
        String passwordVerification = userPasswordVerification.toString().trim();

        if (!email.isEmpty() && !password.isEmpty() && !passwordVerification.isEmpty()) {
            User existingUser = userDao.getUserByEmail(email);

            if (existingUser != null) {
                Snackbar.make(view, getString(R.string.email_taken), BaseTransientBottomBar.LENGTH_SHORT).show();
            } else {
                if (!userPasswordVerification.getText().toString().equals(userPassword.getText().toString())) {
                    Snackbar.make(view, R.string.error_password_verification, BaseTransientBottomBar.LENGTH_SHORT);
                } else {
                    User newUser = new User(email, password);
                    long newUserId = userDao.insert(newUser);
                    newUser.setId(newUserId);

                    GlobalData.setLoggedInUser(newUser, view.getContext());

                    Intent intent = new Intent(view.getContext(), HomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        } else {
            Snackbar.make(view, R.string.error_unfilled, BaseTransientBottomBar.LENGTH_SHORT).show();
        }
    }
}