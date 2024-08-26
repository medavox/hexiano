/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011 David A. Randolph                                    *
 *                                                                         *
 *   FILE: Piano.java                                                      *
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
 *   but WITHOUT ANY WARRANTY without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with Hexiano.  If not, see <http://www.gnu.org/licenses/>.      *
 *                                                                         *
 *   NOTE: The sounds used for this instrument were derived from           *
 *   the acoustic-piano soundfont created by Roberto Gordo Saez.
 *   Here is its license, which we will display more prominently
 *   in the application and our web site, as soon as we get 
 *   organized:
 *   
Acoustic grand piano soundfont (Yamaha Disklavier Pro), release 2008-09-10
116 samples, 44100Hz, 16bit.

The acoustic grand piano soundfont is free. It is built from the Zenph
Studios Yamaha Disklavier Pro Piano Multisamples for OLPC.

The soundfont itself and all modifications made to the original
samples by Roberto Gordo Saez, published under a Creative Commons
Attribution 3.0 license.

Copyright 2008, Roberto Gordo Saez roberto.gordo@gmail.com Creative 
Commons Attribution 3.0 license http://creativecommons.org/licenses/by/3.0/

Zenph Studios Yamaha Disklavier Pro Piano Multisamples for OLPC:

A collection of Grand Piano samples played by a Yamaha Disklavier
Pro. Performed by computer and specifically recorded for OLPC by
Dr. Mikhail Krishtal, Director of Music Research and Production, and
his team at Zenph Studios. They are included in the OLPC sound
sample library.

How is it being done: "The Disklavier Pro has an internal
electronically-controlled mechanism that allows it to play sounds
with very precise specifications. It has its own file format known
as XP MIDI, an extension of standard midi. I Mikhail Krishtal
prepare the files for it to play -- in this case, representing notes
of different registers, durations, and dynamic levels."

http://csounds.com/olpc/pianoSamplesMikhail/pianoMikhail.html
Produced by Zenph Studios in Chapel Hill, North Carolina. The main
studio location is in Raleigh, North Carolina.

http://zenph.com/
Samples from the OLPC sound sample library:

This huge collection of and original samples have been donated
to Dr. Richard Boulanger @ cSounds.com specifically to support the
OLPC developers, students, XO users, and computer and electronic
musicians everywhere. They are FREE and are offered under a CC-BY
license.

http://wiki.laptop.org/go/Sound_samples http://csounds.com/boulanger

Creative Commons Attribution 3.0 license
http://creativecommons.org/licenses/by/3.0/

* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package opensource.hexiano

import java.util.regex.Pattern
import java.util.TreeMap

import android.content.Context
import android.util.Log
import android.widget.Toast

class Piano(context: Context) : Instrument(context) {
    override var mInstrumentName: String = "Piano"
    override var sounds_to_load = TreeMap<Int, MutableList<SoundLoadingTuple>>() // Are there really no tuples in Java?!

    init {
        val pat = Pattern.compile("^pno_m([0-9]+)(v([0-9]+))?") // Pattern: anythingyouwant_mxxvyy.ext where xx is the midi note, and yy the velocity (velocity is optional)
        val raw = R.raw::class.java
        val fields = raw.getFields() // Fetch all the files (fields) in Raw directory

        // For each file (field) in the Raw directory
        for (field in fields) {
            try {
                // Filter out any other file except the ones that are sounds for this instrument (starts with "pno" for piano)
                val fieldName = field.getName()
                if (fieldName.startsWith("pno", 0)) {
                    // If we find a midi note (matching the regexp)
                    val mat = pat.matcher(fieldName)
                    if (mat.find()) {
                        val midiNoteNumber = mat.group(1).toInt()
                        val fieldValue = field.getInt(null)
                        val velocity =  if (mat.groupCount() > 2 && mat.group(2) != null) {
                            mat.group(3).toInt()
                        } else 127
                        Log.d("Piano",
                            "Found midi note: $midiNoteNumber velocity $velocity fileid $fieldValue"
                        )

                        // Use a tuple arraylist of integers,
                        // which will be the int identifier of the raw resource,
                        // which in java are defined by an integer.
                        // SoundPool can directly load from resource identifiers.
                        val tuple = SoundLoadingTuple(
                            midiNoteNumber = midiNoteNumber,
                            velocity = velocity,
                            resource = SoundLoadingTuple.LoadingResource.AndroidResourceId(fieldValue),
                        )
                        sounds_to_load[midiNoteNumber]?.add(tuple) ?: {
                            sounds_to_load[midiNoteNumber] = mutableListOf<SoundLoadingTuple>(tuple)
                        }
                        mRootNotes[midiNoteNumber] = midiNoteNumber
                        mRates[midiNoteNumber] = 1.0f
                    }
                }
            }
            catch(e: IllegalAccessException) {
                Log.e("REFLECTION", String.format("%s threw IllegalAccessException.",
                    field.getName()), e)
            }
        }
        
        // No sounds found? Show an error message then quit
        if (sounds_to_load.size == 0) {
            // TODO: a better error dialog with nice OK button
            Toast.makeText(context, mInstrumentName + ": " + R.string.error_no_soundfiles, Toast.LENGTH_LONG).show()
        } else {// Extrapolate missing notes (for which we have no sound file) from available sound files
            extrapolateSoundNotes()
        }
    }
}
