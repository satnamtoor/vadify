package com.android.vadify.ui.contact.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.vadify.R;
import com.android.vadify.data.db.contact.Contact;
import com.bumptech.glide.Glide;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by dongjunkun on 2015/7/4.
 */
public class ContactAdapter extends BaseAdapter implements StickyListHeadersAdapter {


    private List<Contact> cities;

    public ContactAdapter(List<Contact> cities) {
        this.cities = cities;
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
        viewHolder.mHeadText.setText("" + cities.get(position).getSortLetter().charAt(0));
        return convertView;
    }

    /**
     * 更新列表
     *
     * @param cities
     */
    public void updateList(List<Contact> cities) {
        this.cities = cities;
        notifyDataSetChanged();
    }

    @Override
    public long getHeaderId(int i) {
        return cities.get(i).getSortLetter().charAt(0);
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
        viewHolder.phoneNumber.setText(cities.get(position).getPhone());
        viewHolder.messageNotificationSwitch.setChecked(cities.get(position).getSelection());
        Glide.with(convertView.getContext()).load(cities.get(position).getImageUri()).into(viewHolder.circularImageView);

        viewHolder.messageNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                cities.get(position).setSelection(b);
            }
        });
        return convertView;
    }


    public List<Contact> getContactList() {
        return cities;
    }


    static class ViewHolder {
        MaterialTextView name, phoneNumber, nameLabel;
        MaterialCheckBox messageNotificationSwitch;
        CircleImageView circularImageView;

        ViewHolder(View view) {
            name = (MaterialTextView) view.findViewById(R.id.name);
            phoneNumber = (MaterialTextView) view.findViewById(R.id.number);
            nameLabel = (MaterialTextView) view.findViewById(R.id.nameLabel);
            circularImageView = (CircleImageView) view.findViewById(R.id.circularImageView);
            messageNotificationSwitch = (MaterialCheckBox) view.findViewById(R.id.messageNotificationSwitch);
        }
    }

    static class HeadViewHolder {
        TextView mHeadText;

        HeadViewHolder(View view) {
            mHeadText = (TextView) view.findViewById(R.id.headText);
        }
    }

    //根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
    public int getPositionForSection(int section) {
        for (int i = 0; i < getCount(); i++) {
            String sortStr = cities.get(i).getSortLetter();
            char firstChar = sortStr.toUpperCase().charAt(0);
            if (firstChar == section) {
                return i;
            }
        }
        return -1;
    }
}
