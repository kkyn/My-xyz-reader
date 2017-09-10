package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity /*ActionBarActivity*/
                    implements
                        LoaderManager.LoaderCallbacks<Cursor>
                        ,SwipeRefreshLayout.OnRefreshListener
{

    private static final String TAG = ArticleListActivity.class.toString();

    // Use Butterknife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;

    @BindString(R.string.transition_photo) String transitionPhoto;

    // https://developer.android.com/reference/java/text/SimpleDateFormat.html
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        ButterKnife.bind(this);

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.cyan_a400);
        mSwipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    //----------------------------------------------------------------------
    // Method for interface/callback SwipeRefreshLayout.OnRefreshListener.
    // Called when a swipe gesture triggers a refresh.
    //----------------------------------------------------------------------
    @Override
    public void onRefresh() {
        refresh();
    }

    private void refresh() {

        //-------------------------------------------------
        // Request that a given application service be started.
        // https://developer.android.com/reference/android/content/Context.html#startService(android.content.Intent)
        // https://developer.android.com/reference/android/app/IntentService.html
        // 'Call' MyIntentService/IntentService to start a service (startService).
        // MyIntentService/IntentService is a base class for Services that
        // handle asynchronous requests (expressed as Intents) on demand.
        // Clients send requests through startService(Intent) calls
        //--- Enable/Start a backgound thread to perform a service, which is defined in MyIntentService
        //-------------------------------------------------

        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        //-------------------------------------------------
        // https://developer.android.com/reference/android/content/Context.html
        // Dynamically register an instance of this class (BroadcastReceiver).
        // Register a BroadcastReceiver to be run in the main activity thread.
        // The receiver will be called with any broadcast Intent that matches filter,
        // in the main application thread.
        //--- Enable 'hearing' to receive a specific-broadcast(defined by a filtered signature and value).
        //-------------------------------------------------
        registerReceiver(
                mRefreshingReceiver,    //-- receiver
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE)  //-- filter
        );
        //-------------------------------------------------
        //getApplication().registerReceiver(...);   // equivalent call statement
        //this.registerReceiver(...);   // equivalent call statement
        //-------------------------------------------------
    }

    @Override
    protected void onStop() {
        super.onStop();

        //-------------------------------------------------
        // Unregister a previously registered BroadcastReceiver.
        // All filters ,e.g. UpdaterService.BROADCAST_ACTION_STATE_CHANGE,
        //  that have been registered for this BroadcastReceiver will be removed.
        //--- Disable 'hearing' of receiving a specific-broadcast, signature and value.
        //-------------------------------------------------

        unregisterReceiver(mRefreshingReceiver);
        //-------------------------------------------------
        //getApplication().unregisterReceiver(mBroadcastReceiver); // equivalent statement
        //this.unregisterReceiver(mBroadcastReceiver);   // equivalent statement
        //-------------------------------------------------
    }

    //-----------------------------------------------------------
    //------------- Begin: Swipe to Refresh ---------------------
    //-----------------------------------------------------------
    private boolean mIsRefreshing = false;

    // Instantiate a BroadcastReceiver.
    // When this BroadcastReceiver 'receives/hear' an 'Intent-Broadcast',
    // onReceive-method is called to check/filter the 'Intent-Broadcast'-ACTIONTYPE and
    // its accompanying data/value.
    // In this case, Broadcaster/Sender is from the UpdaterService/IntentService.
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {

        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }
    //-----------------------------------------------------------
    //--------------- End  : Swipe to Refresh -------------------
    //-----------------------------------------------------------

    //---------------------------------------------------------------------//
    //---------- Begin: LoaderManager.LoaderCallbacks<Cursor> Stuff -------//
    //---------------------------------------------------------------------//
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }
    //---------------------------------------------------------------------//
    //---------- End: LoaderManager.LoaderCallbacks<Cursor> Stuff ---------//
    //---------------------------------------------------------------------//
    //---------------------------------------------------------------------//
    //---------- Begin: Adapter for RecyclerView Stuff --------------------//
    //---------------------------------------------------------------------//
    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);

            final ViewHolder vh = new ViewHolder(view);

            return vh;
        }

        // https://developer.android.com/reference/java/text/DateFormat.html#parse(java.lang.String)
        // https://developer.android.com/reference/java/text/DateFormat.html
        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder( final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);

            // tky add, Begin, 9Sept.2017 --------------
            // tky add ----------------------------------------------
            // Set the 'source'-View for 'shared-element-transition' animation
            //-------------------------------------------------------
            long photoId = getItemId(holder.getAdapterPosition());
            String srcRefViewForSharedElementTransition = transitionPhoto + photoId;

            String stringUrl = mCursor.getString(ArticleLoader.Query.THUMB_URL);
            Float floatAspectRatio = mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);

            //***********************
            ImageLoader imageLoader =
                ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader();

            //***********************
            // Get Image-info from ImageLoader-object,
            // with the given image's-Url-id and interface 'pointer'/ (callback).
            ImageLoader.ImageContainer myImageContainer =
                imageLoader.get(stringUrl, new ImageLoader.ImageListener() {

                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {

                        Bitmap bitmap = imageContainer.getBitmap();

                        if (bitmap != null) {
                            colorTheTitleBar(bitmap, holder);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                    }
                });


            //***********************
            //----------------------------------------------
            // -- thumb-nail-View --
            // .setImageUrl -- define in ImageView
            holder.thumbnailDynamicHeightNetworkImageView.setImageUrl(stringUrl,imageLoader);
            holder.thumbnailDynamicHeightNetworkImageView.setAspectRatio(floatAspectRatio);
            holder.thumbnailDynamicHeightNetworkImageView.setTransitionName(srcRefViewForSharedElementTransition);

            //----------------------------------------------
            final Pair<View, String> pair1 =
                        new Pair<>((View)holder.thumbnailDynamicHeightNetworkImageView,
                                         holder.thumbnailDynamicHeightNetworkImageView.getTransitionName());
            //final Pair<View, String> pair2 = new Pair<>((View)holder.barLayout, holder.barLayout.getTransitionName());

            // note !! : tky add, 'itemView' is defined in RecyclerView class
            // define 'setOnClickListener' here works!!
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //----------------- Original, Starter Code Version ---------------------
                    /*startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())))
                    );*/
                    //------------------------------------------------
                    //------------------------------------------------
                    // note: tky add, Enable 'activity content transition' ....
                    /*ActivityOptions option = ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this);
                    Bundle bundle = option.toBundle();
                    startActivity(new Intent(Intent.ACTION_VIEW,
                                    ItemsContract.Items.buildItemUri(getItemId(holder.getAdapterPosition())))

                                    , bundle
                    );*/
                    // note: tky add, Implementing 'Shared Element Transition' ....
                    ActivityOptions option =
                        ActivityOptions.makeSceneTransitionAnimation
                            (
                                ArticleListActivity.this
                                ,pair1
                              //,pair2

                              /*holder.thmbnl_niview,
                                holder.thmbnl_niview.getTransitionName()*/
                            );

                    Bundle bundle = option.toBundle();

                    Uri myUri = ItemsContract.Items.buildItemUri(getItemId(holder.getAdapterPosition()));

                    startActivity(new Intent(Intent.ACTION_VIEW, myUri), bundle);
                    //------------------------------------------------
                }
            });
            // tky add, End, 9Sept.2017 --------------

            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            Date publishedDate = parsePublishedDate();

            // https://developer.android.com/reference/java/util/Date.html#before(java.util.Date)
            // https://developer.android.com/reference/java/util/Calendar.html#getTime()
            // https://developer.android.com/reference/java/util/GregorianCalendar.html
            // Calendar -> GregorianCalendar, getTime() is declared in Calendar.
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                // https://developer.android.com/reference/android/text/Html.html#fromHtml(java.lang.String)
                // https://developer.android.com/reference/android/text/format/DateUtils.html
                // https://developer.android.com/reference/android/text/format/DateUtils.html#getRelativeTimeSpanString(long, long, long, int)
                holder.subtitleView.setText(
                            Html.fromHtml(
                                DateUtils.getRelativeTimeSpanString(
                                    publishedDate.getTime(),
                                    System.currentTimeMillis(),
                                    DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL
                                ).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            )
                        );
            } else {
                holder.subtitleView.setText(
                            Html.fromHtml(
                                outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            )
                        );
            }

        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        ////////////////////////////////////////////////////////////
        // ref: https://developer.android.com/reference/android/support/v7/graphics/Palette.html
        private void colorTheTitleBar(Bitmap bitmap, final ViewHolder holder) {

            // Start generating a Palette with the returned Palette.Builder instance.
            Palette.Builder mPaletteBuilder = Palette.from(bitmap);

            // Clear all added filters. ???
            mPaletteBuilder.clearFilters();

            // Set the maximum number of colors to use in the quantization step
            // when using a Bitmap as the source.
            mPaletteBuilder = mPaletteBuilder.maximumColorCount(14);

            // Generate the Palette asynchronously.
            AsyncTask<Bitmap, Void, Palette> mPalette = mPaletteBuilder.generate(

                new Palette.PaletteAsyncListener() {

                    @Override
                    public void onGenerated(Palette palette) {

                        // Get the "darkVibrant" color swatch based in the input bitmap.
                        Palette.Swatch muted = palette.getMutedSwatch();

                        if (muted != null) {
                            holder.barLayout.setBackgroundColor(muted.getRgb());
                            holder.titleView.setTextColor(muted.getTitleTextColor());
                            holder.subtitleView.setTextColor(muted.getTitleTextColor());
                        }
                    }

                }
            );
        }
    }

    //-------------------------------------------------------------------//
    //---------- End: Adapter for RecyclerView Stuff --------------------//
    //-------------------------------------------------------------------//

    //-------------------------------------------------//
    //------ Begin : ViewHolder For RecyclerView ------//
    //-------------------------------------------------//
    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.thumbnail)  DynamicHeightNetworkImageView thumbnailDynamicHeightNetworkImageView;
        @BindView(R.id.article_title) TextView titleView;
        @BindView(R.id.article_subtitle) TextView subtitleView;
        @BindView(R.id.bar_) LinearLayout barLayout;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
    //-------------------------------------------------//
    //------ End   : ViewHolder For RecyclerView ------//
    //-------------------------------------------------//
}
