/*
 This file is part of Privacy Friendly App Finance Manager.

 Privacy Friendly App Finance Manager is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Finance Manager is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Finance Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package org.secuso.privacyfriendlyfinance.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import org.secuso.privacyfriendlyfinance.R;
import org.secuso.privacyfriendlyfinance.activities.viewmodel.BaseViewModel;
import org.secuso.privacyfriendlyfinance.databinding.ActivityBaseBinding;

/**
 * @author Christopher Beckmann, Karola Marky, Felix Hofmann, Leonard Otto
 * @version 20181129
 *
 * This class is a parent class of all activities the user can access the Navigation Drawer from.
 * just inject your activities content via the setContent() method.
 */
public abstract class BaseActivity extends AppCompatActivity implements OnNavigationItemSelectedListener {
    // delay to launch nav drawer item, to allow close animation to play
    public static final int NAVDRAWER_LAUNCH_DELAY = 250;
    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    public static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    public static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;

    private CoordinatorLayout contentWrapper;
    private View content;
    private LayoutInflater inflater;

    // Helper
    private Handler mHandler;
    protected SharedPreferences mSharedPreferences;
    protected BaseViewModel viewModel;


    protected abstract Class<? extends BaseViewModel> getViewModelClass();

    protected BaseViewModel getViewModel() {
        return ViewModelProviders.of(this).get(getViewModelClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler();
        overridePendingTransition(0, 0);

        viewModel = getViewModel();
        ActivityBaseBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_base);
        binding.setViewModel(viewModel);

       viewModel.getTitle().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String title) {
                BaseActivity.this.setTitle(title);
            }
        });

       viewModel.getNavigationDrawerId().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer navigationDrawerId) {
                if (navigationDrawerId == null) navigationDrawerId = -1;
                selectNavigationItem(navigationDrawerId);
            }
        });

        contentWrapper = findViewById(R.id.content_wrapper);
        inflater = LayoutInflater.from(contentWrapper.getContext());
    }

    protected final View setContent(@LayoutRes int layout) {
        if (content != null) {
            contentWrapper.removeView(content);
        }
        content = inflater.inflate(layout, contentWrapper, false);
        contentWrapper.addView(content);
        return content;
    }

    protected final FloatingActionButton addFab(@LayoutRes int layout, View.OnClickListener listener) {
        FloatingActionButton fab = (FloatingActionButton) inflater.inflate(layout, contentWrapper, false);
        contentWrapper.addView(fab);
        if (listener != null) {
            fab.setOnClickListener(listener);
        }
        return fab;
    }

    protected final FloatingActionButton addFab(@LayoutRes int layout) {
        return addFab(layout, null);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final int itemId = item.getItemId();

        return goToNavigationItem(itemId);
    }

    protected boolean goToNavigationItem(final int itemId) {
        if (itemId == viewModel.getNavigationDrawerId().getValue()) {
            // just close drawer because we are already in this activity
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        // delay transition so the drawer can close
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callDrawerItem(itemId);
            }
        }, NAVDRAWER_LAUNCH_DELAY);

        mDrawerLayout.closeDrawer(GravityCompat.START);

        selectNavigationItem(itemId);

        // fade out the active activity
        if (contentWrapper != null) {
            contentWrapper.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
        }
        return true;
    }

    // set active navigation item
    private void selectNavigationItem(int itemId) {
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            boolean b = itemId == mNavigationView.getMenu().getItem(i).getItemId();
            mNavigationView.getMenu().getItem(i).setChecked(b);
        }
    }

    /**
     * Enables back navigation for activities that are launched from the NavBar. See
     * {@code AndroidManifest.xml} to find out the parent activity names for each activity.
     *
     * @param intent
     */
    private void createBackStack(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            TaskStackBuilder builder = TaskStackBuilder.create(this);
            builder.addNextIntentWithParentStack(intent);
            builder.startActivities();
        } else {
            startActivity(intent);
            finish();
        }
    }

    /**
     * This method manages the behaviour of the navigation drawer
     * Add your menu items (ids) to res/menu/activity_main_drawer.xml
     *
     * @param itemId Item that has been clicked by the user
     */
    private void callDrawerItem(final int itemId) {
        Intent intent;

        switch (itemId) {
            case R.id.nav_main:
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case R.id.nav_category:
                intent = new Intent(this, CategoriesActivity.class);
                createBackStack(intent);
                break;
            case R.id.nav_tutorial:
                intent = new Intent(this, TutorialActivity.class);
                createBackStack(intent);
                break;
            case R.id.nav_about:
                intent = new Intent(this, AboutActivity.class);
                createBackStack(intent);
                break;
            case R.id.nav_help:
                intent = new Intent(this, HelpActivity.class);
                createBackStack(intent);
                break;
            case R.id.nav_account:
                intent = new Intent(this, AccountsActivity.class);
                createBackStack(intent);
                break;
            default:
                throw new UnsupportedOperationException("Trying to call unkown drawer item! Id: " + itemId);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (getSupportActionBar() == null) {
            setSupportActionBar(toolbar);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.nav_drawer_toggle_open_desc, R.string.nav_drawer_toggle_close_desc);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        if (contentWrapper != null) {
            contentWrapper.setAlpha(0);
            contentWrapper.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }
}