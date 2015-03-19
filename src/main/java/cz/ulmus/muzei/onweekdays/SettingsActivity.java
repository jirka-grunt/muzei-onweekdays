/*
 * Copyright 2015 Jirka Grunt
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

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {

	public static final String KEY_PREF_WIKI = "pref_wifi";
	public static final String KEY_PREF_INTERVAL = "pref_interval";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, new SettingsFragment())
			.commit();
	}
}

