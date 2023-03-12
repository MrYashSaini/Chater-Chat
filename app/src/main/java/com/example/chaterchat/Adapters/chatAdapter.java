package com.example.chaterchat.Adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chaterchat.AES;
import com.example.chaterchat.Model.MessageModel;
import com.example.chaterchat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class chatAdapter extends RecyclerView.Adapter {
    ArrayList<MessageModel> messageModels;
    Context context;
    String recId;
    int SenderViewType = 1;
    int ReceiverViewType =2;

    public chatAdapter(ArrayList<MessageModel> messageModels, Context context) {
        this.messageModels = messageModels;
        this.context = context;
    }

    public chatAdapter(ArrayList<MessageModel> messageModels, Context context, String recId) {
        this.messageModels = messageModels;
        this.context = context;
        this.recId = recId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType== SenderViewType){
            View view = LayoutInflater.from(context).inflate(R.layout.sample_sender,parent,false);
            return new SenderViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.sample_reciever,parent,false);
            return new RecieverViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(messageModels.get(position).getuId().equals(FirebaseAuth.getInstance().getUid())){
            return SenderViewType;
        }
        else {
            return ReceiverViewType;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel messageModel = messageModels.get(position);
        String message =AES.decrypt(messageModel.getMessage());
        if(holder.getClass()== SenderViewHolder.class){
            if (Objects.equals(messageModel.getType(), "Img")){
                ((SenderViewHolder)holder).senderMsg.setText(message);
            }
            else {
                ((SenderViewHolder)holder).senderMsg.setText(message);
            }
            ((SenderViewHolder)holder).senderMsg.setOnClickListener(view -> {
                try {
                    if (Objects.equals(messageModel.getType(), "Img")){
                        downloadEnImg(message);
                    }
                }
                catch (Exception ignored){}

                ((SenderViewHolder) holder).senderMsg.setOnLongClickListener(view2 -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Delete")
                            .setMessage("Are you sure you want to delete this message ? ")
                            .setPositiveButton("Yes", (dialogInterface, i) -> {
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                String senderRoom = FirebaseAuth.getInstance().getUid()+recId;
                                database.getReference().child("Chats").child(senderRoom)
                                        .child(messageModel.getMessageId())
                                        .setValue(null);
                            }).setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                    return false;
                });


            });


        }
        else {
            if (Objects.equals(messageModel.getType(), "Img")){
                ((RecieverViewHolder)holder).recieverMessage.setText(message);
            }
            else {
                ((RecieverViewHolder)holder).recieverMessage.setText(message);
            }
            ((RecieverViewHolder)holder).recieverMessage.setOnClickListener(view -> {
                try {
                    if (Objects.equals(messageModel.getType(), "Img")){
                        downloadEnImg(message);
                    }
                }catch (Exception ignored){}
            });

            ((RecieverViewHolder) holder).recieverMessage.setOnLongClickListener(view -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this message ? ")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            String senderRoom = FirebaseAuth.getInstance().getUid()+recId;
                            database.getReference().child("Chats").child(senderRoom)
                                    .child(messageModel.getMessageId())
                                    .setValue(null);
                        }).setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                return false;
            });

        }

    }

    private void downloadEnImg(String imgId) throws IOException {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child("customer image");
        final File localFile = File.createTempFile("image", "jpg");
        imageRef.child(imgId).getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            // Image downloaded successfully
            try {
                FileInputStream fis = new FileInputStream(localFile);
                byte[] encryptedData = new byte[(int) localFile.length()];
                fis.read(encryptedData);
                fis.close();
                byte[] key = "secretkey1234567".getBytes();
                byte[] decryptImg =  decrypt(encryptedData,key);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decryptImg, 0, decryptImg.length);
                Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.img_dialogbox);
                dialog.show();
                ImageView imageView = dialog.findViewById(R.id.ivImgDialogBoxImg);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }).addOnFailureListener(exception -> {
            // Image download failed
        });

    }
    public static byte[] decrypt(byte[] ciphertext, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        byte[] iv = new byte[16];
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return cipher.doFinal(ciphertext);
    }

    @Override
    public int getItemCount() {
        return messageModels.size();
    }

    public static class RecieverViewHolder extends RecyclerView.ViewHolder {
        TextView recieverMessage,recieverTime;
        public RecieverViewHolder(@NonNull View itemView) {
            super(itemView);
            recieverMessage = itemView.findViewById(R.id.recieverMessage);
            recieverTime = itemView.findViewById(R.id.recieverTime);
        }
    }
    public static class SenderViewHolder extends RecyclerView.ViewHolder {
        TextView senderMsg,senderTime;
        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMsg = itemView.findViewById(R.id.senderText);
            senderTime = itemView.findViewById(R.id.senderTime);
        }
    }
}