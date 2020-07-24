package com.portsip.sipsample.ui;

import com.portsip.PortSipSdk;
import com.portsip.R;
import com.portsip.sipsample.adapter.ContactAdapter;
import com.portsip.sipsample.receiver.PortMessageReceiver;
import com.portsip.sipsample.service.PortSipService;
import com.portsip.sipsample.util.CallManager;
import com.portsip.sipsample.util.Contact;
import com.portsip.sipsample.util.ContactManager;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

public class MessageFragment extends BaseFragment implements View.OnClickListener ,PortMessageReceiver.BroadcastListener{
    EditText etContact, etStatus, etmsgdest, etMessage;
    ListView lvContacts;

    MyApplication application;
    MainActivity activity;
    private ContactAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        activity = (MainActivity) getActivity();
        application = (MyApplication) activity.getApplication();

        return inflater.inflate(R.layout.message, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvContacts = (ListView) view.findViewById(R.id.lvcontacs);
        view.findViewById(R.id.btsubscribe).setOnClickListener(this);
        view.findViewById(R.id.btclear).setOnClickListener(this);

        mAdapter = new ContactAdapter(getActivity(), ContactManager.Instance().getContacts());

        lvContacts.setAdapter( mAdapter);

        etStatus = (EditText) view.findViewById(R.id.etstatus);
        etMessage = (EditText) view.findViewById(R.id.etmessage);
        etContact = (EditText) view.findViewById(R.id.etcontact);
        etmsgdest = (EditText) view.findViewById(R.id.etmsgdest);

        view.findViewById(R.id.btsendmsg).setOnClickListener(this);
        view.findViewById(R.id.btsendstatus).setOnClickListener(this);
        view.findViewById(R.id.btaddcontact).setOnClickListener(this);

        view.findViewById(R.id.btaccept).setOnClickListener(this);
        view.findViewById(R.id.btrefuse).setOnClickListener(this);
        view.findViewById(R.id.btsubscribe).setOnClickListener(this);
        onHiddenChanged(false);

    }

    private void btnAddContact_Click(PortSipSdk sdk) {
        if (!isOnline())
            return;
        String sendTo = etContact.getText().toString();
        if (TextUtils.isEmpty(sendTo)) {
            return;
        }

        Contact contact = ContactManager.Instance().findContactBySipAddr(sendTo);
        if (contact == null) {

            contact = new Contact();
            contact.sipAddr = sendTo;
            ContactManager.Instance().addContact(contact);
        }
        updateLV();
    }

    private void btnSubscribeContact_Click(PortSipSdk sdk) {
        if (!isOnline())
            return;
        Contact contact = getSelectContact();
        if (contact != null) {
            sdk.presenceSubscribe(contact.sipAddr, "hello");//subscribe remote
            contact.subScribRemote = true;
        }
        updateLV();
    }

    private void btnClearContact_Click() {
        ContactManager.Instance().removeAll();
        updateLV();
    }

    private void updateLV() {
        mAdapter.notifyDataSetChanged();
    }

    private void btnSetStatus_Click(PortSipSdk sdk) {
        if (!isOnline())
            return;

        String content = etStatus.getText().toString();
        if (TextUtils.isEmpty(content)) {
            //showTips("please input status description string");
            return;
        }
        List<Contact> contacts = ContactManager.Instance().getContacts();
        for (Contact contact : contacts) {
            long subscribeId = contact.subId;

            String statusText = etStatus.getText().toString();
            if (contact.state == Contact.SUBSCRIBE_STATE_FLAG.ACCEPTED)//向已经接受的订阅，发布自己的出席状态
            {
                sdk.setPresenceStatus(subscribeId, statusText);
            }
        }

    }

    private void btnAcceptSubscribe_Click(PortSipSdk sdk) {
        if (!isOnline())
            return;
        Contact contact = getSelectContact();
        if (contact != null && contact.state == Contact.SUBSCRIBE_STATE_FLAG.UNSETTLLED) {
            sdk.presenceAcceptSubscribe(contact.subId);//accept
            contact.state = Contact.SUBSCRIBE_STATE_FLAG.ACCEPTED;
            String status = etStatus.getText().toString();
            if (!TextUtils.isEmpty(status)) {
                status = "hello";
            }
            sdk.setPresenceStatus(contact.subId, status);//set my status

        }

        updateLV();
    }

    private void btnRefuseSubscribe_Click(PortSipSdk sdk) {
        if (!isOnline())
            return;
        Contact contact = getSelectContact();
        if (contact != null && contact.state == Contact.SUBSCRIBE_STATE_FLAG.UNSETTLLED) {

            sdk.presenceRejectSubscribe(contact.subId);//reject
            contact.state = Contact.SUBSCRIBE_STATE_FLAG.REJECTED;// reject subscribe
            contact.subId = 0;

        }
        updateLV();
    }

    private void btnSend_Click(PortSipSdk sdk) {

        if (!isOnline())
            return;
        String content = etMessage.getText().toString();
        String sendTo = etmsgdest.getText().toString();
        if (TextUtils.isEmpty(sendTo)) {
            Toast.makeText(getActivity(), "Please input send to target",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(getActivity(), "Please input message content",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] contentBinary = content.getBytes();
        if (contentBinary != null) {
            sdk.sendOutOfDialogMessage(sendTo, "text", "plain", false,
                    contentBinary, contentBinary.length);
        }
    }

    public void onBroadcastReceiver(Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (PortSipService.PRESENCE_CHANGE_ACTION.equals(action)) {
            updateLV();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            activity.receiver.broadcastReceiver = this;
            updateLV();
        }
    }

    private boolean isOnline() {
        if (!CallManager.Instance().regist) {
            Toast.makeText(getActivity(), "Please login at first", Toast.LENGTH_SHORT).show();
        }
        return CallManager.Instance().regist;
    }

    private Contact getSelectContact() {
        List<Contact> contacts = ContactManager.Instance().getContacts();
        int checkedItemPosition = lvContacts.getCheckedItemPosition();
        if (ListView.INVALID_POSITION != checkedItemPosition && contacts.size()> checkedItemPosition) {
            return contacts.get(checkedItemPosition);
        }
        return null;
    }

    @Override
    public void onClick(View view) {
        if (application.mEngine == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.btsendmsg:
                btnSend_Click(application.mEngine);
                break;
            case R.id.btsendstatus:
                btnSetStatus_Click(application.mEngine);
                break;
            case R.id.btaddcontact:
                btnAddContact_Click(application.mEngine);
                break;
            case R.id.btclear:
                btnClearContact_Click();
                break;
            case R.id.btsubscribe:
                btnSubscribeContact_Click(application.mEngine);
                break;
            case R.id.btaccept:
                btnAcceptSubscribe_Click(application.mEngine);
                break;
            case R.id.btrefuse:
                btnRefuseSubscribe_Click(application.mEngine);
                break;
            default:
                break;
        }
    }
}
