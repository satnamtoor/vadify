package com.android.vadify.ui.dashboard.fragment.call.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.vadify.R;
import com.android.vadify.data.api.models.ContactSyncingResponse;
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by dongjunkun on 2015/7/4.
 */
public class DirectCallAdapter extends BaseAdapter implements StickyListHeadersAdapter {


    CallListener callListener;
    private List<ContactSyncingResponse.Data.User> cities;
    String activityName;
    HashSet<ContactSyncingResponse.UserGroup> mStringHashSet = new HashSet<>();

    ContactSyncingResponse.UserGroup mUser;
    List<String> alreadySelected;
    public DirectCallAdapter(List<ContactSyncingResponse.Data.User> response, CallListener callListener, String activityName, List<String> alreadySelected ) {
        this.callListener = callListener;
        cities = response;
        this.activityName = activityName;
        this.alreadySelected = alreadySelected;

    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        if (cities.size() == 0) {
            return 1;
        }
        return cities.size();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeadViewHolder viewHolder;
        if (convertView != null) {
            viewHolder = (HeadViewHolder) convertView.getTag();
        } else {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_layout, null);
            viewHolder = new HeadViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        if (!cities.get(position).getName().equalsIgnoreCase("")) {
            viewHolder.mHeadText.setText("" + cities.get(position).getName().charAt(0));
        }
        return convertView;
    }

    /**
     * 更新列表
     *
     * @param cities
     */
    public void updateList(List<ContactSyncingResponse.Data.User> cities) {
        this.cities = cities;
        notifyDataSetChanged();
    }

    @Override
    public long getHeaderId(int i) {
        return cities.get(i).getName().charAt(0);
    }

    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public Object getItem(int position) {
        return cities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView != null) {
            viewHolder = (ViewHolder) convertView.getTag();
        } else {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        String nameAlphabet = cities.get(position).getName().replaceAll("^\\s*([a-zA-Z]).*\\s+([a-zA-Z])\\S+$", "$1$2");
        viewHolder.nameLabel.setText(nameAlphabet.toUpperCase());

        viewHolder.name.setText(cities.get(position).getName());
        viewHolder.phoneNumber.setText(cities.get(position).getNumber());

        if (activityName.equalsIgnoreCase("")) {
            viewHolder.messageNotificationSwitch.setVisibility(View.GONE);
            viewHolder.audioCall.setVisibility(View.VISIBLE);
            viewHolder.videoCall.setVisibility(View.VISIBLE);
        } else {
            viewHolder.messageNotificationSwitch.setVisibility(View.VISIBLE);
            viewHolder.audioCall.setVisibility(View.GONE);
            viewHolder.videoCall.setVisibility(View.GONE);
        }

        viewHolder.messageNotificationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                        mUser = new ContactSyncingResponse.UserGroup(cities.get(position).get_id(),
                                cities.get(position).getName()
                                , cities.get(position).getNumber()
                                , cities.get(position).getProfileImage());
                    if (isChecked) {
                        mStringHashSet.add(mUser);
                    } else {
                        mStringHashSet.remove(mUser);
                    }
                    DirectCallActivity.Companion.setMemberList(new ArrayList<>(mStringHashSet));
                });
        viewHolder.audioCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callListener.audioListener(cities.get(position));
            }
        });

        viewHolder.videoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callListener.videoListener(cities.get(position));
            }
        });

        if (alreadySelected.contains(cities.get(position).get_id())){
            viewHolder.messageNotificationSwitch.setChecked(true);
        }
        Glide.with(convertView.getContext()).load(cities.get(position).getProfileImage()).into(viewHolder.circularImageView);
        return convertView;
    }

    public List<ContactSyncingResponse.Data.User> getContactList() {
        return cities;
    }

    //根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
    public int getPositionForSection(int section) {
        for (int i = 0; i < getCount(); i++) {
            String sortStr = cities.get(i).getName();
            char firstChar = sortStr.toUpperCase().charAt(0);
            if (firstChar == section) {
                return i;
            }
        }
        return -1;
    }


    public interface CallListener {
        void audioListener(ContactSyncingResponse.Data.User user);

        void videoListener(ContactSyncingResponse.Data.User user);
    }

    static class ViewHolder {
        MaterialTextView name, phoneNumber, nameLabel;
        MaterialCheckBox messageNotificationSwitch;
        CircleImageView circularImageView;
        AppCompatImageView videoCall, audioCall;
        ConstraintLayout mConstraintLayout;

        ViewHolder(View view) {
            name = (MaterialTextView) view.findViewById(R.id.name);
            phoneNumber = (MaterialTextView) view.findViewById(R.id.number);
            nameLabel = (MaterialTextView) view.findViewById(R.id.nameLabel);
            circularImageView = (CircleImageView) view.findViewById(R.id.circularImageView);
            messageNotificationSwitch = (MaterialCheckBox) view.findViewById(R.id.messageNotificationSwitch);
            videoCall = (AppCompatImageView) view.findViewById(R.id.videoCall);
            audioCall = (AppCompatImageView) view.findViewById(R.id.audioCall);

        }
    }

    static class HeadViewHolder {
        TextView mHeadText;

        HeadViewHolder(View view) {
            mHeadText = (TextView) view.findViewById(R.id.headText);
        }
    }
}
