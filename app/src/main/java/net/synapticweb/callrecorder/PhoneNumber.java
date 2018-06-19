package net.synapticweb.callrecorder;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;



class PhoneNumber implements Comparable<PhoneNumber> {
    private String phoneNumber;
    private String phoneType;
    private int phoneTypeCode;
    private String contactName;
    private Uri photoUri;
    private boolean unkownNumber = false;
    private boolean privateNumber = false;

    static public PhoneNumber searchNumberInContacts(String number, Context context)
    {
        Uri phoneLookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor cursor = context.getContentResolver().
                query(phoneLookupUri,
                        new String[]{ContactsContract.PhoneLookup.TYPE,
                                ContactsContract.PhoneLookup.DISPLAY_NAME,
                                ContactsContract.PhoneLookup.PHOTO_URI},
                        null, null, null);

        if(cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                PhoneNumber phoneNumber = new PhoneNumber();
                phoneNumber.setPhoneType(cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)));
                phoneNumber.setContactName(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)));
                phoneNumber.setPhotoUri(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)));
                phoneNumber.setPhoneNumber(number);

                return phoneNumber;
            }
            else
                return null;
        }
            return null;
    }

    public int getPhoneTypeCode() {
        return phoneTypeCode;
    }

    public boolean isUnkownNumber() {
        return unkownNumber;
    }

    public void setUnkownNumber(boolean unkownNumber) {
        this.unkownNumber = unkownNumber;
    }

    public boolean isPrivateNumber() {
        return privateNumber;
    }

    public void setPrivateNumber(boolean privateNumber) {
        this.privateNumber = privateNumber;
    }


    public int compareTo(@NonNull  PhoneNumber numberToCompare)
    {
        if(this.isUnkownNumber() && !numberToCompare.isUnkownNumber() )
            return -1;
        if(!this.isUnkownNumber() && numberToCompare.isUnkownNumber())
            return 1;

        return this.contactName.compareTo(numberToCompare.getContactName());
    }


    public String getPhoneNumber() {

        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if(phoneNumber == null)
            this.phoneNumber = "Unknown number";
        else
            this.phoneNumber = phoneNumber;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String phoneType)
    {
        this.phoneType = phoneType;
    }

    public void setPhoneType(int phoneTypeCode) {
        this.phoneTypeCode = phoneTypeCode;
        switch (phoneTypeCode) {
            case 1:
                this.phoneType = "Home";
                break;
            case 2:
                this.phoneType = "Mobile";
                break;
            case -1:
                this.phoneType = "Unknown";
                break;
            default:
                this.phoneType = "Other";
        }

    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        if(contactName == null) {
            if(isPrivateNumber())
                this.contactName = "Private number";
            else
                this.contactName = "Unknown contact";
        }
        else
            this.contactName = contactName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUriStr) {
        if(photoUriStr != null)
            this.photoUri = Uri.parse(photoUriStr);
        else
            this.photoUri = null;
    }
}
