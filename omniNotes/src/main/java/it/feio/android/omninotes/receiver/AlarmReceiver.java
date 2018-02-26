/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.feio.android.omninotes.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;

import java.util.Calendar;

import it.feio.android.omninotes.R;
import it.feio.android.omninotes.SnoozeActivity;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.services.NotificationListener;
import it.feio.android.omninotes.utils.*; //?????


public class AlarmReceiver extends BroadcastReceiver {
	SharedPreferences prefs;
	String snoozeDelay;

	@Override
	public void onReceive(Context mContext, Intent intent) {
		try {
			// Retrieving preferences
			prefs = mContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS);
			snoozeDelay = prefs.getString("settings_notification_snooze_delay", Constants.PREF_SNOOZE_DEFAULT); // this value can become  unactual by the time user sees it. Because preferences can be changed unrelated to notification action text. Confirmed by experiment.

			Note note = ParcelableUtil.unmarshall(
				intent.getExtras().getByteArray(Constants.INTENT_NOTE),
				Note.CREATOR
			);
			createNotification(mContext, note);
			SnoozeActivity.setNextRecurrentReminder(note);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !NotificationListener.isRunning()) {
				DbHelper.getInstance().setReminderFired(note.get_id(), true);
			}

			long newReminder = Calendar.getInstance().getTimeInMillis() + Integer.parseInt(snoozeDelay) * 60 * 1000;
			SnoozeActivity.updateNoteReminder(newReminder, note);
		} catch (Exception e) {
			Log.e(Constants.TAG, "Error on receiving reminder", e);
		}
	}


	private void createNotification(Context mContext, Note note) {


		// Prepare text contents
		Spanned[] titleAndContent = TextHelper.parseTitleAndContent(mContext, note);
		String title = TextHelper.getAlternativeTitle(mContext, note, titleAndContent[0]);
		String text = titleAndContent[1].toString();

		Intent snoozeIntent = new Intent(mContext, SnoozeActivity.class);
		snoozeIntent.setAction(Constants.ACTION_SNOOZE);
		snoozeIntent.putExtra(Constants.INTENT_NOTE, (android.os.Parcelable) note); // why casting?
		PendingIntent piSnooze = PendingIntent.getActivity(mContext, getUniqueRequestCode(note), snoozeIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Intent postponeIntent = new Intent(mContext, SnoozeActivity.class);
		postponeIntent.setAction(Constants.ACTION_POSTPONE);
		postponeIntent.putExtra(Constants.INTENT_NOTE, (android.os.Parcelable) note);
		PendingIntent piPostpone = PendingIntent.getActivity(mContext, getUniqueRequestCode(note), postponeIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Next create the bundle and initialize it
		Intent intent = new Intent(mContext, SnoozeActivity.class);
		Bundle bundle = new Bundle();
		bundle.putParcelable(Constants.INTENT_NOTE, note);
		intent.putExtras(bundle);

		// Sets the Activity to start in a new, empty task
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// Workaround to fix problems with multiple notifications
		intent.setAction(Constants.ACTION_NOTIFICATION_CLICK + Long.toString(System.currentTimeMillis()));

		// Creates the Content Intent
		PendingIntent notifyIntent = PendingIntent.getActivity(mContext, getUniqueRequestCode(note), intent,
				PendingIntent.FLAG_UPDATE_CURRENT);



		NotificationsHelper notificationsHelper = new NotificationsHelper(mContext);
		notificationsHelper.createNotification(R.drawable.ic_stat_notification, title, notifyIntent).setLedActive()
				.setMessage(text);

		// todo why and how does it parcellate attachments?
		if (note.getAttachmentsList().size() > 0 && !note.getAttachmentsList().get(0).getMime_type().equals(Constants
				.MIME_TYPE_FILES)) {
			notificationsHelper.setLargeIcon(BitmapHelper.getBitmapFromAttachment(mContext, note.getAttachmentsList()
					.get(0), 128, 128));
		}

		notificationsHelper.getBuilder()
			.addAction(
				R.drawable.ic_material_reminder_time_light,
				it.feio.android.omninotes.utils.TextHelper.capitalize(mContext.getString(R.string.snooze))
					+ ": " + snoozeDelay,
				piSnooze
			)
			.addAction(
				R.drawable.ic_remind_later_light,
				it.feio.android.omninotes.utils.TextHelper.capitalize(mContext.getString(R.string
					.add_reminder)),
				piPostpone
			);

		setRingtone(prefs, notificationsHelper);

		setVibrate(prefs, notificationsHelper);

		notificationsHelper.setOngoing();

		notificationsHelper.show(note.get_id());
	}


	private void setRingtone(SharedPreferences prefs, NotificationsHelper notificationsHelper) {
		String ringtone = prefs.getString("settings_notification_ringtone", null);
		if (ringtone != null) notificationsHelper.setRingtone(ringtone);
	}


	private void setVibrate(SharedPreferences prefs, NotificationsHelper notificationsHelper) {
		if (prefs.getBoolean("settings_notification_vibration", true)) notificationsHelper.setVibration();
	}


	private int getUniqueRequestCode(Note note) {
		return note.get_id().intValue();
	}
}
