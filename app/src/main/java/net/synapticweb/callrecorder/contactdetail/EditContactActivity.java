/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.synapticweb.callrecorder.Config;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Repository;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;


public class EditContactActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private Contact contact;
    private ImageView contactPhoto;
    private EditText contactName;
    private EditText contactPhone;
    private Spinner phoneType;
    private boolean dataChanged = false;
    private boolean setInitialPhoneType = false;
    private File savedPhotoPath = null;
    private Uri oldPhotoUri = null;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PICTURE = 2;
    public static final String EDITED_CONTACT = "edited_contact";
    @Inject
    Repository repository;

    @Override
    protected Fragment createFragment() {
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfThemeChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("contact", contact);
        outState.putBoolean("dataChanged", dataChanged);
        outState.putBoolean("setInitialPhoneType", setInitialPhoneType);
        outState.putSerializable("savedPhotoPath", savedPhotoPath);
        outState.putParcelable("oldPhotoUri", oldPhotoUri);
    }

    private void onCancelOrBackPressed() {
        if (dataChanged) {
            new MaterialDialog.Builder(EditContactActivity.this)
                    .title(R.string.warning_title)
                    .icon(getResources().getDrawable(R.drawable.warning))
                    .content(R.string.discard_edit_message)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if(savedPhotoPath != null) {
                                getContentResolver().delete(contact.getPhotoUri(), null, null);
                            }
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    })
                    .show();
        }
        else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
            onCancelOrBackPressed();
        //https://stackoverflow.com/questions/39715619/onactivityresult-not-called-when-pressed-back-arrow-on-screen-but-only-called-wh
        //e foarte important să întoarcă true și nu altceva.
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((CrApp) getApplication()).appComponent.inject(this);
        super.onCreate(savedInstanceState);
        setTheme();
        setContentView(R.layout.edit_contact_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_edit);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            contact = savedInstanceState.getParcelable("contact");
            dataChanged = savedInstanceState.getBoolean("dataChanged");
            setInitialPhoneType = savedInstanceState.getBoolean("setInitialPhoneType");
            savedPhotoPath = (File) savedInstanceState.getSerializable("savedPhotoPath");
            oldPhotoUri = savedInstanceState.getParcelable("oldPhotoUri");
        }
        else
            contact = getIntent().getExtras().getParcelable(ContactDetailFragment.EDIT_EXTRA_CONTACT);

        ImageButton okButton = findViewById(R.id.edit_done);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dataChanged) {
//                    Cursor cursor = (Cursor) phoneType.getSelectedItem(); //https://stackoverflow.com/questions/5787809/get-spinner-selected-items-text
                    if(oldPhotoUri != null) { //a fost selectată altă poză din galerie sau a fost luată altă poză
                        // cu camera sau a fost scoasă poza existentă.
                        //întotdeauna este poza noastră!
                            getContentResolver().delete(oldPhotoUri, null, null);
                    }

                    contact.update(repository);
                    Intent intent = new Intent();
                    intent.putExtra(EDITED_CONTACT, contact);
                    setResult(RESULT_OK, intent);
                }
                else
                    setResult(RESULT_CANCELED);
                finish();
            }
        });

        contactPhoto = findViewById(R.id.edit_phone_number_photo);

        if(contact.getPhotoUri() != null)
            contactPhoto.setImageURI(contact.getPhotoUri());
        else {
            if(contact.isPrivateNumber())
                contactPhoto.setImageResource(R.drawable.incognito);
            else
                contactPhoto.setImageResource(R.drawable.user_contact);
                contactPhoto.setColorFilter(new
                    PorterDuffColorFilter(contact.getColor(), PorterDuff.Mode.LIGHTEN));
        }

        registerForContextMenu(contactPhoto);
        contactPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.showContextMenu();
            }
        });

        contactName = findViewById(R.id.edit_name);
        contactName.setText(contact.getContactName(), TextView.BufferType.EDITABLE);
        contactName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(!contactName.getText().toString().equals(contact.getContactName())) {
                    contact.setContactName(contactName.getText().toString());
                    dataChanged = true;
                }
            }
        });

        contactPhone = findViewById(R.id.edit_number);
        contactPhone.setText(contact.getPhoneNumber(), TextView.BufferType.EDITABLE);
        contactPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(!contactPhone.getText().toString().equals(contact.getPhoneNumber())) {
                    contact.setPhoneNumber(contactPhone.getText().toString());
                    dataChanged = true;
                }
            }
        });

        //Pentru aceeași chestie cu baze de date: https://stackoverflow.com/questions/13413030/using-simplecursoradapter-with-spinner
        phoneType = findViewById(R.id.edit_types);
        phoneType.setOnItemSelectedListener(this);
        ArrayAdapter<Util.PhoneTypeContainer> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Util.PHONE_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        phoneType.setAdapter(adapter);

        int position;
        for(position = 0; position < Util.PHONE_TYPES.size(); ++position)
            if(Util.PHONE_TYPES.get(position).getTypeCode() == contact.getPhoneTypeCode())
                break;
        //https://stackoverflow.com/questions/11072576/set-selected-item-of-spinner-programmatically
        phoneType.setSelection(adapter.getPosition(Util.PHONE_TYPES.get(position)));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_contact_change_photo, menu);

        //pentru micșorarea fontului: https://stackoverflow.com/questions/29844064/how-to-change-the-menu-text-size
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spanString.length();
            spanString.setSpan(new RelativeSizeSpan(0.87f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spanString);
        }

        MenuItem menuItem = menu.getItem(0);
        if(contact.getPhotoUri() == null)
            menuItem.setEnabled(false);
        menuItem = menu.getItem(2);
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            menuItem.setEnabled(false);
    }

    //timestampul este necesar pentru situația cînd se schimbă pozele și trebuie ștearsă cea veche.
    private void setPhotoPath() {
        savedPhotoPath = new File(getFilesDir(), contact.getPhoneNumber() + System.currentTimeMillis() + ".jpg");
    }

    //http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    private void selectPhoto() {
        setPhotoPath();
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void removePhoto() {
        oldPhotoUri = contact.getPhotoUri();
        contact.setPhotoUri((Uri) null); //ambigous method call
        contactPhoto.setImageResource(R.drawable.user_contact);
        contactPhoto.setColorFilter(new
                PorterDuffColorFilter(contact.getColor(), PorterDuff.Mode.LIGHTEN));
        dataChanged = true;
    }

    private void takePhoto(){
        setPhotoPath();
        try {
            if(!savedPhotoPath.createNewFile())
                throw new IOException("File already exists");
        }
        catch (IOException ioe) {
            CrLog.log(CrLog.ERROR, "Error creating new photo file: " + ioe.getMessage());
            Toast.makeText(this, "Cannot take new photo.", Toast.LENGTH_SHORT).show();
            return ;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, Config.FILE_PROVIDER, savedPhotoPath));
        //fără chestia de mai jos aplicația foto crapă în kitkat cu java.lang.SecurityException:
        // Permission Denial: opening provider android.support.v4.content.FileProvider
        //https://stackoverflow.com/questions/24467696/android-file-provider-permission-denial
        if ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ) {
            intent.setClipData(ClipData.newRawUri("", FileProvider.getUriForFile(this, Config.FILE_PROVIDER, savedPhotoPath)));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri chosenPhotoUri;
        if (resultCode != Activity.RESULT_OK) {
            if(requestCode == TAKE_PICTURE) {
                CrLog.log(CrLog.ERROR, "The take picture activity returned this error code: " + resultCode);
                Toast.makeText(this, "Cannot take picture", Toast.LENGTH_SHORT).show();
            }
            if(requestCode == PICK_IMAGE_REQUEST) {
                CrLog.log(CrLog.ERROR, "The pick image activity returned this error code: " + resultCode);
                Toast.makeText(this, "Cannot pick picture", Toast.LENGTH_SHORT).show();
            }
            if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Exception error = result.getError();
                CrLog.log(CrLog.ERROR,  "Error cropping the image: " + error.getMessage());
                Toast.makeText(this, "Cannot crop the image", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == PICK_IMAGE_REQUEST && (chosenPhotoUri = data.getData()) != null) {
            CropImage.activity(chosenPhotoUri).setCropShape(CropImageView.CropShape.OVAL)
                    .setOutputUri(FileProvider.getUriForFile(this, Config.FILE_PROVIDER, savedPhotoPath))
                    .setAspectRatio(1,1)
                    .setMaxCropResultSize(2000, 2000) //vezi mai jos comentariul
                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setOutputCompressQuality(70)
                    .start(this);
        }
        else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            chosenPhotoUri = result.getUri();
            contactPhoto.clearColorFilter();
            contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhoto.setImageURI(chosenPhotoUri);
            this.oldPhotoUri = contact.getPhotoUri();
            contact.setPhotoUri(chosenPhotoUri);
            dataChanged = true;
        }
        else if(requestCode == TAKE_PICTURE) {
            CropImage.activity(FileProvider.getUriForFile(this, Config.FILE_PROVIDER, savedPhotoPath))
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setOutputUri(FileProvider.getUriForFile(this, Config.FILE_PROVIDER, savedPhotoPath))
                    .setMaxCropResultSize(2000, 2000) //necesar, pentru că dacă poza e prea mare apare un rotund negru
                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG) //necesar, pentru că fișierul output are
                    //totdeauna extensia .jpg
                    .setOutputCompressQuality(70)
                    .start(this);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        if(!setInitialPhoneType) {
            setInitialPhoneType = true;
            return ;
        }
        if(!phoneType.getSelectedItem().toString().equals(contact.getPhoneTypeName())) {
            contact.setPhoneType(phoneType.getSelectedItem().toString());
            dataChanged = true;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onBackPressed() {
        onCancelOrBackPressed();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_photo_remove:
                removePhoto();
                return true;
            case R.id.change_photo_select:
                selectPhoto();
                return true;
            case R.id.change_photo_takenew:
                takePhoto();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
