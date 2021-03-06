package com.example.redwan.firebasechatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText email, pass;
    private Button button;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProgressDialog mprogress;
    private Toolbar toolbar;

    ConstraintLayout constraintLayout;
    AnimationDrawable animationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.email);
        pass = findViewById(R.id.pass_editText);
        button = findViewById(R.id.button5);

        mprogress = new ProgressDialog(this);
        toolbar = findViewById(R.id.toolbarId_login);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        constraintLayout = findViewById(R.id.myLayout_login);
        animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(4500);
        animationDrawable.setExitFadeDuration(4500);
        animationDrawable.start();

        mAuthListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if(firebaseAuth.getCurrentUser() != null){
                    Intent mIntent = new Intent(LoginActivity.this, MainActivity.class);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);      // ber kore deoyar jonno
                    startActivity(mIntent);
                    finish();
                }

            }
        };
        
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignIn();
            }
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    
    public void startSignIn() {
        String ema = email.getText().toString();
        String pas = pass.getText().toString();
        if(!TextUtils.isEmpty(ema) && !TextUtils.isEmpty(pas)) {

            if(pas.length() < 6 ){
                Toast.makeText(LoginActivity.this, "Password Should be At least 6 characters", Toast.LENGTH_LONG).show();
            }
            else {
                mprogress.setMessage("Sign in...");
                mprogress.setCanceledOnTouchOutside(false);
                mprogress.show();
                mAuth.signInWithEmailAndPassword(ema, pas).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            mprogress.cancel();
                            Toast.makeText(LoginActivity.this, "Email or Password is wrong", Toast.LENGTH_LONG).show();
                        }
                        mprogress.cancel();
                    }
                });
            }
        }
        else{
            Toast.makeText(LoginActivity.this, "Email or Password is missing", Toast.LENGTH_LONG).show();
        }
    }
}
