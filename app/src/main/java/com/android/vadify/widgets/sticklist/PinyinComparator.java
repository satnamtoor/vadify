package com.android.vadify.widgets.sticklist;


import com.android.vadify.data.db.contact.Contact;
import com.android.vadify.ui.dashboard.Dashboard;

import java.util.Comparator;

/**
 * Created by dongjunkun on 2015/7/4.
 * <p>
 * 根据拼音来排列HeadListView中的数据
 */
public class PinyinComparator implements Comparator<Contact> {
    @Override
    public int compare(Contact c1, Contact c2) {

        if (c1.getSortLetter().equals("@") || c2.getSortLetter().equals("#")) {
            return -1;
        } else if (c1.getSortLetter().equals("#") || c2.getSortLetter().equals("@")) {
            return 1;
        } else {
            return c1.getSortLetter().compareTo(c2.getSortLetter());
        }
    }
}
