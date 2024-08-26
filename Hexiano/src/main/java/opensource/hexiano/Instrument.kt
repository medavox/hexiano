/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011 David A. Randolph                                    *
 *                                                                         *
 *   FILE: Instrument.java                                                 *
 *                                                                         *
 *   This file is part of Hexiano, an open-source project hosted at:       *
 *   https://github.com/lrq3000/hexiano                                         *
 *                                                                         *
 *   Hexiano is free software: you can redistribute it and/or              *
 *   modify it under the terms of the GNU General License           *
 *   as published by the Free Software Foundation, either version          *
 *   3 of the License, or (at your option) any later version.              *
 *                                                                         *
 *   Hexiano is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General License     *
 *   along with Hexiano.  If not, see <http://www.gnu.org/licenses/>.      *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package opensource.hexiano

import android.content.Context
import android.util.Log
import android.widget.Toast
import opensource.hexiano.SoundLoadingTuple.LoadingResource.AndroidResourceId
import opensource.hexiano.SoundLoadingTuple.LoadingResource.ExternalFilePath
import java.util.TreeMap
import kotlin.math.pow

abstract class Instrument(private val mContext: Context) {
    //private AudioManager mAudioManager  = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    // Instrument type definition

    /**Loading external files (needing to pass Strings instead of int[]?).
	 * Defines if the third argument in sounds tuples is a resource id (int) or a file path (string)*/
    var mExternal = false

    /**Important for reference*/
    abstract var mInstrumentName: String

    // Sounds loading variables

    /**raw list of sounds to load in SoundPool,
	 * each entry being a midiNoteNumber associated to a tuple of
	 * [midiNoteNumber, velocity, file resource id int or string file path]*/
    abstract var sounds_to_load: TreeMap<Int, MutableList<SoundLoadingTuple>>

    /** = sounds.iterator(); when iterating through sounds_to_load in SoundPool.onLoadComplete(),
	 * contains a list of sound files (of different velocities) for one note*/
    var notes_load_queue: Iterator<MutableList<SoundLoadingTuple>> = object : Iterator<MutableList<SoundLoadingTuple>>{
        override fun hasNext(): Boolean = false
        override fun next(): MutableList<SoundLoadingTuple> {
            TODO("Not yet implemented")
        }
    }

    /** = notes_load_queue.next();
	 * temporary holder that contains all sounds file (of different velocities) for one midi note*/
    var currListOfTuples: MutableList<SoundLoadingTuple> = mutableListOf()

    /** = currListOfTuples.iterator() contains one sound file at a time*/
    var sound_load_queue: Iterator<SoundLoadingTuple> = object: Iterator<SoundLoadingTuple>{
        override fun hasNext(): Boolean = false
        override fun next(): SoundLoadingTuple = TODO()
    }

    // Sounds holder variables (when loading is completed)

    /**List of all already loaded sound files,
	 * with their id (relative to velocity) for each midi note,
	 * to easily play the sounds.
	 * Format: [midiNoteNumber, [velocity, SoundPool sound id]]*/
    val mSounds = mutableMapOf<Int, TreeMap<Int, Int>?>()
    /**Extrapolation rates: All rates for each midi note.
	 * Used to extrapolate missing note sounds from existing notes sounds (rootNotes).
	 * The frequency is extrapolated from the nearest rootNote available (see mRootNotes for the association).
	 * Format: [midiNote, rate]*/
    val mRates = mutableMapOf<Int, Float>()

    /**Extrapolation association vector:
	 * for each midi note,
	 * define the nearest rootNote from which it should be extrapolated
	 * (if it's not already a rootNote).
	 * Format: [midiNote, rootNote]*/
    val mRootNotes = mutableMapOf<Int, Int>()

    /** Load into a sound into [SoundPool] from either a given path (eg: on SD card), or from the APK resources
	 * @param tuple A [SoundLoadingTuple] specifying the data needed to load the sound
	 */
    fun addSound(tuple: SoundLoadingTuple) {
        // If there's already an entry for this midinote, we update it to add the velocity subentry
        // else there's no entry for this midinote, we just create it
        val velocity_soundid: TreeMap<Int, Int> = mSounds[tuple.midiNoteNumber] ?: TreeMap<Int, Int>()// fetch the midinote entry (containing all previous velocity subentries)
        val velocityForSound: Int = when(tuple.resource) {
            is AndroidResourceId -> Play.mSoundPool?.load(mContext, tuple.resource.id, 1)!!
            is ExternalFilePath -> Play.mSoundPool?.load(tuple.resource.path, 1)!!
        }
        velocity_soundid[tuple.velocity] = velocityForSound
        mSounds[tuple.midiNoteNumber] = velocity_soundid
    }

