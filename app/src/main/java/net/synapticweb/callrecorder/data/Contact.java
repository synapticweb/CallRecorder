/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import net.synapticweb.callrecorder.Util;

import java.util.List;
import java.util.Objects;


public class Contact implements Comparable<Contact>, Parcelable {
    private Long id = 0L;
    private String phoneNumber = "";
    private int phoneType = Util.UNKNOWN_TYPE_PHONE_CODE;
    private String contactName = "";
    private Uri photoUri = null;
    private boolean shouldRecord = true;
    private Integer color = null;

    public Contact(){
    }

    public Contact(Long id, String phoneNumber, String contactName, String photoUriStr, Integer phoneTypeCode) {
        if(id != null) setId(id);
        if(phoneNumber != null) setPhoneNumber(phoneNumber);
        if(contactName != null) setContactName(contactName);
        if(photoUriStr != null) setPhotoUri(photoUriStr);
        if(phoneTypeCode != null) setPhoneType(phoneTypeCode);
    }

    public static Contact queryNumberInAppContacts(Repository repository, String receivedPhoneNumber) {
        List<Contact> contacts = repository.getAllContacts();

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        for (Contact contact : contacts) {
            String dbNumPhone = contact.getPhoneNumber();
            PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(receivedPhoneNumber, dbNumPhone);
            if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER)
                return contact;
        }
        return null;
    }

    public void update(Repository repository) {
        repository.updateContact(this);
    }

    public void save(Repository repository) throws SQLException {
        repository.insertContact(this);
    }

    public void delete(Repository repository, Context context) throws SQLException {
        List<Recording> recordings = repository.getRecordings(this);
        for(Recording recording : recordings)
            recording.delete(repository);

        if(getPhotoUri() != null ) //întotdeauna este poza noastră.
            context.getContentResolver().delete(getPhotoUri(), null, null);
       repository.deleteContact(this);
    }

  @Nullable
  static public Contact queryNumberInPhoneContacts(final String number, @NonNull ContentResolver resolver) {
        //implementare probabil mai eficientă decît ce aveam eu:
      //https://stackoverflow.com/questions/3505865/android-check-phone-number-present-in-contact-list-phone-number-retrieve-fr
      Uri lookupUri = Uri.withAppendedPath(
              //e atît de lung pentru că am și eu ContactsContract.
              android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
              Uri.encode(number));
      String[] projection = {android.provider.ContactsContract.PhoneLookup.NUMBER,
              android.provider.ContactsContract.PhoneLookup.TYPE,
              android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME,
              android.provider.ContactsContract.PhoneLookup.PHOTO_URI };
      //Matchingul este asigurat de android, aî merg numere în diverse formaturi. Nu este nevoie de PhoneNumberUtil.
      Cursor cursor = resolver.query(lookupUri, projection, null, null, null);

          if(cursor != null && cursor.moveToFirst()) {
              Contact contact = new Contact();
              contact.setPhoneType(cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.TYPE)));
              contact.setContactName(cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)));
              contact.setPhoneNumber(cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.NUMBER)));
              contact.setPhotoUri(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)));
              cursor.close();
              return contact;
          }
      return null;
    }

    public boolean isPrivateNumber() {
        return phoneNumber == null;
    }

    public void setIsPrivateNumber() {
        this.phoneNumber = null;
    }


    public int compareTo(@NonNull Contact numberToCompare)
    {
        return this.contactName.compareTo(numberToCompare.getContactName());
    }


    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
    }

    public int getPhoneTypeCode() {
        return phoneType;
    }

    public String getPhoneTypeName(){
        for(Util.PhoneTypeContainer typeContainer : Util.PHONE_TYPES)
            if(typeContainer.getTypeCode() == this.phoneType)
                return typeContainer.getTypeName();
        return null;
    }

    public void setPhoneType(String phoneType)
    {
        for(Util.PhoneTypeContainer typeContainer : Util.PHONE_TYPES)
            if(typeContainer.getTypeName().equals(phoneType)) {
                this.phoneType = typeContainer.getTypeCode();
                break;
            }

    }

    public void setPhoneType(int phoneTypeCode) {
        for(Util.PhoneTypeContainer type : Util.PHONE_TYPES) {
            if (phoneTypeCode == type.getTypeCode()) {
                this.phoneType = phoneTypeCode;
                return;
            }
        }
        this.phoneType = Util.UNKNOWN_TYPE_PHONE_CODE;
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

    public void setPhotoUri(String photoUriStr) {
        if(photoUriStr != null)
            this.photoUri = Uri.parse(photoUriStr);
        else
            this.photoUri = null;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public Long getId() { //Trebuie Long ptr că id poate să fie null
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    /** Necesară pentru comparațiile din teste. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return phoneType == contact.phoneType &&
                shouldRecord == contact.shouldRecord &&
                Objects.equals(id, contact.id) &&
                phoneNumber.equals(contact.phoneNumber) &&
                contactName.equals(contact.contactName) &&
                Objects.equals(photoUri, contact.photoUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phoneNumber, contactName);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.phoneNumber);
        dest.writeInt(this.phoneType);
        dest.writeString(this.contactName);
        dest.writeParcelable(this.photoUri, flags);
        dest.writeByte(this.shouldRecord ? (byte) 1 : (byte) 0);
        dest.writeValue(this.color);
    }

    protected Contact(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.phoneNumber = in.readString();
        this.phoneType = in.readInt();
        this.contactName = in.readString();
        this.photoUri = in.readParcelable(Uri.class.getClassLoader());
        this.shouldRecord = in.readByte() != 0;
        this.color = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
