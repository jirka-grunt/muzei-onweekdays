/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.ulmus.muzei.onweekdays;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import static cz.ulmus.muzei.onweekdays.ArtService.Photo;
import static cz.ulmus.muzei.onweekdays.ArtService.PhotoResponse;

public class ArtSource extends RemoteMuzeiArtSource {
	private static final String SOURCE_NAME = "OnWeekdaysArtSource";

	private static final int HOUR = 60 * 60 * 1000;

	public ArtSource() {
		super(SOURCE_NAME);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
	}

	@Override
	protected void onTryUpdate(int reason) throws RetryException {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// check connection
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean wifiOnly = sharedPref.getBoolean(SettingsActivity.KEY_PREF_WIKI, true);
		if (wifiOnly) {
			NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
			if (!isConnected) {
				scheduleUpdate(System.currentTimeMillis() + HOUR);
				return;
			}
			boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
			if (!isWiFi) {
				scheduleUpdate(System.currentTimeMillis() + HOUR);
				return;
			}
		}

		// get new artwork info
		String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint("http://onweekdays.ulmus.cz")
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
					}
				})
				.setErrorHandler(new ErrorHandler() {
					@Override
					public Throwable handleError(RetrofitError retrofitError) {
						int statusCode = retrofitError.getResponse().getStatus();
						if (retrofitError.getKind() == RetrofitError.Kind.NETWORK
								|| (500 <= statusCode && statusCode < 600)) {
							return new RetryException();
						}
						scheduleUpdate(System.currentTimeMillis() + HOUR);
						return retrofitError;
					}
				})
				.build();

		ArtService service = restAdapter.create(ArtService.class);
		PhotoResponse response = service.getRandomPhoto();

		if (response == null || response.photo == null) {
			throw new RetryException();
		}

		// publish new artwork
		publishArtwork(new Artwork.Builder()
				.title(response.photo.name)
				.byline(response.photo.author)
				.imageUri(Uri.parse(response.photo.url))
				.token(response.photo.id)
				.viewIntent(new Intent(Intent.ACTION_VIEW,
						Uri.parse(response.photo.source)))
				.build());

		// schedule next change
		String intervalPref = sharedPref.getString(SettingsActivity.KEY_PREF_INTERVAL, "6");
		Integer interval = Integer.parseInt(intervalPref);

		scheduleUpdate(System.currentTimeMillis() + interval * HOUR);
	}
}

