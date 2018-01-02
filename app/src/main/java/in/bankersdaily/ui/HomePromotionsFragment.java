package in.bankersdaily.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tobishiba.circularviewpager.library.CircularViewPagerHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.bankersdaily.R;
import in.bankersdaily.model.Post;
import in.bankersdaily.network.ApiClient;
import in.bankersdaily.network.RetrofitCallback;
import in.bankersdaily.network.RetrofitException;
import in.bankersdaily.util.ViewUtils;
import in.testpress.core.TestpressSdk;
import in.testpress.util.UIUtils;

import static in.bankersdaily.ui.PostListActivity.CATEGORY_SLUG;

public class HomePromotionsFragment extends Fragment {

    private static final int CURRENT_AFFAIRS_ID = 5;
    public static final int DAILY_QUIZ_ID = 19;
    public static final int NOTIFICATIONS_ID = 32;
    public static final int HOME_SLIDER_PREVIEW_PADDING = 50;

    @BindView(R.id.promotions_pager) ViewPager promotionsPager;
    @BindView(R.id.promotions_view) RelativeLayout promotionsView;
    @BindView(R.id.content) LinearLayout content;
    @BindView(R.id.current_affairs_list) ListView currentAffairsListView;
    @BindView(R.id.daily_quiz_list) ListView dailyQuizListView;
    @BindView(R.id.notifications_list) ListView notificationListView;
    @BindView(R.id.current_affairs_card_view) CardView currentAffairsCardView;
    @BindView(R.id.daily_quiz_card_view) CardView notificationCardView;
    @BindView(R.id.notifications_card_view) CardView dailyQuizCardView;
    @BindView(R.id.more_current_affairs_text) TextView moreCurrentAffairsText;
    @BindView(R.id.more_notifications_text) TextView moreNotificationText;
    @BindView(R.id.more_daily_quiz_text) TextView moreDailyQuizText;
    @BindView(R.id.empty_container) LinearLayout emptyView;
    @BindView(R.id.retry_button) Button retryButton;
    @BindView(R.id.scroll_view) ScrollView scrollView;
    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeRefresh;