    /** Limit the range of sounds and notes to the given list of notes*/
    fun limitRange(ListOfMidiNotesNumbers: List<Int>) {
        // -- Delete first the root notes that are not directly used
        // (if the rootNote is not visible then we just delete its entry in mRootNotes and mRates,
        // but NOT the reference from extrapolated keys,
        // this ensures that we trim useless rootNotes that are neither visible neither extrapolated from,
        // but we keep useful rootNotes that are either visible on-screen _or_ extrapolated from
        // (no index in mRootNotes but used as a value for other midi notes))
         var notesToDelete = mutableListOf<Int>()
        // Loop through all root notes
        for (midiNoteNumber: Int in mRootNotes.keys) {
            // And check if this root note is used
            if (!ListOfMidiNotesNumbers.contains(midiNoteNumber)) {
                notesToDelete.add(midiNoteNumber)
            }
        }
        // Delete useless rootNotes and rates for these rootNotes
        for (midiNoteNumber:Int in  notesToDelete) {
            mRootNotes.remove(midiNoteNumber)
            mRates.remove(midiNoteNumber)
        }

        // -- Then delete all the not used sounds
        // (now that only visible or extrapolated from rootNotes are still in mRootNotes,
        // we can remove all sounds that are neither directly visible on-screen, nor extrapolated from.
        // A bit like the first step above but here we do it for all sounds, not just rootNotes).
        notesToDelete = mutableListOf<Int>()
        // Loop through all found notes (from sounds files)
        for (midiNoteNumber: Int in sounds_to_load.keys) {
            // If the note is not in the limited range and there's no note extrapolated from this note's sound,
            // we remove it and its associated sounds
            if (!ListOfMidiNotesNumbers.contains(midiNoteNumber) && !mRootNotes.values.contains(midiNoteNumber) ) {
                notesToDelete.add(midiNoteNumber)
            }
        }
        // Delete notes
        if (notesToDelete.size > 0) {
            for (midiNoteNumber: Int in  notesToDelete) {
                if (sounds_to_load.containsKey(midiNoteNumber)) sounds_to_load.remove(midiNoteNumber)
                if (mRootNotes.containsKey(midiNoteNumber)) mRootNotes.remove(midiNoteNumber)
                if (mRates.containsKey(midiNoteNumber)) mRates.remove(midiNoteNumber)
            }
        }

        // -- Recreate the iterator to generate all sounds of all notes
        notes_load_queue = sounds_to_load.values.iterator()
    }

    /** Extrapolate missing notes from Root Notes (notes for which we have a sound file)*/
    fun extrapolateSoundNotes() {
        var previousRate = 1.0f
        var previousRootNote = -1
         /**Notes before any root note, that we will extrapolate (by downpitching) as soon as we find one root note.
		 TODO: downpitching by default and uppitch only for the rest.
		  Downpitching should not cause any aliasing,
		  but we have to check if the extrapolation doesn't cause evil downpitching.
		  See http://www.discodsp.com/highlife/aliasing/ */
        val beforeEmptyNotes = mutableListOf<Int>()
        val oneTwelfth = 1.0/12.0
        var firstRootNote = true
        var minRate = Float.POSITIVE_INFINITY
        var maxRate = Float.NEGATIVE_INFINITY
        for (noteId in 0..128) {
            // Found a root note, we will extrapolate the next missing notes using this one
            if (mRootNotes.containsKey(noteId)) {
                previousRootNote = noteId
                previousRate = 1.0f
                // Down-pitching extrapolation of before notes (notes before the first root note)
                if (firstRootNote) {
                    // Only if we have before notes to extrapolate
                    if (beforeEmptyNotes.size > 0) {
                        for (bNoteId: Int in beforeEmptyNotes) {
                            mRootNotes[bNoteId] = previousRootNote

                            // a = b / (2^1/12)^n , with n positive number of semitones between frequency a and b
                            val beforeRate = previousRate / 2.0.pow(oneTwelfth).pow((previousRootNote - bNoteId).toDouble())
                            mRates[bNoteId] = beforeRate.toFloat()
                            // Update the min and max rate found (only used for warning message)
                            if (beforeRate < minRate) minRate = beforeRate.toFloat()
                            if (beforeRate > maxRate) maxRate = beforeRate.toFloat()
                        }
                    }
                    firstRootNote = false
                }
            } else { // Else we have a missing note here
                // Up-pitching extrapolation of after notes (notes after we have found the first, and subsequente, root note)
                if (previousRootNote >= 0) {
                    mRootNotes[noteId] = previousRootNote
                    val newRate: Double = previousRate * 2.0.pow(oneTwelfth).pow((noteId - previousRootNote).toDouble()) // b = a * (2^1/12)^n , with n positive number of semitones between frequency a and b
                    mRates[noteId] = newRate.toFloat()
                    // Update the min and max rate found (only used for warning message)
                    if (newRate < minRate) minRate = newRate.toFloat()
                    if (newRate > maxRate) maxRate = newRate.toFloat()
                } else {
                    beforeEmptyNotes.add(noteId)
                }
            }
        }
        // Warning message when min rate or max rate outside of SoundPoolt rate range
        // (rate is guaranteed to be supported between [0.5, 2.0] on all devices,
        // but some devices may also support rates outside of this range, generally below 0.5)
        if (minRate < 0.5f && maxRate > 2.0f) {
            Toast.makeText(mContext, R.string.warning_rate_out_of_range, Toast.LENGTH_LONG).show()
            Log.d("Instr.extrapSoundNotes", mContext.resources.getString(R.string.warning_rate_out_of_range))
        } else if (minRate < 0.5f) {
            Toast.makeText(mContext, R.string.warning_rate_out_of_range_min, Toast.LENGTH_LONG).show()
            Log.d("Instr.extrapSoundNotes", mContext.resources.getString(R.string.warning_rate_out_of_range_min))
        } else if (maxRate > 2.0f) {
            Toast.makeText(mContext, R.string.warning_rate_out_of_range_max, Toast.LENGTH_LONG).show()
            Log.d("Instr.extrapSoundNotes", mContext.resources.getString(R.string.warning_rate_out_of_range_max))
        }
    }

