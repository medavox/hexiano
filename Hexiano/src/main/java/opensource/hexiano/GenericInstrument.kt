/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *                                                                         *
 *   FILE: GenericInstrument.java                                          *
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

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.TreeMap
import java.util.regex.Matcher
import java.util.regex.Pattern


class GenericInstrument(context: Context , instrument: String): Instrument(context) {
    override var mInstrumentName = instrument
    override var sounds_to_load: TreeMap<Int, MutableList<SoundLoadingTuple>>

    init {
        this.mExternal = true

        /** Pattern: anythingyouwant_mxxvyy.ext where xx is the midi note,
         * and yy the velocity (velocity is optional)*/
        val pat = Pattern.compile("m([0-9]+)(v([0-9]+))?.*\\.[^\\.]*$")

        /** Get the list of all files for this instrument (folder)*/
        val files: List<File> = listExternalFiles("$instrument/")
        sounds_to_load = TreeMap<Int, MutableList<SoundLoadingTuple>>()
        // For each file in this folder/instrument
        for (file in files) {
            val fileName: String = file.getName()
            Log.d("GenericInstrument", "Found file: $fileName")
            // If we find a midi note (matching the regexp)

            val mat: Matcher = pat.matcher(fileName)
            if (mat.find()) {
                // Parse the filename string to get the midi note
                val midiNoteNumber: Int = mat.group(1)?.toInt() ?: 0
                val filePath: String = file.absolutePath
                val velocity: Int = mat.group(2)?.toInt() ?: 127
                Log.d("GenericInstrument", "Found midi note: "+midiNoteNumber + " velocity " + Integer.toString(velocity) + " filepath " + filePath)
                // And store it inside the array sounds with the midiNoteNumber being the id and filePath the resource to load

                val tuple = SoundLoadingTuple(
                    midiNoteNumber = midiNoteNumber,
                    velocity = velocity,
                    resource = SoundLoadingTuple.LoadingResource.ExternalFilePath(filePath)
                )
                sounds_to_load[midiNoteNumber]?.add(tuple) ?: {
                    sounds_to_load[midiNoteNumber] = mutableListOf<SoundLoadingTuple>(tuple)
                }
                // Also set this note as a root note (Root Notes are notes we have files for,
                // from which we will extrapolate other notes that are missing if any)
                mRootNotes[midiNoteNumber] = midiNoteNumber;
                // Rate to play the file with, default is always used for root notes,
                // we only change the rate on other notes (where we don't have a file available) to interpolate the frequency and thus the note
                mRates[midiNoteNumber] = 1.0f
            }
        }

        // No sounds found? Show an error message then quit
        if (sounds_to_load.size == 0) {
            // TODO: a better error dialog with nice OK button
            Toast.makeText(context, mInstrumentName + ": " + R.string.error_no_soundfiles, Toast.LENGTH_LONG).show();
        } else {
            // Extrapolate missing notes (for which we have no sound file) from available sound files
            extrapolateSoundNotes()
        }
    }

    companion object {
        /**List all external (eg: sd card) instruments (just the name of the subfolders)*/
        fun listExternalInstruments():  List<String> = GenericInstrument.listExternalInstruments("/hexiano/")

        /**List all external instruments (just the name of the subfolders)*/
        fun listExternalInstruments(p: String): List<String> {
            val Directories = mutableListOf<String>()
            var path = p
            if (path.isNotEmpty()) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    path = Environment.getExternalStorageDirectory().toString() + path
                    Log.d(
                        "GenrcInstr.lsExtInstrs",
                        "GenericInstrument: list external instruments (directories) from path: $path"
                    )

                    val f = File(path)
                    val files = f.listFiles()
                    if (files != null && files.isNotEmpty()) {
                        for (file in files) {
                            if (file.isDirectory()) { // is directory
                                Directories.add(file.getName())
                            }
                        }
                    }
                }
            }
            return Directories
        }

        /**List all files in the external instrument's folder*/
        fun listExternalFiles(p: String): List<File> {
            var files: List<File>? = null
            var path = p
            if (path.isNotEmpty()) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    path = Environment.getExternalStorageDirectory().toString() + "/hexiano/" + path
                    Log.d(
                        "GenrcInstr.listExtFiles",
                        "GenericInstrument: list external files from path: $path"
                    )

                    val f = File (path)
                    files = f.listFiles()?.toList()
                }
            }
            return files ?: listOf()
        }
    }
}
