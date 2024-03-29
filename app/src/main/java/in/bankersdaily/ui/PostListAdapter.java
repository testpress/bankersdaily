package in.bankersdaily.ui;

import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.greenrobot.greendao.query.LazyList;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import in.bankersdaily.R;
import in.bankersdaily.model.Category;
import in.bankersdaily.model.Post;
import in.bankersdaily.util.FormatDate;
import in.bankersdaily.util.SingleTypeAdapter;
import in.bankersdaily.util.ViewUtils;
import in.testpress.core.TestpressSdk;

public class PostListAdapter extends SingleTypeAdapter<Post> {

    private Activity activity;
    private int categoryId;
    private boolean filterBookmarked;
    private QueryBuilder<Post> queryBuilder;
    private LazyList<Post> posts;

    PostListAdapter(Activity activity, int categoryId, boolean filterBookmarked) {
        super(activity, R.layout.post_list_item);
        this.activity = activity;
        this.categoryId = categoryId;
        this.filterBookmarked =filterBookmarked;
        queryBuilder = Post.getPostListQueryBuilder(activity, categoryId, filterBookmarked);
        posts = queryBuilder.listLazy();
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public Post getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    protected int[] getChildViewIds() {
        return new int[] { R.id.title, R.id.date, R.id.category_layout, R.id.category,
                R.id.post_item_layout, R.id.image_view };
    }

    @Override
    public void notifyDataSetChanged() {
        posts = queryBuilder.listLazy();
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = super.getView(position, null, parent);
            textView(0).setTypeface(TestpressSdk.getRubikMediumFont(activity));
            ViewUtils.setTypeface(new TextView[] { textView(1), textView(3) },
                    TestpressSdk.getRubikRegularFont(activity));

            return convertView;
        }
        return super.getView(position, convertView, parent);
    }

    @Override
    protected void update(final int position, final Post post) {

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.placeholder(R.drawable.placeholder_icon);
        requestOptions.error(R.mipmap.ic_launcher);


        Glide.with(activity)
                .setDefaultRequestOptions(requestOptions)
                .load(post.getFeaturedMediaSquare())
                .into(imageView(5));

        setText(0, Html.fromHtml(post.getTitle()));
        setText(1, FormatDate.getAbbreviatedTimeSpan(post.getDate().getTime()));
        if (post.getCategories().isEmpty()) {
            setGone(2, true);
        } else {
            setGone(2, false);
            StringBuilder categoryString = new StringBuilder();
            List<Category> categories = post.getCategories();
            for (int i = 0; i < categories.size();) {
                categoryString.append(categories.get(i).getName());
                if (++i < categories.size()) {
                    categoryString.append(", ");
                }
            }
            setText(3, categoryString);
        }
        view(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, PostDetailPagerActivity.class);
                intent.putExtra(PostDetailPagerActivity.POST_POSITION, position);
                intent.putExtra(PostsListFragment.CATEGORY_ID, categoryId);
                intent.putExtra(PostDetailPagerActivity.FILTER_BOOKMARKED, filterBookmarked);
                activity.startActivity(intent);
            }
        });
    }

}