    List<Post> promotions = new ArrayList<>();
    List<Post> currentAffairs = Collections.emptyList();
    List<Post> dailyQuiz = Collections.emptyList();
    List<Post> notifications = Collections.emptyList();
    PromotionPagerAdapter promotionPagerAdapter;
    int noOfItemsLoaded;
    ApiClient apiClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_promotions_home, container, false);
        ButterKnife.bind(this, view);
        ViewUtils.setTypeface(
                new TextView[] { moreCurrentAffairsText, moreDailyQuizText, moreNotificationText },
                TestpressSdk.getRubikRegularFont(getActivity())
        );
        noOfItemsLoaded = 0;
        promotionPagerAdapter = new PromotionPagerAdapter(getFragmentManager(), promotions);
        promotionsPager.setAdapter(promotionPagerAdapter);
        setPromotionsHeight();
        scrollView.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        int scrollY = scrollView.getScrollY(); // For vertical scroll view
                        if (scrollY == 0 && scrollView.isEnabled()) {
                            swipeRefresh.setEnabled(true);
                        } else {
                            swipeRefresh.setEnabled(false);
                        }
                    }
                });
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                swipeRefresh.setRefreshing(true);
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        apiClient = new ApiClient(getActivity());
        loadCurrentAffairs();
        loadDailyQuiz();
        loadNotifications();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setPromotionsHeight();
    }

    public void loadCurrentAffairs() {
        Post.loadLatest(apiClient, CURRENT_AFFAIRS_ID, new RetrofitCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                currentAffairs = posts;
                promotions.add(posts.get(0));
                PostSearchListAdapter adapter = new PostSearchListAdapter(getActivity());
                adapter.setItems(posts);
                currentAffairsListView.setAdapter(adapter);
                hideProgress();
            }

            @Override
            public void onException(RetrofitException exception) {
                content.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                promotionsView.setVisibility(View.GONE);
                if (exception.isNetworkError()) {
                    retryButton.setVisibility(View.VISIBLE);
                }
                hideProgress();
            }
        });
    }

    public void loadDailyQuiz() {
        Post.loadLatest(apiClient, DAILY_QUIZ_ID, new RetrofitCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                dailyQuiz = posts;
                promotions.add(posts.get(0));
                PostSearchListAdapter adapter = new PostSearchListAdapter(getActivity());
                adapter.setItems(posts);
                dailyQuizListView.setAdapter(adapter);
                hideProgress();
            }

            @Override
            public void onException(RetrofitException exception) {
                content.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                promotionsView.setVisibility(View.GONE);
                if (exception.isNetworkError()) {
                    retryButton.setVisibility(View.VISIBLE);
                }
                hideProgress();
            }
        });
    }

    public void loadNotifications() {
        Post.loadLatest(apiClient, NOTIFICATIONS_ID, new RetrofitCallback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                notifications = new ArrayList<>(posts);
                promotions.add(posts.get(0));
                PostSearchListAdapter adapter = new PostSearchListAdapter(getActivity());
                adapter.setItems(posts);
                notificationListView.setAdapter(adapter);
                hideProgress();
            }

            @Override
            public void onException(RetrofitException exception) {
                content.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                promotionsView.setVisibility(View.GONE);
                if (exception.isNetworkError()) {
                    retryButton.setVisibility(View.VISIBLE);
                }
                hideProgress();
            }
        });
    }

    @OnClick(R.id.more_current_affairs) void showCurrentAffairs() {
        Intent intent = new Intent(getActivity(), PostListActivity.class);
        intent.putExtra(CATEGORY_SLUG, "current-affairs");
        startActivity(intent);
    }

    @OnClick(R.id.more_daily_quiz) void showDailyQuiz() {
        Intent intent = new Intent(getActivity(), PostListActivity.class);
        intent.putExtra(CATEGORY_SLUG, "study-materials");
        startActivity(intent);
    }

    @OnClick(R.id.more_notifications) void showNotifications() {
        Intent intent = new Intent(getActivity(), PostListActivity.class);
        intent.putExtra(CATEGORY_SLUG, "notifications");
        startActivity(intent);
    }

    @OnClick(R.id.retry_button) void refresh() {
        swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                swipeRefresh.setRefreshing(true);
            }
        });
        noOfItemsLoaded = 0;
        loadCurrentAffairs();
        loadDailyQuiz();
        loadNotifications();
        content.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        currentAffairsCardView.setVisibility(View.GONE);
        dailyQuizCardView.setVisibility(View.GONE);
        notificationCardView.setVisibility(View.GONE);
    }

    void displayPromotions() {
        promotionsView.setVisibility(View.VISIBLE);
        ViewUtils.slide_down(getActivity(), promotionsView);
        promotionPagerAdapter.setItems(promotions);
        promotionPagerAdapter.notifyDataSetChanged();
        promotionsPager.addOnPageChangeListener(new CircularViewPagerHandler(promotionsPager) {
            @Override
            public void onPageScrollStateChanged(int state) {
                swipeRefresh.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
                scrollView.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
                super.onPageScrollStateChanged(state);
            }
        });
        promotionsPager.setClipToPadding(false);
        promotionsPager.setPadding(HOME_SLIDER_PREVIEW_PADDING, 0, HOME_SLIDER_PREVIEW_PADDING, 0);
        promotionsView.setVisibility(View.VISIBLE);
    }

    void setPromotionsHeight() {
        //noinspection ConstantConditions

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) promotionsPager.getLayoutParams();

        params.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
        int heightOfTitle = (int) UIUtils.getPixelFromDp(getActivity(), 80);
        Log.e("heightOfTitle", ""+heightOfTitle);
        params.height = ViewUtils.getImageHeightWithRespectToDevice(getActivity()) + heightOfTitle;
        promotionsPager.setLayoutParams(params);
    }

    void hideProgress() {
        int noOfItems = 3;
        if(++noOfItemsLoaded == noOfItems) {
            displayPromotions();
            displayCardView(currentAffairsCardView, currentAffairsListView);
            displayCardView(dailyQuizCardView, dailyQuizListView);
            displayCardView(notificationCardView, notificationListView);
            swipeRefresh.setRefreshing(false);
        }
    }

    void displayCardView(CardView cardView, ListView listView) {
        cardView.setVisibility(View.VISIBLE);
        ViewUtils.slide_down(getActivity(), cardView);
        ViewUtils.setListViewHeightBasedOnItems(listView);
    }

}