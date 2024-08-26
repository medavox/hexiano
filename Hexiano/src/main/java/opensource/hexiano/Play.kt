/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011, 2012 David A. Randolph                              *
 *                                                                         *
 *   FILE: Play.java                                                       *
 *                                                                         *
 *   This file is part of Hexiano, an open-source project hosted at:       *
 *   https://github.com/lrq3000/hexiano                                         *
 *                                                                         *
 *   Hexiano is free software: you can redistribute it and/or              *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation, either version          *
 *   3 of the License, or (at your option) any later version.              *
 *                                                                         *
 *   Hexiano is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with Hexiano.  If not, see <http://www.gnu.org/licenses/>.      *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package opensource.hexiano

import java.util.HashMap

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import android.content.pm.PackageManager.NameNotFoundException
import android.media.SoundPool
import android.media.AudioManager

class Play : Activity(), OnSharedPreferenceChangeListener {
	companion object {
		const val ABOUT_DIALOG_ID = 1
		lateinit var mPrefs: SharedPreferences
		lateinit var mBoard: HexKeyboard
		//static FrameLayout mFrame;
		lateinit var mInstrument: MutableMap<String, Instrument>
		lateinit var instrument_load_queue: Iterator<Instrument>
		lateinit var currLoadingInstrument: Instrument
		var mInstrumentChanged = false
		var POLYPHONY_COUNT = 16
		lateinit var mSoundPool: SoundPool

		// Load SoundPool, the sound manager
		fun loadSoundManager() {
			POLYPHONY_COUNT = mPrefs.getString("polyphonyCount", "8")!!.toInt() // Reload polyphony count
			if(!::mSoundPool.isInitialized) {
				mSoundPool = SoundPool(POLYPHONY_COUNT, AudioManager.STREAM_MUSIC, 0).apply {

					// Load another sound whenever the previous one has finished loading
					// NOTE: ensure that this function ALWAYS load only one sound,
					// and that only ONE SOUND is loaded prior (so that this function is only called once.
					// If you load two sounds, this function will be called twice, and so on).
					// This is critical to ensure that the UI remains responsive during sound loading.
					// NOTE2: SoundPool already works in its own thread when loading sounds,
					// so there's no need to use an AsyncTask.
					// There's no way to enhance the UI responsiveness while loading,
					// it's because of SoundPool using lots of resources when loading
					// (hence why you should always ensure to load only one sound to kickstart the SoundPool listener).
					setOnLoadCompleteListener(object : SoundPool.OnLoadCompleteListener {
						override fun onLoadComplete(
							mSoundPool: SoundPool,
							sampleId: Int,
							status: Int
						) {
							mBoard.invalidate() // Redraw board to refresh keys that now have their sound loaded

							// If there are yet other sounds to load in this batch of sounds (tuples)
							if (currLoadingInstrument.sound_load_queue.hasNext()) {
								val tuple = currLoadingInstrument.sound_load_queue.next()
								currLoadingInstrument.addSound(tuple)

								// Else if batch of sounds (tuples) empty but we have other notes sounds to load
							} else if (currLoadingInstrument.notes_load_queue != null && currLoadingInstrument.notes_load_queue.hasNext()) {
								currLoadingInstrument.currListOfTuples =
									currLoadingInstrument.notes_load_queue.next()
								currLoadingInstrument.sound_load_queue =
									currLoadingInstrument.currListOfTuples.iterator()
								onLoadComplete(
									mSoundPool,
									sampleId,
									status
								) // try to load sounds for this next list of tuples (batch of sounds)

								// Else if no more sound for this instrument but we have another instrument for which sounds are to be loaded, we switch to the next instrument
							} else if (instrument_load_queue.hasNext()) {
								// Switch to the next instrument
								currLoadingInstrument = instrument_load_queue.next()
								// Setup the sound load queue for this instrument
								currLoadingInstrument.notes_load_queue =
									currLoadingInstrument.sounds_to_load.values.iterator()
								onLoadComplete(
									mSoundPool,
									sampleId,
									status
								) // try to load sounds for this next instrument

								// Else all sounds loaded! Show a short notification so that the user knows that (s)he can start playing without lags
							} else {
								Toast.makeText(
									HexKeyboard.mContext,
									R.string.finished_loading,
									Toast.LENGTH_SHORT
								).show()
							}
						}
					})
				}
			}
		}

		protected fun addInstrument(instrumentName: String) {
			// Add instrument only if not already in the map
			if (!mInstrument.containsKey(instrumentName)) {
				// Choose the correct instrument class to load, deducting from instrument's name

				// Piano
				if (instrumentName.equals("Piano", ignoreCase=true) ||
					instrumentName.equals("DUMMY (dont choose)", ignoreCase=true)) // Place an if conditional for each staticInstrument (included in APK resources)
				{
					mInstrument[instrumentName] = Piano(HexKeyboard.mContext)
					// Generic external instrument for any other case
				} else {
					try {
						mInstrument[instrumentName] = GenericInstrument(HexKeyboard.mContext, instrumentName)
					} catch (e: IllegalAccessException) {
						// TODO Auto-generated catch block
						e.printStackTrace()
					}
				}
			}
		}

		fun loadInstruments(): Boolean {
			val multiInstrumentsEnabled = mPrefs.getBoolean("multiInstrumentsEnable", false)
			mInstrument = mutableMapOf<String, Instrument>()
			if (!multiInstrumentsEnabled) { // Single instrument
				val instrumentName:String = mPrefs.getString("instrument", "Piano")!!
				addInstrument(instrumentName)
			} else { // Multi instruments
				val mapping = Prefer.getMultiInstrumentsMappingHashMap(mPrefs)
				if (mapping != null && mapping.size > 0) {
					for(instru in mapping.values) {
						val instrumentName = instru["instrument"] ?: "Piano"
						if (!mInstrument.containsKey(instrumentName)) {
							addInstrument(instrumentName)
						}
					}
				}
				// Also add single instrument as the default instrument for undefined keys in mapping
				val instrumentName = mPrefs.getString("instrument", "Piano")!!
				addInstrument(instrumentName)
			}
			return true
		}
	}

