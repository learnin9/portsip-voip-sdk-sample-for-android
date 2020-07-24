package com.portsip.sipsample.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import com.portsip.sipsample.util.Contact;

import java.util.List;

public class ContactAdapter extends BaseAdapter {
    private List<Contact> mContacts;
    private Context mContext;

    public ContactAdapter(Context context, List<Contact> contacts) {
        mContacts = contacts;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public Object getItem(int position) {
        if (mContacts.size() > position) {
            return mContacts.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        CheckedTextView contactview = (CheckedTextView) LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_single_choice, null);

        Contact contact = (Contact) getItem(i);
        contactview.setText(contact.sipAddr + "   " + contact.currentStatusToString());
        return contactview;
    }
}

