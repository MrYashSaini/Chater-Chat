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
import com.example.chaterchat.databinding.ActivityGroupChatBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class GroupChatActivity extends AppCompatActivity {
    ActivityGroupChatBinding binding;
    FirebaseAuth auth;
    Toolbar toolbar;
    int imgId;
    String photoUri;
    FirebaseDatabase database;
    FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        toolbar = findViewById(R.id.group_toolbar);
        auth = FirebaseAuth.getInstance();
        setSupportActionBar(toolbar);

        ProgressDialog dialog = new ProgressDialog(GroupChatActivity.this);
        dialog.setTitle("Load Message");
        dialog.setMessage("Loading...");
        dialog.show();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        final ArrayList<MessageModel> messageModels = new ArrayList<>();
        final String senderId = FirebaseAuth.getInstance().getUid();
        binding.userNameTv.setText("Friends Group");
        final chatAdapter adapter = new chatAdapter(messageModels,this);
        binding.charRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.charRecyclerView.setLayoutManager(layoutManager);
        StorageReference imageRef = storage.getReference().child("customer image");

//        set back arrow in toolbar
        binding.backArrow.setOnClickListener(view -> {
            Intent intent = new Intent(GroupChatActivity.this,MainActivity.class);
            startActivity(intent);
        });

//        show chat message on screen
        database.getReference().child("Group Chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageModels.clear();
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    MessageModel model = dataSnapshot.getValue(MessageModel.class);
                    messageModels.add(model);
                }
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

//        select image from device
        binding.ivGroupChatFileUpload.setOnClickListener(v -> ImagePicker.Companion.with(GroupChatActivity.this)
                .crop()
                .cropSquare()
                .start(4));

//        send message and image
        binding.sendbtn.setOnClickListener(view -> {
            ProgressDialog dialog2 = new ProgressDialog(GroupChatActivity.this);
            dialog2.setTitle("Send Message");
            dialog2.setMessage("Wait...");
            dialog2.show();
            final String message = AES.encrypt(binding.messagebox.getText().toString());
            final MessageModel model = new MessageModel(senderId,message);
            model.setTimestamp(new Date().getTime());
            model.setType("Message");
            binding.messagebox.setText("");
            if(photoUri !=null){
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
                    imageRef.child("Img"+imgId).putBytes(encryptedData).addOnSuccessListener(taskSnapshot ->
                            imageRef.child("Img"+imgId).getDownloadUrl().addOnSuccessListener(uri -> {
                        photoUri = uri.toString();
                        String modelMsg = AES.encrypt("Img"+imgId);
                        model.setMessage(modelMsg);
                        database.getReference().child("Group Chat")
                                .push()
                                .setValue(model).addOnSuccessListener(unused -> {
                                    imgId++;
                                    database.getReference().child("imgId").setValue(imgId);
                                    photoUri = null;
                                    dialog2.dismiss();
                                });
                    }));
                }
                catch (NoSuchPaddingException | NoSuchAlgorithmException |
                       InvalidAlgorithmParameterException | InvalidKeyException |
                       IllegalBlockSizeException | BadPaddingException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                database.getReference().child("Group Chat")
                        .push()
                        .setValue(model).addOnSuccessListener(unused -> {
                            dialog2.dismiss();
                        });
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.Setting:
                Intent intent0 = new Intent(GroupChatActivity.this,SettingActivity.class);
                startActivity(intent0);
                break;
            case R.id.logout:
                auth.signOut();
                Intent intent = new Intent(GroupChatActivity.this, SignInActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==4){
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