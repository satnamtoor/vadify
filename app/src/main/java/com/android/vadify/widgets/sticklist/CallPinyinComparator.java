package com.android.vadify.widgets.sticklist;


import com.android.vadify.data.api.models.ContactSyncingResponse;

import java.util.Comparator;

/**
 * Created by dongjunkun on 2015/7/4.
 * <p>
 * 根据拼音来排列HeadListView中的数据
 */
public class CallPinyinComparator implements Comparator<ContactSyncingResponse.Data.User> {
    @Override
    public int compare(ContactSyncingResponse.Data.User c1, ContactSyncingResponse.Data.User c2) {
        if (c1.getName().equals("@") || c2.getName().equals("#")) {
            return -1;
        } else if (c1.getName().equals("#") || c2.getName().equals("@")) {
            return 1;
        } else {
            return c1.getName().compareTo(c2.getName());
        }
    }
}
