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

package it.feio.android.omninotes.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;
import it.feio.android.omninotes.OmniNotes;
import it.feio.android.omninotes.R;
import it.feio.android.omninotes.async.notes.SaveNoteTask;
import it.feio.android.omninotes.helpers.date.DateHelper;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.receiver.AlarmReceiver;
import it.feio.android.omninotes.utils.date.DateUtils;

import java.util.Calendar;


public class ReminderHelper {

	/**
	 * Sets the reminder on {note}.getAlarm().
	 * {@link #addReminder addReminder(Context, Note, long)}
	 * @see #addReminder(Context, Note, long)
	 * */
	public static void addReminder(Context context, Note note) {
		String alarm = note.getAlarm();
		if (alarm == null) return;

		addReminder(context, note, Long.parseLong(alarm));
	}

	/**
	 * Only if the {reminder} is {@link DateUtils#isFuture(long) future} will marshall the {note}
	 * and put it in broadcast. The broadcast will be set on the {@link AlarmManager AlarmManager}.
	 *
	 * @param context of app is for getting alarm system service and PendingIntent.getBroadcast()
	 */
	public static void addReminder(Context context, Note note, long reminder) {
		if (DateUtils.isFuture(reminder)) {
			Intent intent = new Intent(context, AlarmReceiver.class); //видимо , этот бродкаст получит только AlarmReceiver в onReceive.
			intent.putExtra(Constants.INTENT_NOTE, ParcelableUtil.marshall(note));

      PendingIntent sender = PendingIntent.getBroadcast(
        context, getRequestCode(note),
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT
      );

      AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				am.setExact(AlarmManager.RTC_WAKEUP, reminder, sender);
			} else {
				am.set(AlarmManager.RTC_WAKEUP, reminder, sender);
			}
		}
	}


	/**
	 * Checks if there is any reminder for a given note
	 */
	public static boolean checkReminder(Context context, Note note) {
		return PendingIntent.getBroadcast(context, getRequestCode(note), new Intent(context, AlarmReceiver
				.class), PendingIntent.FLAG_NO_CREATE) != null;
	}


	static int getRequestCode(Note note) {
		//Why could be no creation date?
		Long longCode = note.getCreation() != null ? note.getCreation() : Calendar.getInstance().getTimeInMillis() / 1000L;
		return longCode.intValue();
	}


	public static void removeReminder(Context context, Note note) {
		if (!TextUtils.isEmpty(note.getAlarm())) {
			cancelAlarm(context, note);
		}
	}

	/**
	 * Instantly creates a PendingIntent same as for alarm,
	 * but uses it for {@link AlarmManager#cancel AlarmManager.cancel()},
	 * not {@link AlarmManager#set AlarmManager.set()} to time.
	 * @param note for which cancel in {@link AlarmManager}.
	 * */
	public static void cancelAlarm(Context context, Note note) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent p = PendingIntent.getBroadcast(context, getRequestCode(note), intent, 0);

		am.cancel(p);
		p.cancel();
	}


	/**
	 * Shows a toast
	 * */
	public static void showReminderMessage(String reminderString) {
		if (reminderString != null) {
			long reminderDate = Long.parseLong(reminderString);
			if (reminderDate > Calendar.getInstance().getTimeInMillis()) {
				new Handler(OmniNotes.getAppContext().getMainLooper()).post(() -> Toast.makeText(
						OmniNotes.getAppContext(),
						OmniNotes.getAppContext().getString(R.string.alarm_set_on) + " " + DateHelper.getDateTimeShort(OmniNotes.getAppContext(), reminderDate), Toast.LENGTH_LONG)
						.show());
			}
		}
	}

	/**
   * Updates the note with new alarm property depending on recurrence rule. This leads to {@link SaveNoteTask SaveNoteTask}.
   * Or saves a task in AsyncTask. Why?
   * todo move out of Activity
   * */
  public static void setNextRecurrentReminder(Note note) {
      String rule = note.getRecurrenceRule();

      if (!TextUtils.isEmpty(rule)) {
          long nextReminder = DateHelper.nextReminderFromRecurrenceRule(Long.parseLong(note.getAlarm()), rule);
          if (nextReminder > 0) {
              updateNoteReminder(nextReminder, note, true);
          }
      } else {
          new SaveNoteTask(false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, note);
      }
  }

	/**
   * Sets the alarm without updating the {note}
   * @param note to marshall for passing to the AlarmManager.
   * @see #updateNoteReminder(long, Note, boolean)
   * */
  public static void updateNoteReminder(long reminder, Note note) {
      addReminder(OmniNotes.getAppContext(), note, reminder);
      showReminderMessage(note.getAlarm());
  }

	/**
   * If {updateNote} is true , calls {noteToUpdate}.setAlarm({reminder}) and {@link SaveNoteTask SaveNoteTask}.
   * */
  public static void updateNoteReminder(long reminder, Note noteToUpdate, boolean updateNote) {
      if (updateNote) {
          noteToUpdate.setAlarm(reminder);
          new SaveNoteTask(false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteToUpdate);
      } else {
          updateNoteReminder(reminder, noteToUpdate);
      }
  }
}
