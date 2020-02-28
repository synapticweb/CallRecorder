/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.recorder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ControlRecordingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        RecorderService service = RecorderService.getService();

        if(intent.getAction().equals(RecorderService.ACTION_STOP_SPEAKER)) {
            service.putSpeakerOff();
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, 0));
        }

        else if(intent.getAction().equals(RecorderService.ACTION_START_SPEAKER)) {
            service.putSpeakerOn();
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, 0));
        }
    }
}
