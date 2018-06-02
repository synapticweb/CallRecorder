package net.synapticweb.callrecorder;

import android.net.Uri;
import android.support.annotation.NonNull;

class PhoneNumber implements Comparable<PhoneNumber> {
    private String phoneNumber;
    private String phoneType;
    private String contactName;
    private Uri photoUri;
    private boolean unknownPhone = false;

    public int compareTo(@NonNull  PhoneNumber numberToCompare)
    {
        if(this.isUnknownPhone() && !numberToCompare.isUnknownPhone() )
            return -1;
        if(!this.isUnknownPhone() && numberToCompare.isUnknownPhone())
            return 1;

        return this.contactName.compareTo(numberToCompare.getContactName());
    }


    public boolean isUnknownPhone() {
        return unknownPhone;
    }

    public void setUnknownPhone(boolean unknownPhone) {
        this.unknownPhone = unknownPhone;
    }

    public String getPhoneNumber() {

        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String phoneType) {
        this.phoneType = phoneType;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }
}
