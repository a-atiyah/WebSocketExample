package com.a.atiyah.websocketexample.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.a.atiyah.websocketexample.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{
    private final static int MSG_SEND_CODE = 0;
    private final static int MSG_RECEIVED_CODE = 1;
    private final static int IMG_SEND_CODE = 2;
    private final static int IMG_RECEIVED_CODE = 3;

    Context mContext;
    private List<JSONObject> mMessages = new ArrayList<>();

    public MessageAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case MSG_SEND_CODE:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_msg_send, parent, false);
                return new MessageViewHolder(view);
            case MSG_RECEIVED_CODE:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_msg_recieved, parent, false);
                return new MessageViewHolder(view);
            case IMG_SEND_CODE:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_img_send, parent, false);
                return new MessageViewHolder(view);
            case IMG_RECEIVED_CODE:
                view = LayoutInflater.from(mContext).inflate(R.layout.item_img_recieved, parent, false);
                return new MessageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        JSONObject msg = mMessages.get(position);
        try {
            if (msg.getBoolean("isSent")) {
                if (msg.has("message"))
                    holder.mTVMessage.setText(msg.getString("message"));
                else {
                    Bitmap bitmap = stringToBitmap(msg.getString("image"));
                    holder.mIVImage.setImageBitmap(bitmap);
                }

            } else {
                if (msg.has("message")) {
                    holder.mTVMessage.setText(msg.getString("message"));
                    holder.mTVName.setText(msg.getString("username"));
                }
                else{
                    holder.mTVName.setText(msg.getString("username"));
                    Bitmap bitmap = stringToBitmap(msg.getString("image"));
                    holder.mIVImage.setImageBitmap(bitmap);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap stringToBitmap(String image) {
        byte[] bytes = Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        JSONObject msg = mMessages.get(position);
        try {
            if (msg.getBoolean("isSent")) {
                if (msg.has("message"))
                    return MSG_SEND_CODE;
                else
                    return IMG_SEND_CODE;
            } else {
                if (msg.has("message"))
                    return MSG_RECEIVED_CODE;
                else
                    return IMG_RECEIVED_CODE;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder{

        TextView mTVMessage, mTVName;
        ImageView mIVImage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            mTVMessage = itemView.findViewById(R.id.tv_msg_chat_item);
            mTVName = itemView.findViewById(R.id.tv_name_chat_item);
            mIVImage = itemView.findViewById(R.id.iv_img_chat_item);
        }
    }

    public void setItem(JSONObject obj) {
        mMessages.add(obj);
        notifyDataSetChanged();
    }
}
