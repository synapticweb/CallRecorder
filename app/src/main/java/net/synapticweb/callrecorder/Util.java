package net.synapticweb.callrecorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import net.synapticweb.callrecorder.data.Contact;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Util {
    public static void copyPhotoFromPhoneContacts(Context context, Contact contact) {
        Uri photoUri = contact.getPhotoUri();
        try {
            Bitmap originalPhotoBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
            //am adăugat System.currentTimeMillis() pentru consistență cu EditContactActivity.setPhotoPath().
            File copiedPhotoFile = new File(context.getFilesDir(), contact.getPhoneNumber() + System.currentTimeMillis() + ".jpg");
            OutputStream os = new FileOutputStream(copiedPhotoFile);
            originalPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 70, os);
            contact.setPhotoUri(FileProvider.getUriForFile(context, Config.FILE_PROVIDER, copiedPhotoFile));
        } catch (IOException exception) {
            CrLog.log(CrLog.ERROR, "IO exception: Could not copy photo from phone contacts: " + exception.getMessage());
            contact.setPhotoUri((Uri) null);
        }
    }
}
