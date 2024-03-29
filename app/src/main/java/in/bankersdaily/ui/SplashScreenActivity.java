package in.bankersdaily.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

//import com.crashlytics.android.Crashlytics;
//import com.crashlytics.android.core.CrashlyticsCore;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.bankersdaily.BuildConfig;
import in.bankersdaily.R;
import in.bankersdaily.util.Assert;
import in.testpress.core.TestpressSdk;
import in.testpress.core.TestpressSession;
import in.testpress.course.TestpressCourse;
import in.testpress.exam.TestpressExam;
import in.testpress.store.TestpressStore;
//import io.fabric.sdk.android.Fabric;

import static in.bankersdaily.ui.LoginActivity.AUTHENTICATE_REQUEST_CODE;
import static in.bankersdaily.ui.PostDetailFragment.POST_SLUG;
import static in.bankersdaily.ui.PostListActivity.CATEGORY_SLUG;
import static in.testpress.core.TestpressSdk.ACTION_PRESSED_HOME;
import static in.testpress.store.TestpressStore.CONTINUE_PURCHASE;

public class SplashScreenActivity extends Activity {

    // Splash screen timer
    private static final int SPLASH_TIME_OUT = 500;

    public static final int DEEPLINK_REQUEST_CODE = 4444;

    @BindView(R.id.splash_image) ImageView splashImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
//        Crashlytics crashlytics = new Crashlytics.Builder()
//                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
//                .build();

//        Fabric.with(this, crashlytics);
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Uri uri = getIntent().getData();
                if (uri != null && uri.getPathSegments().size() > 0) {
                    List<String> pathSegments = uri.getPathSegments();
                    switch (pathSegments.get(0)) {
                        case "exams":
                            if (pathSegments.size() == 2 || pathSegments.size() == 4) {
                                // If exam slug is present, directly goto the start exam screen
                                authenticateUser(uri);
                            } else {
                                gotoHome();
                            }
                            break;
                        case "products":
                            authenticateUser(uri);
                            break;
                        case "category":
                            if (pathSegments.size() > 1) {
                                // If category slug is present, display posts of that category
                                Intent intent = new Intent(SplashScreenActivity.this,
                                        PostListActivity.class);

                                intent.putExtra(CATEGORY_SLUG, uri.getLastPathSegment());
                                startActivityForResult(intent, DEEPLINK_REQUEST_CODE);
                            } else {
                                gotoHome();
                            }
                            break;
                        default:
                            if (pathSegments.size() == 1) {
                                Intent intent = new Intent(SplashScreenActivity.this,
                                        PostDetailActivity.class);

                                intent.putExtra(POST_SLUG, pathSegments.get(0));
                                startActivityForResult(intent, DEEPLINK_REQUEST_CODE);
                            } else {
                                gotoHome();
                            }
                            break;
                    }
                } else {
                    // This method will be executed once the timer is over
                    // Start app main activity
                    gotoHome();
                }
            }
        }, SPLASH_TIME_OUT);
    }

    private void gotoHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void authenticateUser(final Uri uri) {
        final Activity activity = SplashScreenActivity.this;
        final List<String> pathSegments = uri.getPathSegments();
        if (TestpressSdk.hasActiveSession(this)) {
            TestpressSession testpressSession = TestpressSdk.getTestpressSession(activity);
            Assert.assertNotNull("TestpressSession must not be null.", testpressSession);
            switch (pathSegments.get(0)) {
                case "exams":
                    String slug;
                    if (pathSegments.size() == 2) {
                        slug = pathSegments.get(1);
                    } else if (pathSegments.size() == 4) {
                        slug = pathSegments.get(2);
                    } else {
                        gotoHome();
                        break;
                    }
                    // If exam slug is present, directly goto the start exam screen
                    TestpressExam.showExamAttemptedState(
                            activity,
                            slug,
                            testpressSession
                    );
                    break;
                case "products":
                    if (pathSegments.size() == 2) {
                        slug = pathSegments.get(1);
                        TestpressStore.showProduct(activity, slug, testpressSession);
                    } else {
                        TestpressStore.show(activity, testpressSession);
                    }
                    break;
                default:
                    gotoHome();
                    break;
            }
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, AUTHENTICATE_REQUEST_CODE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        splashImage.setImageResource(R.drawable.splash_screen);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Result code OK will come if attempted an exam & back press or user purchased a product
            switch (requestCode) {
                case AUTHENTICATE_REQUEST_CODE:
                    Uri uri = getIntent().getData();
                    authenticateUser(uri);
                    break;
                case TestpressStore.STORE_REQUEST_CODE:
                    TestpressSession session = TestpressSdk.getTestpressSession(this);
                    Assert.assertNotNull("TestpressSession must not be null", session);
                    if (data != null && data.getBooleanExtra(CONTINUE_PURCHASE, false)) {
                        TestpressStore.show(this, session);
                    } else {
                        TestpressCourse.show(this, session);
                    }
                    break;
                default:
                    gotoHome();
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (data != null && data.getBooleanExtra(ACTION_PRESSED_HOME, false)) {
                // Go to home if user pressed home button
                gotoHome();
            } else {
                finish();
            }
        }
    }

}