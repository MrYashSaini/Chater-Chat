package com.example.chaterchat;

import static com.example.chaterchat.AESImage.encrypt2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chaterchat.Adapters.chatAdapter;
import com.example.chaterchat.Model.MessageModel;
import com.example.chaterchat.account.SignInActivity;
import com.example.chaterchat.databinding.ActivityChatBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ChatDetailActivity extends AppCompatActivity {
    ActivityChatBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    Toolbar toolbar;
    String photoUri;
    FirebaseStorage storage;
    int imgId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        final String senderId = auth.getUid();
        String recieverId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");
        String profilePic = getIntent().getStringExtra("profilePic");
        StorageReference imageRef = storage.getReference().child("customer image");

//        toolbar
        binding.userNameTv.setText(userName);
        Picasso.get().load(profilePic).placeholder(R.drawable.man).into(binding.profileImage);
        binding.backArrow.setOnClickListener(view -> {
            Intent intent = new Intent(ChatDetailActivity.this,MainActivity.class);
            startActivity(intent);
        });

//        show chat on screen
        final ArrayList<MessageModel> messageModels= new ArrayList<>();
        final chatAdapter chatAdapter = new chatAdapter(messageModels,this,recieverId);
        binding.charRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.charRecyclerView.setLayoutManager(linearLayoutManager);

        final String senderRoom = senderId+recieverId;
        final String receiverRoom = recieverId+senderId;
        database.getReference().child("Chats").child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageModels.clear();
                        for(DataSnapshot snapshot1 : snapshot.getChildren()){
                            MessageModel model = snapshot1.getValue(MessageModel.class);
                            Objects.requireNonNull(model).setMessageId(snapshot1.getKey());
                            messageModels.add(model);
                        }
                        chatAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

//        select Image from device
        binding.ivChatFileUpload.setOnClickListener(v -> ImagePicker.Companion.with(ChatDetailActivity.this)
                .crop()
                .cropSquare()
                .start(3));

//        send message
        binding.sendbtn.setOnClickListener(view -> {
            ProgressDialog dialog = new ProgressDialog(ChatDetailActivity.this);
            dialog.setTitle("Send");
            dialog.setMessage("send message please wait....");
            dialog.show();
            String message = binding.messagebox.getText().toString();
            String msg = AES.encrypt(message);
            final MessageModel model = new MessageModel(senderId,msg);
            model.setTimestamp(new Date().getTime());
            model.setType("Message");
            binding.messagebox.setText("");
            if(photoUri!=null){
                model.setType("Img");
//                Image encryption using AES Algorithm  and upload
                try {
//                    get image and change in byte array
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(photoUri));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] bitmapData = stream.toByteArray();
//                    key
                    byte[] key = "secretkey1234567".getBytes();

//                    get encrypted image and upload
                    byte[] encryptedData;
                    encryptedData = encrypt2(bitmapData, key);
                    imageRef.child("Img"+imgId).putBytes(encryptedData).addOnSuccessListener(taskSnapshot -> imageRef.child("Img"+imgId).getDownloadUrl().addOnSuccessListener(uri -> {
                        photoUri = uri.toString();
                        String modelMsg = AES.encrypt("Img"+imgId);
                        model.setMessage(modelMsg);
                        database.getReference().child("Chats").child(senderRoom)
                                .push()
                                .setValue(model).addOnSuccessListener(unused -> database.getReference().child("Chats").child(receiverRoom).push()
                                        .setValue(model).addOnSuccessListener(unused1 -> {
                                            imgId++;
                                            database.getReference().child("imgId").setValue(imgId);
                                            photoUri = null;
                                            dialog.dismiss();
                                        }));
                    }));
                }
                catch (NoSuchPaddingException | NoSuchAlgorithmException |
                         InvalidAlgorithmParameterException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException | IOException e) {
                    throw new RuntimeException(e);
                }

            }
            else {
                database.getReference().child("Chats").child(senderRoom)
                        .push()
                        .setValue(model).addOnSuccessListener(unused -> database.getReference().child("Chats").child(receiverRoom).push()
                                .setValue(model).addOnSuccessListener(unused12 -> {
                                    dialog.dismiss();
                                }));

            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.Setting:
                Intent intent0 = new Intent(ChatDetailActivity.this,SettingActivity.class);
                startActivity(intent0);
                break;
            case R.id.logout:
                auth.signOut();
                Intent intent = new Intent(ChatDetailActivity.this, SignInActivity.class);
                startActivity(intent);
                break;
            case R.id.groupChat:
                Intent intent1 = new Intent(this,GroupChatActivity.class);
                startActivity(intent1);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==3){
            assert data != null;
            Uri uri = data.getData();
            photoUri = uri.toString();
            binding.messagebox.setText(photoUri);
            database.getReference().child("imgId").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    imgId = snapshot.getValue(int.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
}