package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    @BindString(R.string.transition_photo) String transitionPhoto;

    @BindView(R.id.photo) ImageView mPhotoView;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.share_fab) ImageButton shareFab;
    @BindView(R.id.photo_container) View mPhotoContainerView;
    @BindView(R.id.nested_scrollview) NestedScrollView mNestedScrollView;

    @BindView(R.id.article_title) TextView titleView;
    @BindView(R.id.article_byline) TextView bylineView;
    @BindView(R.id.article_body) TextView bodyView;

    private View mRootView;
    private Cursor mCursor;
    private long mItemId;
    private int mMutedColor = 0xFF333333;

    private ColorDrawable mStatusBarColorDrawable;

    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*Bundle bundle = this.getArguments();
        if (bundle.containsKey(ARG_ITEM_ID)) {
            mItemId = bundle.getLong(ARG_ITEM_ID);
        }*/
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        ButterKnife.bind(this, mRootView);

        // tky add, Begin --------------
        // Set the 'target'-View for 'shared-element-transition' animation
        //-------------------------------------------------------
        String targetRefViewForSharedElementTransition = transitionPhoto + mItemId;

        ViewCompat.setTransitionName(mPhotoView, targetRefViewForSharedElementTransition);


        if (mToolbar != null) {

            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //----------------------------------------------------------------
                    // tky add, ref: https://developer.android.com/reference/android/support/v4/app/FragmentActivity.html#supportFinishAfterTransition()
                    // Indirect a call to a method via ArticaleDetailActivity/Activity.
                    // supportFinishAfterTransition() -- refers to a method in FragmentActivity.java
                    // Reverses the Activity Scene entry Transition and triggers the calling Activity to reverse its exit Transition.
                    //----------------------------------------------------------------
                    getActivityCast().supportFinishAfterTransition(); // For Content Transition
                    //onSupportNavigateUp();
                }
            });
        }

        if (mNestedScrollView != null){
            mNestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollY > oldScrollY) {
                        //Log.d(TAG, "SCROLL DOWN");
                        shareFab.setVisibility(View.INVISIBLE);
                    }
                    if (scrollY < oldScrollY) {
                        //Log.d(TAG, "SCROLL UP");
                        shareFab.setVisibility(View.INVISIBLE);
                    }
                    if (scrollY == 0) {
                        //Log.d(TAG, "SCROLL TOP");
                        shareFab.setVisibility(View.VISIBLE);
                    }
                    if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                        //Log.d(TAG, "SCROLL BOTTOM");
                        shareFab.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
        // tky add, End --------------

        mStatusBarColorDrawable = new ColorDrawable(0);

        shareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();

        return mRootView;
    }


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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        bylineView.setMovementMethod(new LinkMovementMethod());
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            Date publishedDate = parsePublishedDate();

            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(
                            Html.fromHtml(
                                DateUtils.getRelativeTimeSpanString(
                                    publishedDate.getTime(),
                                    System.currentTimeMillis(),
                                    DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL
                                ).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"
                            )
                        );

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(
                            Html.fromHtml(
                                outputFormat.format(publishedDate)
                                    + " by <font color='#ffffff'>"
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                    + "</font>"
                            )
                        );

            }

            bodyView.setText(
                        Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />"))
                        );

            //****************************************
            ImageLoaderHelper.getInstance(getActivity())
                             .getImageLoader()
                             .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                                  new ImageLoader.ImageListener() {

                                      @Override
                                      public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                                          Bitmap bitmap = imageContainer.getBitmap();

                                          if (bitmap != null) {
                                              Palette p = Palette.generate(bitmap, 12);

                                              //--------------------------------------------------
                                              // tky add comment, android.graphics.color, grayscale: (0xFF333333)
                                              // http://encycolorpedia.com/333333
                                              //--------------------------------------------------
                                              mMutedColor = p.getDarkMutedColor(0xFF333333);

                                              mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                              mRootView.findViewById(R.id.meta_bar)
                                                  .setBackgroundColor(mMutedColor);

                                              //--------------------------------------------
                                              scheduleStartPostponedTransition(mPhotoView);
                                              //--------------------------------------------
                                          }
                                      }

                                      @Override
                                      public void onErrorResponse(VolleyError volleyError) {

                                      }
                                  });
            //****************************************

        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    // tky add, Begin --------------
    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);

                    // ----- Use in shared-element-transition -----
                    getActivity().startPostponedEnterTransition();
                    // --------------------------------------------

                    return true;
                }
            });
    }
    // tky add, End ----------------

    //---------------------------------------------------//
    //---------- Begin: Loader Stuff --------------------//
    //---------------------------------------------------//
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        //Return true if the fragment is currently added to its activity.
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
    //---------------------------------------------------//
    //---------- End: Loader Stuff ----------------------//
    //---------------------------------------------------//

}
