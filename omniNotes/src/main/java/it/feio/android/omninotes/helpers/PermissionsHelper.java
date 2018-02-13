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

package it.feio.android.omninotes.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;
import it.feio.android.omninotes.R;
import it.feio.android.omninotes.models.listeners.OnPermissionRequestedListener;


public class PermissionsHelper {

	public final static int PERMISSION_ACTIVITY_REQUEST_CODE = 1;

	/**
	 * Lets set a callback on when a permission is granted.
	 * */
	public static void requestPermission(Activity activity, String permission, int rationaleDescription, View
			messageView, OnPermissionRequestedListener onPermissionRequestedListener) {

		if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
				Snackbar.make(messageView, rationaleDescription, Snackbar.LENGTH_INDEFINITE)
						.setAction(R.string.ok, view -> {
							requestPermissionExecute(activity, permission, onPermissionRequestedListener, messageView);
						})
						.show();
			} else {
				//false means neverAsk or first time asking.
				requestPermissionExecute(activity, permission, onPermissionRequestedListener, messageView);
			}
		} else {
			onPermissionRequestedListener.onPermissionGranted();
		}
	}

	/**
	 * Asks for permission and calls the callback if granted. Otherwise shows a snackbar with error. A button on snackbar leads to Settings app.
	 * */
	private static void requestPermissionExecute(Activity activity, String permission, OnPermissionRequestedListener
			onPermissionRequestedListener, View messageView) {
		RxPermissions.getInstance(activity)
				.request(permission)
				.subscribe(granted -> {
					if (granted) {
						onPermissionRequestedListener.onPermissionGranted();
					} else {
						String msg = activity.getString(R.string.permission_not_granted) + ": " + permission;
						Snackbar.make(messageView, msg, Snackbar.LENGTH_LONG)
						.setAction("Настройки", (View v) -> {
							PermissionsHelper.startSettingsAppForResult(activity);
						})
						.show();
					}
				});
	}



	//SANZ17
	public static void showNoPermissionSnackbar(Activity homeForResult, View messageView) {
		Snackbar.make(messageView, "Для редактирования разрешений откройте настройки" , Snackbar.LENGTH_INDEFINITE)
			.setAction("Настройки", new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PermissionsHelper.startSettingsAppForResult(homeForResult);

					Toast.makeText(homeForResult.getApplicationContext(),
						"Откройте Разрешения", Toast.LENGTH_SHORT
          ).show();
				}
			})
			.show();
	}




	//cannot start activity for result from Context
	public static void startSettingsAppForResult(Activity homeForResult) {
		Intent appSettingsIntent = new Intent(
			Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
			Uri.parse("package:" + homeForResult.getPackageName())
		);
		//todo antipattern? неправильно ,что здесь указан реквест код для активити, и запускается forResult, а сам результат отлавливаетяс там в том активти.
		// todo передавать контекст между классами - антипаттерн?
		homeForResult.startActivityForResult(appSettingsIntent, PERMISSION_ACTIVITY_REQUEST_CODE);
	}


	public static void checkForPermissions(int requestCode, Activity homeForResult, View snackbarPlace) {
		if (requestCode == PermissionsHelper.PERMISSION_ACTIVITY_REQUEST_CODE) {
			// попробовать выполнить действие, которое нельзя было сделать без нужного разрешения
			if (ActivityCompat.checkSelfPermission(
				homeForResult,
				Manifest.permission.READ_EXTERNAL_STORAGE
			) != PackageManager.PERMISSION_GRANTED) {

				final String MESSAGE = "Чтение данных недоступно.";

				if (snackbarPlace == null) return;

				Snackbar.make(snackbarPlace, MESSAGE, Snackbar.LENGTH_LONG)
					.setAction("Попробовать еще", new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							//инфа для пользователя, если разрешение Never ask.
							PermissionsHelper.showNoPermissionSnackbar(
								homeForResult, snackbarPlace
							);
						}
					})
					.show();
			}
		}
	}
}