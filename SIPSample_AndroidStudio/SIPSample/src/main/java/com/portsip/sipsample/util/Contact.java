package com.portsip.sipsample.util;

public class Contact
{
    public String subRequestDescription;
    public enum SUBSCRIBE_STATE_FLAG
    {
        UNSETTLLED,
        ACCEPTED ,
        REJECTED,
        UNSUBSCRIBE,
    }
    public String sipAddr;
    public String subDescription;
    public boolean subScribRemote;

    public long subId;//if SubId >0 means received remote subscribe
    public SUBSCRIBE_STATE_FLAG state; // weigher accepte remote subscribe

    public String currentStatusToString()
    {
        String status = "";

        status += "Subscribe："+ subScribRemote;
        status += "  Remote presence is：" + subDescription;


        status += " Subscription received:("+ subRequestDescription+")";
        switch (state)
        {
            case ACCEPTED:
                status += "Accepted";
                break;
            case REJECTED:
                status += "Rejected";
                break;
            case UNSETTLLED:
                status += "Pending";
                break;
            case UNSUBSCRIBE:
                status += "Not subscripted";
                break;
        }

        return status;
    }
    public Contact()
    {
        state = SUBSCRIBE_STATE_FLAG.UNSUBSCRIBE;//Not being subscripted
        subScribRemote = false;//Not subscripted
        subId = 0;
    }

}


