package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utils.ImageLoaderHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = ArticleListActivity.class.toString();
    private static final int XYZ_ARTICLE_LOADER = 222;
    public static final String MAIN_LIST_STATE_KEY = "main_list_state_key";
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private BroadcastReceiver mRefreshingReceiver;
    private StaggeredGridLayoutManager mGridLayoutManager;
    private Parcelable mMainListState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setAdapter(null);

        if (savedInstanceState == null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                    onRefresh();
                }
            });
        } else {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                    loadData();
                }
            });
        }

        mRefreshingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterService.BROADCAST_ACTION_IS_ONLINE.equals(intent.getAction())) {

                    boolean serviceNotOnline = intent.getBooleanExtra(UpdaterService.EXTRA_NOT_ONLINE, false);
                    if (serviceNotOnline) {
                        /* Device is offline so use existing data*/
                        Toast.makeText(ArticleListActivity.this, "Not online", Toast.LENGTH_SHORT).show();
                        loadData();
                    }
                }

                if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {

                    boolean serviceRefreshStatus = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);

                    if (!mIsRefreshing && !serviceRefreshStatus)
                        return;

                    if (!mIsRefreshing && serviceRefreshStatus) {
                        mIsRefreshing = true;
                        return;
                    }
                    if (mIsRefreshing && !serviceRefreshStatus) {
                        mIsRefreshing = false;
                        loadData();
                    }

                }
            }
        };
    }

    private void loadData() {

        if (getSupportLoaderManager().getLoader(XYZ_ARTICLE_LOADER) == null) {
            getSupportLoaderManager().initLoader(XYZ_ARTICLE_LOADER, null, ArticleListActivity.this);
        } else {
            getSupportLoaderManager().restartLoader(XYZ_ARTICLE_LOADER, null, ArticleListActivity.this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));

        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_IS_ONLINE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);

        mGridLayoutManager = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        if (mMainListState != null) {
            mGridLayoutManager.onRestoreInstanceState(mMainListState);
        }

        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onRefresh() {
        startService(new Intent(getApplicationContext(), UpdaterService.class));
    }

    protected void onSaveInstanceState(Bundle state) {
        Log.d(TAG, "onSaveInstanceState: ");
        super.onSaveInstanceState(state);

        if (mGridLayoutManager != null) {
            mMainListState = mGridLayoutManager.onSaveInstanceState();
            state.putParcelable(MAIN_LIST_STATE_KEY, mMainListState);
        }
    }

    protected void onRestoreInstanceState(Bundle state) {
        Log.d(TAG, "onRestoreInstanceState: ");
        super.onRestoreInstanceState(state);

        // Retrieve list state and list/item positions
        if (state != null)
            mMainListState = state.getParcelable(MAIN_LIST_STATE_KEY);
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            this.mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {

            if (mCursor != null) {
                mCursor.moveToPosition(position);
                return mCursor.getLong(ArticleLoader.Query._ID);
            } else
                return 0;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startTransition(vh);
                }
            });
            return vh;
        }

        private void startTransition(ViewHolder viewHolder) {
            Bundle b = ActivityOptions
                    .makeSceneTransitionAnimation(ArticleListActivity.this
                            , viewHolder.thumbnailView
                            , viewHolder.thumbnailView.getTransitionName())
                    .toBundle();
            Intent i = new Intent(Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition())));
            startActivity(i, b);
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, "parsePublishedDate: " + ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            if (mCursor == null) return;

            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            if (mCursor != null) {
                return mCursor.getCount();
            } else {
                return 0;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
