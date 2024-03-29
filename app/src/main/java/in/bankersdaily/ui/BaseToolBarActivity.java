package in.bankersdaily.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static android.view.WindowManager.LayoutParams.FLAG_SECURE;

import in.bankersdaily.BankersDailyApp;
import in.bankersdaily.R;

import static in.testpress.core.TestpressSdk.ACTION_PRESSED_HOME;

/**
 * Base activity used to support the toolbar & handle backpress.
 * Activity that extends this activity must needs to include the #layout/testpress_toolbar
 * in its view.
 */
public abstract class BaseToolBarActivity extends AppCompatActivity {

    public static final String ACTIONBAR_TITLE = "title";

    @Override
    protected void onResume() {
        super.onResume();
        trackScreenViewAnalytics();
    }

    @Override
    public void setContentView(final int layoutResId) {
        getWindow().setFlags(FLAG_SECURE, FLAG_SECURE);
        super.setContentView(layoutResId);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressWarnings("DanglingJavadoc")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            /**
             * Set result with home button pressed flag if activity is started by
             * {@link #startActivityForResult}
             *
             * Note: {@link #getCallingActivity} return null if activity is started using
             * {@link #startActivity} instead of {@link #startActivityForResult}
             */
            if (getCallingActivity() == null) {
                onBackPressed();
            } else {
                setResult(RESULT_CANCELED, new Intent().putExtras(getDataToSetResult()));
                super.onBackPressed();
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("DanglingJavadoc")
    @Override
    public void onBackPressed() {
        /**
         * Set result with home button pressed flag false if activity is started by
         * {@link #startActivityForResult}
         */
        if (getCallingActivity() != null) {
            setResult(RESULT_CANCELED, new Intent().putExtra(ACTION_PRESSED_HOME, false));
        }
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            supportFinishAfterTransition();
        }
    }

    protected Bundle getDataToSetResult() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ACTION_PRESSED_HOME, true);
        return bundle;
    }

    protected abstract String getScreenName();

    protected void trackScreenViewAnalytics() {
        BankersDailyApp.getInstance().trackScreenView(this, getScreenName());
    }

}
