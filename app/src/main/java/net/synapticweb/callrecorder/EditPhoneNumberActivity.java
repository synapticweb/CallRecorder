package net.synapticweb.callrecorder;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.File;
import java.io.IOException;


public class EditPhoneNumberActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private PhoneNumber phoneNumber;
    private ImageView contactPhoto;
    private EditText contactName;
    private EditText contactPhone;
    private Spinner phoneType;
    private boolean dataChanged = false;
    private boolean setInitialPhoneType = false;
    private File savedPhotoPath;
    private Uri savedPhotoUri;
    private static final String TAG = "CallRecorder";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PICTURE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_phone_number);
        Button cancelButton = findViewById(R.id.edit_phone_number_cancel);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataChanged) {
                    new AlertDialog.Builder(EditPhoneNumberActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.discard_edit_title)
                            .setMessage(R.string.discard_edit_message)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
                else
                    finish();
            }
        });

        Button okButton = findViewById(R.id.edit_phone_number_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dataChanged) {
                    phoneNumber.setContactName(contactName.getText().toString());
                    phoneNumber.setPhoneNumber(contactPhone.getText().toString());
//                    Cursor cursor = (Cursor) phoneType.getSelectedItem(); //https://stackoverflow.com/questions/5787809/get-spinner-selected-items-text
                    phoneNumber.setPhoneType(phoneType.getSelectedItem().toString());
                    phoneNumber.setUnkownNumber(false);
                    //Uri-ul pozei a fost deja setat
                    phoneNumber.updateNumber(EditPhoneNumberActivity.this, false);
                    Intent intent = new Intent();
                    intent.putExtra("edited_number", phoneNumber);
                    setResult(RESULT_OK, intent);
                }
                else
                    setResult(RESULT_CANCELED);
                finish();
            }
        });

        contactPhoto = findViewById(R.id.edit_phone_number_photo);
        phoneNumber = getIntent().getExtras().getParcelable("phoneNumber");

        if(phoneNumber.getPhotoUri() != null)
            contactPhoto.setImageURI(phoneNumber.getPhotoUri());
        else {
            if(phoneNumber.isPrivateNumber())
                contactPhoto.setImageResource(R.drawable.user_contact_yellow);
            else if(phoneNumber.isUnkownNumber())
                contactPhoto.setImageResource(R.drawable.user_contact_red);
            else
                contactPhoto.setImageResource(R.drawable.user_contact_blue);
        }

        registerForContextMenu(contactPhoto);
        contactPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.showContextMenu();
            }
        });

        contactName = findViewById(R.id.edit_name);
        contactName.setText(phoneNumber.getContactName(), TextView.BufferType.EDITABLE);
        contactName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                dataChanged = true;
            }
        });

        contactPhone = findViewById(R.id.edit_number);
        contactPhone.setText(phoneNumber.getPhoneNumber(), TextView.BufferType.EDITABLE);
        contactPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                dataChanged = true;
            }
        });

        //Pentru aceeași chestie cu baze de date: https://stackoverflow.com/questions/13413030/using-simplecursoradapter-with-spinner
        phoneType = findViewById(R.id.edit_types);
        phoneType.setOnItemSelectedListener(this);
        ArrayAdapter<PhoneTypeContainer> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GlobalConstants.PHONE_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        phoneType.setAdapter(adapter);

        int position;
        for(position = 0; position < GlobalConstants.PHONE_TYPES.size(); ++position)
            if(GlobalConstants.PHONE_TYPES.get(position).getTypeCode() == phoneNumber.getPhoneTypeCode())
                break;
        //https://stackoverflow.com/questions/11072576/set-selected-item-of-spinner-programmatically
        phoneType.setSelection(adapter.getPosition(GlobalConstants.PHONE_TYPES.get(position)));

        savedPhotoPath = new File(getFilesDir(), phoneNumber.getPhoneNumber() + ".jpg");
        savedPhotoUri = FileProvider.getUriForFile(this, "net.synapticweb.callrecorder.fileprovider", savedPhotoPath);
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
        if(phoneNumber.getPhotoUri() == null)
            menuItem.setEnabled(false);
        menuItem = menu.getItem(2);
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            menuItem.setEnabled(false);
    }

//http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
    private void selectPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void removePhoto() {
        phoneNumber.setPhotoUri((Uri) null); //ambigous method call
        contactPhoto.setImageResource(R.drawable.user_contact_blue);
        dataChanged = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri photoUri;
        if (resultCode != Activity.RESULT_OK ) {
            Log.wtf(TAG, "The result code is error");
            if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Exception error = result.getError();
                Log.wtf(TAG, error.getMessage());
            }
            return;
        }

        if (requestCode == PICK_IMAGE_REQUEST && (photoUri = data.getData()) != null) {
            CropImage.activity(photoUri).setCropShape(CropImageView.CropShape.OVAL)
                    .setOutputUri(savedPhotoUri).setMaxCropResultSize(2000, 2000) //vezi mai jos comentariul
                    .start(this);
        }
        else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            photoUri = result.getUri();
            contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhoto.setImageURI(photoUri);
            phoneNumber.setPhotoUri(photoUri);
            dataChanged = true;
        }
        else if(requestCode == TAKE_PICTURE) {
            CropImage.activity(savedPhotoUri).setCropShape(CropImageView.CropShape.OVAL)
                    .setOutputUri(savedPhotoUri).setMaxCropResultSize(2000, 2000) //necesar, pentru că dacă poza e prea mare apare un rotund negru
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
        dataChanged = true;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void takePhoto(){
        try {
            savedPhotoPath.createNewFile();
        }
        catch (IOException ioe) {
            Log.wtf(TAG, ioe.getMessage());
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, savedPhotoUri);
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, TAKE_PICTURE);
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