    fun play(midiNoteNumber: Int, pressure: Int) = play(midiNoteNumber, pressure.toFloat(), 0)

    /**Play a note sound given the midi number,
	 * the pressure and optionally a loop number
	 * (-1 for indefinite looping, 0 for no looping, >0 for a definite number of loops)
	@return int[] array int of SoundPool StreamId (to be able to stop the streams later on) */
    fun play(midiNoteNumber: Int, pressure: Float, loop: Int): IntArray {
        // == Get root note and frequency if this note is interpolated
        // Note: a root note is a midi number where we have a sound, the other midi notes sounds being interpolated
        Log.d("Instrument", "play($midiNoteNumber)")
        if (mRootNotes.isEmpty()) return intArrayOf(0) // no sound note available, exit
        val index = mRootNotes[midiNoteNumber] // get root note for this midi number
        Log.d("Instrument", "rootNote found: $index")
        if (!mSounds.containsKey(index)) return intArrayOf(-1) // no sound (root or interpolated) available (yet?) for this note, exit
        val rate = mRates[midiNoteNumber] ?: 0f // Get the rate to which to play this note: 1.0f (normal) for root notes, another number for other notes (changing rate valerpolates the note sound rate is computed at loading)

        //val streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var streamVolume = 1.0f // streamVolume range: 0.0f - 1.0f

        // == Play with the correct velocity
        val velocity_soundid: TreeMap<Int, Int> = mSounds[index] ?: TreeMap()// use a TreeMap to make sure that sounds are sorted in ascending order by velocity

        // -- Compute the velocity value from user's pressure and scale the value (normalize between min and max pressure, and then scale over min and max velocity notes for this midi note)
        val max_vel = velocity_soundid.lastKey() // max velocity available for this note
        val min_vel = velocity_soundid.firstKey() // min velocity available for this note
        var velocity = 0 // user's velocity from touch pressure/surface (will be computed from var pressure)
        var pdiff = (HexKeyboard.mMaxPressure-HexKeyboard.mMinPressure)
        if (pdiff == 0.0f) pdiff = 1.0f // afun division by 0
        val veldiff = (max_vel-min_vel) // no relative range if only one velocity is available! Need to use an absolute range and fake velocity
        if (HexKeyboard.mVelocityRelativeRange && veldiff > 0) {
            // Relative range = min midi note velocity - max midi note velocity (change for each note!)
            velocity = Math.round(((pressure-HexKeyboard.mMinPressure)/pdiff) * veldiff + min_vel )
        } else {
            // Absolute range = 0-127 (like real midi velocity)
            velocity = Math.round((pressure-HexKeyboard.mMinPressure)/pdiff * 127 )
        }
        // Velocity Boost
        if (HexKeyboard.mVelocityBoost > 0) velocity = Math.round( velocity * (1.0f + HexKeyboard.mVelocityBoost/100f ) )
        // Final check: make sure velocity is never above 127
        if (velocity > 127) velocity = 127

        // -- Get the corresponding sound(s) for the user's velocity and from the available velocities
        var lower_vel = 0 // lower velocity bound if user's velocity is in-between (to select the sound at the velocity below the user's pressure)
        var higher_vel = 0 // higher velocity bound if user's velocity is in-between (to select the sound at the velocity higher to the user's pressure, and we will mix it with the lower_vel sound to simulate a velocity that is in-between)
        var soundid: Int = 0 // lower velocity sound or sound exactly equal to user's velocity
        var soundid2 = 0 // higher velocity sound if user's velocity is in-between, or null
        var stream1Volume: Float = 0f // volume for lower velocity sound if user's velocity is in-between, to allow for blending of two velocity sounds
        var stream2Volume: Float = 0f // volume for higher velocity sound if user's velocity is in-between, to allow for blending of two velocity sounds

        // Fake velocity (modulate only the sound volume)
        if (velocity_soundid.size == 1) {
          //higher_vel = (Integer) velocity_soundid.keySet().toArray()[0];
            higher_vel = velocity_soundid.firstKey()
            soundid = velocity_soundid[higher_vel] ?: 0
            streamVolume *= velocity / higher_vel
        // Real velocity with interpolation (either get a sample sound for this velocity and note, or interpolate from two close velocities)
        } else {
            // TreeMap ensures that entries are always ordered by velocity (from lowest to highest),
            // thus a subsequent velocity may only be higher than the previous one.
            // For each velocity available for this note (iterate in ascending order from lowest velocity to highest)
            for (vel in velocity_soundid.entries) {
                higher_vel = vel.key // get the current velocity in TreeMap (at the end, it will store the higher velocity bound sound)

                // Case 1: higher bound: one sound when user's velocity is equal or lower than any available velocity sound
                if (higher_vel == velocity || // if current available velocity is exactly equal to user's velocity
                        (higher_vel > velocity && lower_vel == 0)) { // or if it's above usen's velocity but there's no lower velocity available
                    // Just use the current velocity sound
                    soundid = vel.value
                    break // Found our sound, exit the loop
                // Case 2: middle bound: two sounds when user's velocity is in-between two available velocity sounds
                } else if (higher_vel > velocity && lower_vel != 0) {
                    soundid = velocity_soundid[lower_vel] ?: 0 // get lower bound velocity sound
                    soundid2 = vel.value // == velocity_soundid.get(current_vel) // higher bound velocity sound
                    // Compute the streams volumes for sounds blending
                    val vdiff = higher_vel - lower_vel // compute ratio between max and min available velocity, which will represent the max volume ratio
                    stream2Volume = (velocity - lower_vel) / (vdiff * streamVolume) // compute lower velocity sound volume (note: inversed s1 and s2 on purpose, to afun computing stream1Volume = 1.0f - stream1Volume and stream1Volume = 1.0f - stream2Volume)
                    stream1Volume = (higher_vel - velocity) / (vdiff * streamVolume) // compute higher velocity sound volume
                    break // Found our sounds, exit the loop
                }
                lower_vel = higher_vel // keep previous velocity (= lower bound velocity)
            }
            // Case 3: lower bound: one sound when user's velocity is higher than any available velocity
            // (we iterated all available velocities and could not find any higher)
            if (soundid == 0) {
                soundid = (if (lower_vel != 0) velocity_soundid[lower_vel] else velocity_soundid[higher_vel]) ?: 0
            }
        }

        Log.d("Instrument.play", "VelocityCheck: midinote: " + midiNoteNumber +
                " soundid: " + soundid + " soundid2: "+ soundid2 + " sound_max_vel/sound_min_vel " +
                max_vel + "/" + min_vel + " velocity/lower_vel/higher_vel " + velocity + "/" +
                lower_vel + "/" + higher_vel + " pressure " + pressure + " max/min " +
                HexKeyboard.mMaxPressure + "/" + HexKeyboard.mMinPressure + " s/s1/s2 vol " +
                streamVolume + "/" + stream1Volume + "/" + stream2Volume
        )

        // Velocity interpolation via Blending: we blend two velocity sounds (lower and higher bound)
        // with volumes proportional to the user's velocity to interpolate the missing velocity sound
        return if (soundid2 != 0) {
            intArrayOf(
                Play.mSoundPool?.play(soundid, stream1Volume.toFloat(), stream1Volume, 1, 0, rate),
                Play.mSoundPool?.play(soundid2, stream2Volume, stream2Volume, 1, 0, rate)
            )
            // Else no interpolation, we have found an exact match for the user's velocity
        } else {
            IntArray(Play.mSoundPool.play(soundid, streamVolume, streamVolume, 1, 0, rate))
        }
    }

    fun stop(mStreamId: IntArray) {
        for(streamId: Int in mStreamId) {
            Play.mSoundPool.stop(streamId)
        }
    }

    // Play and indefinitely loop a sound
    fun loop(midiNoteNumber: Int, pressure: Float): IntArray {
        return this.play(midiNoteNumber, pressure, -1)
        /*
		val streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
		mSoundPool.play(mSounds.get(index), streamVolume, streamVolume, 1, -1, 1f)
		*/
    }
}