	var configChanged: Boolean = false

	private fun getVersionName(): String = try {
		packageManager.getPackageInfo(packageName, 0).versionName
	} catch (e: NameNotFoundException) {
		Log.e("getVersionName", e.message?:"", e)
		""
	}


	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	override protected fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Lock volume control to Media volume.
		volumeControlStream = AudioManager.STREAM_MUSIC

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mPrefs.registerOnSharedPreferenceChangeListener(this)

    	val versionStr = this.getVersionName()

		this.requestWindowFeature(Window.FEATURE_NO_TITLE)
		this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN)

        loadKeyboard()
	}

	protected fun setOrientation(): Int {
		val layout = mPrefs.getString("layout", null)

		var orientationId = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

		if (layout.equals("Sonome")) {
			val isLandscape = mPrefs.getBoolean("sonomeLandscape", false)
			if (!isLandscape) {
				orientationId = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			}
		} else if (layout.equals("Jammer")) {
			val isLandscape = mPrefs.getBoolean("jammerLandscape", false)
			if (!isLandscape) {
				orientationId = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			}
		} else if (layout.equals("Janko")) {
			val isLandscape = mPrefs.getBoolean("jankoLandscape", false)
			if (!isLandscape) {
				orientationId = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			}
		}

		this.requestedOrientation = orientationId
		return orientationId
	}

	// Load the first sound and then delegate the rest of the loading to the SoundManager (SoundPool)
	protected fun loadFirstSound() {
		// Setup the instruments iterator (for the soundmanager to iteratively load all sounds for each instrument)
		instrument_load_queue = mInstrument.values.iterator()
		// Loop until we find a sound to load
		// Note: we do that because with an incorrect multi-instruments setup, it may happen that an instrument has no sound mapped on it (the range specified by user is out of visible screen), in this case we will try to load sounds from other instruments that are correctly mapped (normally, the default single instrument should always map at least one sound in this case)
		var found_a_sound = false
		while (!found_a_sound && instrument_load_queue.hasNext()) { // Loop until either we have found a sound, or we have iterated through all instruments and none have any sound to load (this should never happen!)
			// Initiate the loading process, by loading the first instrument and the first sound for this instrument
			currLoadingInstrument = instrument_load_queue.next()
			// Setup the sound load queue for this instrument
			currLoadingInstrument.notes_load_queue = currLoadingInstrument.sounds_to_load.values.iterator()

			// If we have found a sound to load, we stop here
			if (currLoadingInstrument.notes_load_queue.hasNext()) found_a_sound = true
		}
		// If we iterated through all instruments and never found any sound to load, then it's a fatal error that should never happen!
		if (!found_a_sound) {
			Log.e("Play::loadFirstSound", "Cannot load the first sound for any instrument! Fatal error!")
			Toast.makeText(HexKeyboard.mContext, R.string.error_no_first_sound, Toast.LENGTH_LONG).show()
		} else {
			// Start loading the first instrument and the first sound for the first note, the rest of the sounds are loaded from the Play::loadKeyboard()::OnLoadCompleteListener()
			currLoadingInstrument.currListOfTuples = currLoadingInstrument.notes_load_queue.next()
			currLoadingInstrument.sound_load_queue = currLoadingInstrument.currListOfTuples.iterator()
			val tuple = currLoadingInstrument.sound_load_queue.next()
			currLoadingInstrument.addSound(tuple)
		}
	}

	protected fun loadKeyboard() {
	    val orientationId = setOrientation()

		Toast.makeText(applicationContext, R.string.beginning_loading, Toast.LENGTH_SHORT).show() // Show a little message so that user know that the app is loading

		//mFrame = FrameLayout(con);
		mBoard = HexKeyboard(applicationContext)

		// Reload previously cached data (SoundPool decoded sounds) if available, this really speeds up orientation switches!
		//mInstrument = (HashMap<String, Instrument>) getLastNonConfigurationInstance();
		val prevActivity = lastNonConfigurationInstance as Play?
		if(prevActivity != null) {
			Log.d("Play", "prevActivity is not null!")
			//FIXME: the two properties below are already static, meaning they would already be retained across instances anyway.
			//	was there any other reason it was done this way?
//			Play.mSoundPool = prevActivity.mSoundPool
//			Play.mInstrument = prevActivity.mInstrument
		} else { // If no retained audio (or changed), load it all up (slow).
		//if (mInstrument == null || mInstrumentChanged) {
			// Load SoundPool, the sound manager
	        loadSoundManager()
	        // Then, load the instruments
			loadInstruments()
		//}
		}
		mBoard.setUpBoard(orientationId)
		mBoard.invalidate()

		loadFirstSound() // Do this only after setUpBoard() so that it can setup the keyboard and limit the range of notes to load

		// mFrame.addView(mBoard);
		//LayoutParams layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
		//		LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);

		// this.setContentView(mFrame);
		this.setContentView(mBoard)
	}

	// Cache data to speedup orientation switches
	override fun onRetainNonConfigurationInstance(): Any {
		Log.d("Play", "onRetainNonConfigurationInstance()")
		// Retain the audio across configuration changes.
		// TODO: try to retain SoundPool when just quitting preferences without changing anything,
		//  but not sure it could be retained since it's not serializable.
		//  Maybe as a service? http://stackoverflow.com/questions/16169488/how-to-get-an-android-bound-service-to-survive-a-configuration-restart
		// TODO: or use fragments: https://github.com/alexjlockwood/worker-fragments
		// TODO: or change SoundPool for AudioTrack to read directly from file (but does it handle ogg?):
		//  http://www.martinhoeller.net/2012/01/13/developing-a-musical-instrument-app-for-android/
		//return mInstrument;
		return this
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Log.d("Play", "Menu, ho!")
		val inflater = getMenuInflater()
		inflater.inflate(R.menu.menu, menu)
		return true
	}

    override protected fun onCreateDialog(id: Int): Dialog? {
		return when (id) {
			ABOUT_DIALOG_ID -> AboutDialog(this)
			else -> null
		}
    }

	override fun onOptionsItemSelected(item: MenuItem ): Boolean {
		if (item.itemId == R.id.preferences) {
			startActivity(Intent(this, Prefer::class.java))
		} else if (item.itemId == R.id.quit) {
			finish()
		} else if (item.itemId == R.id.about) {
			showDialog(ABOUT_DIALOG_ID)
		}

		return true
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		configChanged = true
	}


	override fun onOptionsMenuClosed(menu: Menu) {
		if (configChanged) {
			unbindDrawables(mBoard)
			System.gc()
			// if (Prefer.InstrumentChanged) {
				// Play.loadInstruments();
			// } else if (Prefer.BoardChanged) {
				//mBoard.setUpBoard(setOrientation());
				//mBoard.invalidate();
			//}
			loadKeyboard()
		}
	}

	fun unbindDrawables(view: View ) {
		if (view.background != null)
			view.background.callback = null

		if (view is ImageView) {
			view.setImageBitmap(null)
		} else if (view is ViewGroup) {
			for (i in 0 .. view.childCount) {
				unbindDrawables(view.getChildAt(i))
			}
			if (view !is AdapterView<*>) {
				view.removeAllViews()
			}
		}
	}

	// Clean all playing states (eg: sounds playing, etc)
	fun cleanStates() {
		HexKeyboard.stopAll()
	}

	override protected fun onPause() {
		// If app is closed/minimized (home button is pressed)
		//if (this.isFinishing()){ // The function isFinishing() returns a boolean. True if your App is actually closing, False if your app is still running but for example the screen turns off.
			// Clean all playing states
			this.cleanStates()
		//}
		super.onPause()
	}

	override protected fun onDestroy() {
		this.cleanStates()
		super.onDestroy()
	}
}
