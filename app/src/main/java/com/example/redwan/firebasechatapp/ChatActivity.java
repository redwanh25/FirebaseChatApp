package com.example.redwan.firebasechatapp;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import id.zelory.compressor.Compressor;

public class ChatActivity extends AppCompatActivity {

    private static final int GALLERY_PIC = 2;
    private Toolbar toolbar;
    private String mChatUser, chatWithUserName;
    private DatabaseReference databaseReference;
    private TextView chatUserName, chatOnlineStatus;
    private CircleImageView chatProfilePic;

    private DatabaseReference onlineDatabase, mRootRef, mRootRefUser;
    private FirebaseUser current;
    private StorageReference mImageStorage;

    private ImageButton mChatAddBtn;
    private ImageButton mChatSendBtn;
    //private TextInputLayout mChatMessageView;
    private String mCurrentUserId;

    private RecyclerView recyclerViewChat;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

//    private ProgressDialog mProgressBar;
    private ProgressBar progressBar;

    EmojiconEditText emojiconEditText;
    ImageView emojiImageView;
    View rootView;
    EmojIconActions emojIcon;

//    ConstraintLayout constraintLayout;
//    AnimationDrawable animationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

  //      mProgressBar = new ProgressDialog(this);
        progressBar = findViewById(R.id.progressBarId);

        mChatUser = getIntent().getStringExtra("key");
        chatWithUserName = getIntent().getStringExtra("user_name");

        toolbar = findViewById(R.id.toolbarId_chat);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setTitle(null);
        //       actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        chatUserName = findViewById(R.id.chat_user_name);
        chatProfilePic = findViewById(R.id.chat_profile_pic);
        chatOnlineStatus = findViewById(R.id.chat_online_status);

        chatUserName.setText(chatWithUserName);

        mChatAddBtn = findViewById(R.id.chat_add_btn);
        mChatSendBtn = findViewById(R.id.chat_send_btn);
 //       mChatMessageView = findViewById(R.id.chat_message_view);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRefUser = FirebaseDatabase.getInstance().getReference("users");
        mImageStorage = FirebaseStorage.getInstance().getReference();
        mCurrentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerViewChat = findViewById(R.id.chat_recyclerView);

        mLinearLayout = new LinearLayoutManager(this);
        mAdapter = new MessageAdapter(messagesList);
        recyclerViewChat.setHasFixedSize(true);
        recyclerViewChat.setLayoutManager(mLinearLayout);
        recyclerViewChat.setAdapter(mAdapter);

//        constraintLayout = findViewById(R.id.root_view);
//        animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
//        animationDrawable.setEnterFadeDuration(4500);
//        animationDrawable.setExitFadeDuration(4500);
//        animationDrawable.start();

        rootView = findViewById(R.id.root_view);
        emojiImageView = (ImageView) findViewById(R.id.emoji_btn);
        emojiconEditText = (EmojiconEditText) findViewById(R.id.emojicon_edit_text);
        emojIcon = new EmojIconActions(this, rootView, emojiconEditText, emojiImageView);
        emojIcon.ShowEmojIcon();
        emojIcon.setIconsIds(R.drawable.ic_action_keyboard, R.drawable.smiley);
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {

            }

