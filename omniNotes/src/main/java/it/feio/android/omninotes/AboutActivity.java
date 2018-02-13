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
package it.feio.android.omninotes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.FrameLayout;

import it.feio.android.omninotes.helpers.PermissionsHelper;


public class AboutActivity extends BaseActivity {

    FrameLayout root;
    AboutActivity mActivity = this;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        root = (FrameLayout) findViewById(R.id.root);

        WebView webview = (WebView) findViewById(R.id.webview);
        webview.loadUrl("file:///android_asset/html/about.html");

        initUI();
    }


	@Override
  public void onStart() {
    ((OmniNotes) getApplication()).getAnalyticsHelper().trackScreenView(getClass().getName());
    super.onStart();
  }


    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_settings, menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.menu_go_to_settings:
        PermissionsHelper.startSettingsAppForResult(mActivity);
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    PermissionsHelper.checkForPermissions(requestCode, mActivity, root);
  }


  private void initUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onNavigateUp());
    }

}
