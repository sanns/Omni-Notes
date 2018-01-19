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

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import edu.emory.mathcs.backport.java.util.Arrays;
import it.feio.android.omninotes.async.UpdaterTask;
import it.feio.android.omninotes.async.bus.PasswordRemovedEvent;
import it.feio.android.omninotes.async.bus.SwitchFragmentEvent;
import it.feio.android.omninotes.async.notes.NoteProcessorDelete;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.helpers.NotesHelper;
import it.feio.android.omninotes.intro.IntroActivity;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.PasswordHelper;
import it.feio.android.omninotes.utils.SystemHelper;


public class MainActivity extends BaseActivity implements OnDateSetListener, OnTimeSetListener {

    @BindView(R.id.crouton_handle) ViewGroup croutonViewContainer;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;

    public final String FRAGMENT_DRAWER_TAG = "fragment_drawer";
    public final String FRAGMENT_LIST_TAG = "fragment_list";
    public final String FRAGMENT_DETAIL_TAG = "fragment_detail";
    public final String FRAGMENT_SKETCH_TAG = "fragment_sketch";
    private FragmentManager mFragmentManager;
    public Uri sketchUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.OmniNotesTheme_ApiSpec);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        // This method starts the bootstrap chain.
        checkPassword();

        initUI();

        if (IntroActivity.mustRun()) {
            startActivity(new Intent(this.getApplicationContext(), IntroActivity.class));
        }

        new UpdaterTask(this).execute();
    }


	@Override
	protected void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}


	private void initUI() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }


    private void checkPassword() {
        if (prefs.getString(Constants.PREF_PASSWORD, null) != null
          && prefs.getBoolean("settings_password_access", false)) {
            PasswordHelper.requestPassword(this, passwordConfirmed -> {
                if (passwordConfirmed) {
                    init();
                } else {
                    finish();
                }
            });
        } else {
            init();
        }
    }


	public void onEvent(PasswordRemovedEvent passwordRemovedEvent) {
		init();
	}


    //@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //return ((AppCompatActivity)this).onKeyDown(keyCode, event);

        //вариант: можно выполнять super, а потом всё равно возвращать false.

        // override BaseActivity's unclear behavior.
        return !(keyCode == KeyEvent.KEYCODE_MENU) && super.onKeyDown(keyCode, event);
    }






	private void init() {
        mFragmentManager = getSupportFragmentManager();

        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment) mFragmentManager
                .findFragmentById(R.id.navigation_drawer);
        if (navigationDrawerFragment == null) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.navigation_drawer, new NavigationDrawerFragment(), 
                    FRAGMENT_DRAWER_TAG).commit();
        }

        if (mFragmentManager.findFragmentByTag(FRAGMENT_LIST_TAG) == null) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, new ListFragment(), FRAGMENT_LIST_TAG).commit();
        }

        // Handling of Intent actions
        handleIntents();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction() == null) {
            intent.setAction(Constants.ACTION_START_APP);
        }
        setIntent(intent);
        handleIntents();
        Log.d(Constants.TAG, "onNewIntent");
        super.onNewIntent(intent);
    }


    public MenuItem getSearchMenuItem() {
        Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
        if (f != null) {
            return ((ListFragment) f).getSearchMenuItem();
        } else {
            return null;
        }
    }


    public void editTag(Category tag) {
        Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
        if (f != null) {
            ((ListFragment) f).editCategory(tag);
        }
    }


    public void initNotesList(Intent intent) {
        Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
        if (f != null) {
            ((ListFragment) f).toggleSearchLabel(false);
            ((ListFragment) f).initNotesList(intent);
        }
    }


    public void commitPending() {
        Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
        if (f != null) {
            ((ListFragment) f).commitPending();
        }
    }


    /**
     * Checks if allocated fragment is of the required type and then returns it or returns null
     */
    private Fragment checkFragmentInstance(int id, Object instanceClass) {
        Fragment result = null;
        if (mFragmentManager != null) {
            Fragment fragment = mFragmentManager.findFragmentById(id);
            if (instanceClass.equals(fragment.getClass())) {
                result = fragment;
            }
        }
        return result;
    }


    /*
     * (non-Javadoc)
     * @see android.support.v7.app.ActionBarActivity#onBackPressed()
     *
     * Overrides the onBackPressed behavior for the attached fragments
     */
    public void onBackPressed() {

        Fragment f;

        // SketchFragment
        f = checkFragmentInstance(R.id.fragment_container, SketchFragment.class);
        if (f != null) {
            ((SketchFragment) f).save();

            // Removes forced portrait orientation for this fragment
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            mFragmentManager.popBackStack();
            return;
        }

        // DetailFragment
        f = checkFragmentInstance(R.id.fragment_container, DetailFragment.class);
        if (f != null) {
            ((DetailFragment) f).goBack = true;
            ((DetailFragment) f).saveAndExit((DetailFragment) f);
            return;
        }

        // ListFragment
        f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
        if (f != null) {
            boolean openOnExit = prefs.getBoolean("settings_navdrawer_on_exit", false);

            // Before exiting from app the navigation drawer is opened
            if (openOnExit
            && getDrawerLayout() != null && !getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
                getDrawerLayout().openDrawer(GravityCompat.START);
            } else if (!openOnExit
            && getDrawerLayout() != null && getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
                getDrawerLayout().closeDrawer(GravityCompat.START);
            } else {
                if (!((ListFragment)f).closeFab()) {
                    super.onBackPressed();
                }
            }
            return;
        }
        super.onBackPressed();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("navigationTmp", navigationTmp);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Crouton.cancelAllCroutons();
    }








    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }


    public ActionBarDrawerToggle getDrawerToggle() {
        if (mFragmentManager != null && mFragmentManager.findFragmentById(R.id.navigation_drawer) != null) {
            return ((NavigationDrawerFragment) mFragmentManager.findFragmentById(R.id.navigation_drawer)).mDrawerToggle;
        } else {
            return null;
        }
    }


    /**
     * Finishes multiselection mode started by ListFragment
     */
    public void finishActionMode() {
        ListFragment fragment = (ListFragment) mFragmentManager.findFragmentByTag(FRAGMENT_LIST_TAG);
        if (fragment != null) {
            fragment.finishActionMode();
        }
    }


    Toolbar getToolbar() {
        return this.toolbar;
    }


    /**
    * функция для ...
    * */
    private void handleIntents() { // все if здесь следовало бы поменять на elseif ?
        Intent i = getIntent();

        if (i.getAction() == null) return;

        if (Constants.ACTION_RESTART_APP.equals(i.getAction())) {
            SystemHelper.restartApp(getApplicationContext(), MainActivity.class);
        }

        if (receivedIntent(i)) {
            Note note = i.getParcelableExtra(Constants.INTENT_NOTE);
            if (note == null) {
                note = DbHelper.getInstance()
                .getNote(i.getIntExtra(Constants.INTENT_KEY, 0));
            }
            // Checks if the same note is already opened to avoid to open again
            if (note != null && noteAlreadyOpened(note)) {
                return;
            }
            // Empty note instantiation
            if (note == null) {
                note = new Note();
            }
            switchToDetail(note);
            return;
        }

        if (Constants.ACTION_SEND_AND_EXIT.equals(i.getAction())) {
            saveAndExit(i);
            return;
        }

        // Tag search
        if (Intent.ACTION_VIEW.equals(i.getAction())) {
            switchToList();
            return;
        }

        // Home launcher shortcut widget
        if (Constants.ACTION_SHORTCUT_WIDGET.equals(i.getAction())) {
            switchToDetail(new Note());
            return;
        }
    }


    /**
     * Used to perform a quick text-only note saving (eg. Tasker+Pushbullet)
     */
    private void saveAndExit(Intent i) {
        Note note = new Note();
        note.setTitle(i.getStringExtra(Intent.EXTRA_SUBJECT));
        note.setContent(i.getStringExtra(Intent.EXTRA_TEXT));
        DbHelper.getInstance().updateNote(note, true);
        showToast(getString(R.string.note_updated), Toast.LENGTH_SHORT);
        finish();
    }

    /**
     * некий признак интента, по которому надо открыть запись.
     * @param i
     * @return
     */
    private boolean receivedIntent(Intent i) {
        return Constants.ACTION_SHORTCUT.equals(i.getAction())
                || Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())
                || Constants.ACTION_WIDGET.equals(i.getAction())
                || Constants.ACTION_TAKE_PHOTO.equals(i.getAction())
                || ((Intent.ACTION_SEND.equals(i.getAction())
                || Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
                || Constants.INTENT_GOOGLE_NOW.equals(i.getAction()))
                && i.getType() != null)
                || i.getAction().contains(Constants.ACTION_NOTIFICATION_CLICK);
    }


    private boolean noteAlreadyOpened(Note note) {
        DetailFragment detailFragment = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
        return detailFragment != null && NotesHelper.haveSameId(note, detailFragment.getCurrentNote());
    }


    public void switchToList() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        animateTransition(transaction, TRANSITION_HORIZONTAL);
        ListFragment mListFragment = new ListFragment();
        transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack
                (FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
        if (getDrawerToggle() != null) {
            getDrawerToggle().setDrawerIndicatorEnabled(false);
        }
        mFragmentManager.getFragments();
        EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.PARENT));
    }

    /**
     * set DetailFragment as content with a note as bundle.

     */
    public void switchToDetail(Note note) {

        Bundle b = new Bundle();
        b.putParcelable(Constants.INTENT_NOTE, note);
        DetailFragment mDetailFragment = new DetailFragment();
        mDetailFragment.setArguments(b);

        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        animateTransition(transaction, TRANSITION_HORIZONTAL);

        if (mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG) == null) {
            transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG)
            .addToBackStack(FRAGMENT_LIST_TAG).commitAllowingStateLoss(); // имя означает, куда следует вернуться при back?
        } else {
            transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG)
              .addToBackStack(FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
        }
    }

    protected Intent createShareNoteIntent(Note note){
        String titleText = note.getTitle();

        String contentText = titleText
          + System.getProperty("line.separator")
          + note.getContent();


        Intent shareIntent = new Intent();
        // Prepare sharing intent with only text
        if (note.getAttachmentsList().size() == 0) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

        } else if (note.getAttachmentsList().size() == 1) {
            // Intent with single image attachment
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType(note.getAttachmentsList().get(0).getMime_type());
            shareIntent.putExtra(Intent.EXTRA_STREAM, note.getAttachmentsList().get(0).getUri());

        } else if (note.getAttachmentsList().size() > 1) {
            // Intent with multiple images
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);

            // Для этого случая, телеграм принимает только аттачменты, выкидывая экстра тему и текст. Причем, если файлы не картики хоть один, то принимает не как "фотопленку".

            ArrayList<Uri> uris = new ArrayList<>();
            HashMap<String, Boolean> mimeTypes = new HashMap<>(); // todo а зачем Map ?
            for (Attachment attachment : note.getAttachmentsList()) {
                uris.add(attachment.getUri());
                mimeTypes.put(attachment.getMime_type(), true);
            }

            // A check to decide the mime type of attachments to share is done here
            // If many mime types are present a general type is assigned to intent
            if (mimeTypes.size() > 1) {
                shareIntent.setType("*/*");
            } else {
                shareIntent.setType((String) mimeTypes.keySet().toArray()[0]);
            }

            // откуда вк и телега знают, что здесь, а не в intent data будут лежать uri?
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
        shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

        return shareIntent;
    }

    /**
     * Notes sharing
     */
    public void shareNote(Note note) {
        Intent shareIntent = createShareNoteIntent(note);

        Intent chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.share_message_chooser));

        Log.d( "Intents resolved" , getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY).toString() );

        // Verify the original intent will resolve to at least one activity
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        }
    }


    /**
     * Single note permanent deletion
     *
     * @param note Note to be deleted
     */
    public void deleteNote(Note note) {
        new NoteProcessorDelete(Arrays.asList(new Note[]{note})).process();
        BaseActivity.notifyAppWidgets(this);
        Log.d(Constants.TAG, "Deleted permanently note with id '" + note.get_id() + "'");
    }


    public void showMessage(int messageId, Style style) {
        showMessage(getString(messageId), style);
    }


    public void showMessage(String message, Style style) {
        // ViewGroup used to show Crouton keeping compatibility with the new Toolbar
		runOnUiThread(() -> Crouton.makeText(this, message, style, croutonViewContainer).show());
    }




    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear,
                          int dayOfMonth) {
        DetailFragment f = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
        if (f != null && f.isAdded() && f.onDateSetListener != null) {
            f.onDateSetListener.onDateSet(view, year, monthOfYear, dayOfMonth);
        }
    }


    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        DetailFragment f = (DetailFragment) mFragmentManager.findFragmentByTag(FRAGMENT_DETAIL_TAG);
        if (f != null && f.isAdded()) {
            f.onTimeSetListener.onTimeSet(view, hourOfDay, minute);
        }
    }


}