            @Override
            public void onKeyboardClose() {

            }
        });


        loadMessages();

        current = FirebaseAuth.getInstance().getCurrentUser();
        onlineDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(current.getUid()).child("Online");

        // for checking deleted sms

        DatabaseReference current = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrentUserId).child(mChatUser);
        current.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (final DataSnapshot data : dataSnapshot.getChildren()) {

                    final int position = Integer.parseInt(data.child("position").getValue().toString());

                    if (data.child("isDelete").getValue().toString().equals("true") && position != -1 ) {

                        String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser + "/" + data.getKey().toString();
                        Map delete = new HashMap();
                        delete.put(current_user_ref, null);


                        //   Log.d("redwan", String.valueOf(position));

                        mRootRef.updateChildren(delete, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                messagesList.remove(position);
                                mAdapter.notifyItemRemoved(position);

                            }
                        });

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        databaseReference.child("users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("Online").getValue().toString();
                final String image = dataSnapshot.child("Thumb_image").getValue().toString();

                if(online.equals("true")) {
                    chatOnlineStatus.setText("Online");
                }
                else {
                    GetTimeAgo timeAgo = new GetTimeAgo();
                    long time = Long.parseLong(online);
                    String lastSeenTime = timeAgo.getTimeAgo(time, getApplicationContext());
                    chatOnlineStatus.setText(lastSeenTime);
                }

                chatProfilePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getApplication(), FullScreenImageView.class);
                        intent.setType(image);
                        startActivity(intent);
                    }
                });

                Picasso.with(ChatActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.avatar_default).into(chatProfilePic, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError() {
                        Picasso.with(ChatActivity.this).load(image).placeholder(R.drawable.avatar_default).into(chatProfilePic);
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRootRef.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {

                String message1 = emojiconEditText.getText().toString();

                if(dataSnapshot.hasChild(mChatUser)) {

                    mRootRef.child("Chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);

//                    Map chatAddMap = new HashMap();
//                    chatAddMap.put("seen", false);
//                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);
//
//                    Map chatUserMap = new HashMap();
//                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
//                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUserId, chatAddMap);
//
//                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                            if (databaseError != null) {
//
//                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
//
//                            }
//
//                        }
//                    });

//                    Map chatAddMap = new HashMap();
//                    chatAddMap.put("seen", true);
//                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);
//
//                    Map chatAddMap1 = new HashMap();
//                    chatAddMap1.put("seen", false);
//                    chatAddMap1.put("timestamp", ServerValue.TIMESTAMP);
//
//                    Map chatUserMap = new HashMap();
//                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
//                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUserId, chatAddMap1);
//
//
//                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                            if (databaseError != null) {
//
//                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
//
//                            }
//
//                        }
//                    });

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                Intent galleryIntent = new Intent();
//                galleryIntent.setType("image/*");
//                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//
//                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PIC);

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(ChatActivity.this);

            }
        });

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String sms_id, sms, fromUser, smsType;
        int position = -1;
        try {
            position = mAdapter.getPosition();
            sms_id = mAdapter.getSms_id();
            sms = mAdapter.getSms();
            fromUser = mAdapter.getFromUser();
            smsType = mAdapter.getSmsType();
        } catch (Exception e) {
            return super.onContextItemSelected(item);
        }

        if(item.getItemId() == 0) {
            if(!smsType.equals("image")) {
                if (position != -1) {
                    ClipboardManager clipboard = (ClipboardManager) ChatActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                    String text = sms;
                    ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ChatActivity.this, "Copy text", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(ChatActivity.this, "You can not copy image", Toast.LENGTH_SHORT).show();
            }
        }

        else if(item.getItemId() == 1) {

            if(smsType.equals("image")) {

                Intent intent = new Intent(this, FullScreenImageView.class);
                intent.setType(sms);
                startActivity(intent);
            }
        }
        else if(item.getItemId() == 2) {
            //delete message

            if(fromUser.equals(mCurrentUserId)){
                deleteMessage(sms, sms_id, position);
            } else {
                Toast.makeText(ChatActivity.this, "You can not delete your friends sms", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onContextItemSelected(item);
    }

// delete sms from me (one side)..

//    public void deleteMessage(String sms_id, final int position) {
//        //    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(mCurrentUserId).child(mChatUser);
//        String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser + "/" + sms_id;
//        Map delete = new HashMap();
//        delete.put(current_user_ref, null);
//        mRootRef.updateChildren(delete, new DatabaseReference.CompletionListener() {
//            @Override
//            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                Toast.makeText(ChatActivity.this, String.valueOf(position) + " Delete text", Toast.LENGTH_SHORT).show();
//                messagesList.remove(position);
//                mAdapter.notifyItemRemoved(position);
//
//            }
//        });
//    }


// delete sms from both side..

    public void deleteMessage(String sms, String sms_id, final int position) {

    //    mChatMessageView.getEditText().setText(sms);

        String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser + "/" + sms_id;

        DatabaseReference current = FirebaseDatabase.getInstance().getReference().child("messages").child(mChatUser).child(mCurrentUserId).child(sms_id);
        current.child("isDelete").setValue("true");
        current.child("position").setValue(position);
        Map delete = new HashMap();
        delete.put(current_user_ref, null);

        mRootRef.updateChildren(delete, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                messagesList.remove(position);
                mAdapter.notifyItemRemoved(position);

                Toast.makeText(ChatActivity.this, " Sms has been deleted from both side", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if(requestCode == GALLERY_PIC && resultCode == RESULT_OK){
//            Uri imageUri = data.getData();
//            CropImage.activity(imageUri)
//                    .setAspectRatio(1, 1)       // setAspectRatio(1, 1) aita hosse pic k square kore crop korar jonno.
//                    //         .setMinCropWindowSize(200, 200)       // ai line dile code crash kore
//                    .start(this);
//        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

//                mProgressBar.setTitle("Sending Image");
//                mProgressBar.setMessage("Please wait while we sending the image. when will be upload will will get Toast...");
//                mProgressBar.setCanceledOnTouchOutside(false);
//                mProgressBar.show();
                progressBar.setVisibility(View.VISIBLE);

                Uri imageUri = result.getUri();

                final String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser;
                final String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUserId;

                DatabaseReference user_message_push = mRootRef.child("messages")
                        .child(mCurrentUserId).child(mChatUser).push();

                final String push_id = user_message_push.getKey();

                File thumb_filePath = new File(imageUri.getPath());
                Bitmap thumb_bitmap = new Compressor(this)
                        .setMaxWidth(1000)
                        .setMaxHeight(1000)
                        .setQuality(1000)
                        .compressToBitmap(thumb_filePath);

                //for uploading thumb image to database

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] thumb_byte = baos.toByteArray();


                StorageReference filepath = mImageStorage.child("message_images").child(push_id + ".jpg");
                final StorageReference thumb_store = mImageStorage.child("message_images").child("Thumb").child(push_id + ".jpg");

                filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if (task.isSuccessful()) {

                            UploadTask uploadTask = thumb_store.putBytes(thumb_byte);

                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {

                                String download_url = thumb_task.getResult().getDownloadUrl().toString();

                                if (thumb_task.isSuccessful()) {

                                    String date = DateFormat.getDateTimeInstance().format(new Date());

                                    Map messageMap = new HashMap();
                                    messageMap.put("message", download_url);
                                    messageMap.put("seen", false);
                                    messageMap.put("type", "image");
                                    messageMap.put("time", date);
                                    messageMap.put("from", mCurrentUserId);
                                    messageMap.put("sms_id", push_id);
                                    messageMap.put("isDelete", "false");
                                    messageMap.put("position", -1);

                                    Map messageUserMap = new HashMap();
                                    messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                                    messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                                    emojiconEditText.setText("");

                                    mRootRef.child("Chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);
                                    mRootRef.child("Chat").child(mCurrentUserId).child(mChatUser).child("timestamp").setValue(ServerValue.TIMESTAMP);

                                    mRootRef.child("Chat").child(mChatUser).child(mCurrentUserId).child("seen").setValue(false);
                                    mRootRef.child("Chat").child(mChatUser).child(mCurrentUserId).child("timestamp").setValue(ServerValue.TIMESTAMP);

                                    mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                            if (databaseError != null) {

                                            //    mProgressBar.dismiss();
                                                progressBar.setVisibility(View.INVISIBLE);
                                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_LONG).show();
                                                Log.d("CHAT_LOG", databaseError.getMessage().toString());

                                            } else {
                                            //    mProgressBar.dismiss();
                                                progressBar.setVisibility(View.INVISIBLE);
                                                Toast.makeText(ChatActivity.this, "Image is send", Toast.LENGTH_LONG).show();
                                            }

                                        }
                                    });

                                } else{

                                }
                                }
                            });
                        }
                        else{
                          //  mProgressBar.dismiss();
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void loadMessages() {
        mRootRef.child("messages").child(mCurrentUserId).child(mChatUser).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                messagesList.add(message);
                mAdapter.notifyDataSetChanged();

                recyclerViewChat.scrollToPosition(messagesList.size() - 1);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {


        String message = emojiconEditText.getText().toString();

        if(!TextUtils.isEmpty(message)){

            String date = DateFormat.getDateTimeInstance().format(new Date());

            String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser;
            String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mRootRef.child("messages")
                    .child(mCurrentUserId).child(mChatUser).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", date);
            messageMap.put("from", mCurrentUserId);
            messageMap.put("sms_id", push_id);
            messageMap.put("isDelete", "false");
            messageMap.put("position", -1);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

            emojiconEditText.setText("");

            mRootRef.child("Chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);
            mRootRef.child("Chat").child(mCurrentUserId).child(mChatUser).child("timestamp").setValue(ServerValue.TIMESTAMP);

            mRootRef.child("Chat").child(mChatUser).child(mCurrentUserId).child("seen").setValue(false);
            mRootRef.child("Chat").child(mChatUser).child(mCurrentUserId).child("timestamp").setValue(ServerValue.TIMESTAMP);

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                    if(databaseError != null){

                        Log.d("CHAT_LOG", databaseError.getMessage().toString());

                    }

                }
            });

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        super.onOptionsItemSelected(item);

        return true;
    }

    // online -------------------------

    @Override
    public void onResume() {
        super.onResume();
        onlineDatabase.setValue("true");
    }

    @Override
    public void onPause() {
        super.onPause();
        onlineDatabase.setValue(ServerValue.TIMESTAMP);
    }
}