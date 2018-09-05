package net.synapticweb.callrecorder;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static net.synapticweb.callrecorder.AppLibrary.SQLITE_TRUE;

public class ListContactsFragment extends Fragment {
    RecyclerView listenedPhones;
    ListenedAdapter adapter;

    public void updateContactsList() {
        adapter.phoneNumbers = this.getPhoneNumbersList();
        adapter.notifyDataSetChanged();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        listenedPhones = (RecyclerView) inflater.inflate(R.layout.list_contacts_fragment, container, false);
        listenedPhones.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ListenedAdapter(this.getPhoneNumbersList());
        listenedPhones.setAdapter(adapter);
        return listenedPhones;
    }

    private List<PhoneNumber> getPhoneNumbersList() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getActivity());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<PhoneNumber> phoneNumbers = new ArrayList<>();

        Cursor cursor = db.
                query(ListenedContract.Listened.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext())
        {
            PhoneNumber phoneNumber = new PhoneNumber();
            String number = cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER));

            //dacă avem un nr necunoscut în db încercăm să vedem dacă nu cumva a fost între timp introdus în contacte.
            // Dacă îl găsim în contacte populăm un obiect PhoneNumber cu datele sale: numele și tipul.
            //Apoi modificăm cu PhoneNumber::updateNumber() recordul din baza de date, setăm id-ul obiectului nou creat și
            //îl introducem în lista phoneNumbers. Cum cursorul conține datele de dinainte de updatarea db, nu are rost
            // să mergem mai jos și ne ducem la următoarea iterație cu continue.
            if(cursor.getInt(
                    cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_UNKNOWN_NUMBER)) == SQLITE_TRUE) {
                if((phoneNumber = PhoneNumber.searchNumberInContacts(number, getActivity())) != null)
                {
                    phoneNumber.updateNumber(getActivity(), true);
                    phoneNumber.setId(cursor.getLong(cursor.getColumnIndex(ListenedContract.Listened._ID)));
                    phoneNumbers.add(phoneNumber);
                    continue;
                }
                else { //dacă nu a fost găsit în contacte, trebuie să recreem phoneNumber întrucît acum este null.
                    // Apoi setăm unknownNumber la true, pentru că nefiind găsit rămîne necunoscut.
                    phoneNumber = new PhoneNumber();
                    phoneNumber.setUnkownNumber(true);
                }
            }

            if(cursor.getInt(
                    cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PRIVATE_NUMBER)) == SQLITE_TRUE)
                phoneNumber.setPrivateNumber(true);

            phoneNumber.setContactName(
                    cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME)));
            phoneNumber.setPhoneNumber(number);
            phoneNumber.setPhotoUri(
                    cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PHOTO_URI)));

            phoneNumber.setPhoneType(
                    cursor.getInt(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE)));
            phoneNumber.setId(cursor.getLong(cursor.getColumnIndex(ListenedContract.Listened._ID)));
            phoneNumber.setShouldRecord(
                    cursor.getInt(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD)) == 1);

            phoneNumbers.add(phoneNumber);
        }

        cursor.close();
        Collections.sort(phoneNumbers);
        return phoneNumbers;
    }

    public class PhoneHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView contactPhoto;
        TextView mContactName;
        TextView mPhoneNumber;
        PhoneNumber number;

        PhoneHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.listened_phone, parent, false));
            itemView.setOnClickListener(this);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            mContactName = itemView.findViewById(R.id.contact_name);
            mPhoneNumber = itemView.findViewById(R.id.phone_number);
        }

        @Override
        public void onClick(View view) {
            Intent detailIntent = new Intent(getActivity(), ContactDetailActivity.class);
            detailIntent.putExtra("phoneNumber", number);
            startActivity(detailIntent);
        }
    }

    class ListenedAdapter extends RecyclerView.Adapter<PhoneHolder> {
        List<PhoneNumber> phoneNumbers;
        ListenedAdapter(List<PhoneNumber> list){
            phoneNumbers = list;
        }

        @Override
        @NonNull
        public PhoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new PhoneHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PhoneHolder holder, int position) {
            PhoneNumber phoneNumber = phoneNumbers.get(position);

            if(phoneNumber.getPhotoUri() != null) {
                holder.contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
                holder.contactPhoto.setImageURI(phoneNumber.getPhotoUri());
            }
            else {
                if(phoneNumber.isPrivateNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_yellow);
                else if(phoneNumber.isUnkownNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_red);
                else
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_blue);
            }

            holder.mContactName.setText(phoneNumber.getContactName());
            holder.number = phoneNumber;
            if(!phoneNumber.isPrivateNumber())
                holder.mPhoneNumber.setText(phoneNumber.getPhoneNumber());
        }

        @Override
        public int getItemCount() {
            return phoneNumbers.size();
        }

    }
}
