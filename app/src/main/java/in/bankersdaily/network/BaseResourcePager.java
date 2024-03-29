package in.bankersdaily.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import in.bankersdaily.util.Assert;
import retrofit2.Response;

/**
 * Generic resource pager for elements with an id that can be paged
 *
 * @param <E>
 */
public abstract class BaseResourcePager<E> {

    private static final String TOTAL_ITEMS_HEADER = "X-WP-Total";
    private static final String TOTAL_PAGES_HEADER = "X-WP-TotalPages";
    private static final String PAGE = "page";
    private static final String PER_PAGE = "per_page";

    protected Response<List<E>> response;

    /**
     * Number of items per page
     */
    protected int itemsPerPage = 20;

    /**
     * Next page to request
     */
    protected int page = 1;

    /**
     * Number of pages to request
     */
    protected int count = 1;

    /**
     * All resources retrieved
     */
    protected final Map<Object, E> resources = new LinkedHashMap<Object, E>();

    /**
     * Query Params to be passed
     */
    public Map<String, Object> queryParams = new LinkedHashMap<String, Object>();

    /**
     * Are more pages available?
     */
    protected boolean hasMore;

    /**
     * Reset the number of the next page to be requested from {@link #next()}
     * and clear all stored state
     *
     * @return this pager
     */
    public BaseResourcePager<E> reset() {
        page = 1;
        return clear();
    }

    /**
     * Clear all stored resources and have the next call to {@link #next()} load
     * all previously loaded pages using count variable
     *
     * @return this pager
     */
    public BaseResourcePager<E> clear() {
        count = Math.max(1, page - 1);
        page = 1;
        resources.clear();
        response = null;
        hasMore = true;
        clearQueryParams();
        return this;
    }

    public BaseResourcePager<E> clearResources() {
        resources.clear();
        return this;
    }

    /**
     * Get number of resources loaded into this pager
     *
     * @return number of resources
     */
    public int size() {
        return resources.size();
    }

    /**
     * Get resources
     *
     * @return resources
     */
    public List<E> getResources() {
        return new ArrayList<E>(resources.values());
    }

    /**
     * Get the next page of resources
     *
     * @return true if more pages
     * @throws RetrofitException
     */
    public boolean next() throws RetrofitException {
        boolean emptyPage = false;
        try {
            for (int i = 0; i < count && hasNext(); i++) {
                queryParams.put(PAGE, page);
                queryParams.put(PER_PAGE, itemsPerPage);
                Response<List<E>> response = getItems(page, itemsPerPage);
                List<E> items;
                if (response.isSuccessful()) {
                    this.response = response;
                    items = response.body();
                } else {
                    hasMore = false;
                    throw RetrofitException.httpError(response);
                }
                Assert.assertNotNull("Items must not be null", items);
                emptyPage = items.isEmpty();
                if (emptyPage)
                    break;
                for (E item : items) {
                    item = register(item);
                    if (item == null)
                        continue;
                    resources.put(getId(item), item);
                }
                page++;
            }
            // Set count value to 1 if first load request made after call clear()
            if (count > 1) {
                count = 1;
            }

        } catch (Exception e) {
            hasMore = false;
            if (e instanceof IOException) {
                throw RetrofitException.networkError((IOException) e);
            } else {
                throw RetrofitException.unexpectedError(e);
            }
        }
        hasMore = hasNext() && !emptyPage;
        return hasMore;
    }

    public boolean hasNext() {
        return response == null ||
                Integer.parseInt(response.headers().get(TOTAL_PAGES_HEADER)) >= page;
    }

    /**
     * Set hasMore
     *
     * @param hasMore Are more pages available to request
     */
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * Are more pages available to request?
     *
     * @return true if the last call to {@link #next()} returned true, false
     *         otherwise
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * Callback to register a fetched resource before it is stored in this pager
     * <p>
     * Sub-classes may override
     *
     * @param resource
     * @return resource
     */
    protected E register(final E resource) {
        return resource;
    }

    public int getTotalCount() {
        return Integer.parseInt(response.headers().get(TOTAL_ITEMS_HEADER));
    }

    /**
     * Get id for resource
     *
     * @param resource
     * @return id
     */
    protected abstract Object getId(E resource);

    /**
     * Create iterator to return given page and size
     *
     * @param page
     * @param size
     * @return iterator
     */
    public abstract Response<List<E>> getItems(final int page, final int size)
            throws IOException;

    public Object getQueryParams(String key) {
        return queryParams.get(key);
    }

    public void setQueryParams(String key, Object value) {
        queryParams.put(key, value);
    }

    public void removeQueryParams(String key) {
        queryParams.remove(key);
    }

    public void clearQueryParams() {
        queryParams.clear();
    }
}

