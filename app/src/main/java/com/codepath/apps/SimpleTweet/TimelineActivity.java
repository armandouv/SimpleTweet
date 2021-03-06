package com.codepath.apps.SimpleTweet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.codepath.apps.SimpleTweet.adapters.TweetsAdapter;
import com.codepath.apps.SimpleTweet.databinding.ActivityTimelineBinding;
import com.codepath.apps.SimpleTweet.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;


public class TimelineActivity extends AppCompatActivity {
    private TwitterClient mClient;
    public final static String TAG = "TimelineActivity";
    private final static int REQUEST_CODE = 20;
    private ActivityTimelineBinding mBinding;
    private List<Tweet> mTweets;
    private TweetsAdapter mTweetsAdapter;
    private MenuItem mProgressBarItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityTimelineBinding.inflate(getLayoutInflater());
        View rootView = mBinding.getRoot();
        setContentView(rootView);

        mClient = TwitterApp.getRestClient(this);
        mTweets = new ArrayList<>();
        mTweetsAdapter = new TweetsAdapter(this, mTweets, (view, position) -> {
            Intent intent = new Intent(TimelineActivity.this, TweetDetailsActivity.class);
            Tweet tweet = mTweets.get(position);
            intent.putExtra(Tweet.class.getSimpleName(), Parcels.wrap(tweet));
            startActivity(intent);
        });

        mBinding.logoutButton.setOnClickListener(view -> onLogoutButton());

        mBinding.tweetsView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.tweetsView.setAdapter(mTweetsAdapter);
        populateHomeTimeline();

        this.mClient = TwitterApp.getRestClient(this);

        mBinding.swipeContainer.setOnRefreshListener(() -> fetchTimelineAsync(0));

        mBinding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        populateHomeTimeline();
    }

    private void fetchTimelineAsync(int page) {
        mClient.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONArray jsonArray = json.jsonArray;
                mTweetsAdapter.clear();
                try {
                    mTweetsAdapter.addAll(Tweet.extractFromJsonArray(jsonArray));
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception while refreshing", e);
                }
                mBinding.swipeContainer.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "Refresh onFailure: " + response, throwable);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.compose) {
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra("tweet"));
            mTweets.add(0, tweet);
            mTweetsAdapter.notifyItemInserted(0);
            mBinding.tweetsView.smoothScrollToPosition(0);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.mProgressBarItem = menu.findItem(R.id.item_progress_bar);
        return super.onPrepareOptionsMenu(menu);
    }

    private void populateHomeTimeline() {
        mClient.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                mProgressBarItem.setVisible(false);
                JSONArray jsonArray = json.jsonArray;
                try {
                    List<Tweet> tweets = Tweet.extractFromJsonArray(jsonArray);
                    mTweets.addAll(tweets);
                    mTweetsAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception while populating timeline", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure: " + response, throwable);
            }
        });
    }

    private void onLogoutButton() {
        TwitterApp.getRestClient(this).clearAccessToken();

        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // this makes sure the Back button won't work
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // same as above
        startActivity(i);
    }
}