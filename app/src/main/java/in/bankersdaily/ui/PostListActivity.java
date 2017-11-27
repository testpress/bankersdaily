package in.bankersdaily.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import junit.framework.Assert;

import org.greenrobot.greendao.query.LazyList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.bankersdaily.BankersDailyApp;
import in.bankersdaily.R;
import in.bankersdaily.model.Category;
import in.bankersdaily.model.CategoryDao;
import in.bankersdaily.network.ApiClient;
import in.bankersdaily.network.RetrofitCallback;
import in.bankersdaily.network.RetrofitException;

import static in.bankersdaily.network.ApiClient.SLUG;
import static in.bankersdaily.ui.PostsListFragment.CATEGORY_ID;

public class PostListActivity extends BaseToolBarActivity {

    public static final String CATEGORY_SLUG = "categorySlug";

    @BindView(R.id.pb_loading) ProgressBar progressBar;
    @BindView(R.id.empty_container) LinearLayout emptyView;
    @BindView(R.id.empty_title) TextView emptyTitleView;
    @BindView(R.id.empty_description) TextView emptyDescView;
    @BindView(R.id.image_view) ImageView emptyImageView;
    @BindView(R.id.retry_button) Button retryButton;

    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);
        ButterKnife.bind(this);
        if (getIntent().getStringExtra(CATEGORY_SLUG) != null) {
            loadCategory(getIntent().getStringExtra(CATEGORY_SLUG));
        } else {
            title = getIntent().getStringExtra(ACTIONBAR_TITLE);
            displayPostsList();
        }
    }

    void displayPostsList() {
        Assert.assertNotNull("ACTIONBAR_TITLE must not be null.", title);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
        PostsListFragment fragment = new PostsListFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    void loadCategory(final String categorySlug) {
        progressBar.setVisibility(View.VISIBLE);
        Map<String, Object> queryParams = new LinkedHashMap<String, Object>();
        queryParams.put(SLUG, categorySlug);
        queryParams.put(ApiClient.EMBED, "1");
        new ApiClient(this).getCategories(queryParams)
                .enqueue(new RetrofitCallback<List<Category>>() {
                    @Override
                    public void onSuccess(List<Category> categories) {
                        if (!categories.isEmpty()) {
                            Category category = categories.get(0);
                            CategoryDao categoryDao =
                                    BankersDailyApp.getDaoSession(PostListActivity.this)
                                            .getCategoryDao();

                            LazyList<Category> categoriesInDB = categoryDao.queryBuilder()
                                    .where(CategoryDao.Properties.Id.eq(category.getId())).listLazy();

                            if (categoriesInDB.isEmpty()) {
                                categoryDao.insertOrReplaceInTx(category);
                            }
                            title = category.getName();
                            getIntent().putExtra(CATEGORY_ID, category.getId().intValue());
                            progressBar.setVisibility(View.GONE);
                            displayPostsList();
                        } else {
                            setEmptyText(
                                    R.string.category_not_available,
                                    R.string.category_not_available_description,
                                    R.drawable.alert_warning
                            );
                        }
                    }

                    @Override
                    public void onException(RetrofitException exception) {
                        if (exception.isUnauthenticated()) {
                            setEmptyText(R.string.authentication_failed, R.string.please_login,
                                    R.drawable.alert_warning);
                        } else if (exception.isNetworkError()) {
                            setEmptyText(R.string.network_error, R.string.no_internet_try_again,
                                    R.drawable.no_wifi);

                            retryButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    progressBar.setVisibility(View.VISIBLE);
                                    emptyView.setVisibility(View.GONE);
                                    loadCategory(categorySlug);
                                }
                            });
                            retryButton.setVisibility(View.VISIBLE);
                        } else  {
                            setEmptyText(
                                    R.string.loading_failed,
                                    R.string.some_thing_went_wrong_try_again,
                                    R.drawable.alert_warning
                            );
                        }
                    }
                });
    }

    protected void setEmptyText(final int title, final int description, final int imageResId) {
            emptyView.setVisibility(View.VISIBLE);
            emptyTitleView.setText(title);
            emptyDescView.setText(description);
            emptyImageView.setImageResource(imageResId);
            retryButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
    }

}
