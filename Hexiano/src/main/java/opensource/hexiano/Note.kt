/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011, 2012 David A. Randolph                              *
 *                                                                         *
 *   FILE: Note.java                                                       *
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
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package opensource.hexiano

class Note(
    val midiNoteNumber: Int,
    /**Just for reference, can be shown as a label on the key, but useless otherwise for the Note*/
    val keyNumber: Int
) {
    val octave: Int
    val flatName: String
    val sharpName: String

    init {
        flatName = getFlatNameForNoteNumber(midiNoteNumber)
        sharpName = getSharpNameForNoteNumber(midiNoteNumber)
        octave = getOctaveForNoteNumber(midiNoteNumber)
    }
    
    fun getFlatName() = flatName + octave

    fun getSharpName() = sharpName + octave

    fun getMidiNoteNumber(): Int = midiNoteNumber

    fun getDisplayString(labelType: String?, showOctave: Boolean): String {
        var noteStr = "?"

        when (labelType) {
            "None" -> {
                return ""
            }
            "Key Number (DEV)" -> {
                return("" + keyNumber)
            }
            "MIDI Note Number" -> {
                return("" + midiNoteNumber)
            }
            "Whole Tone Number" -> {
                noteStr = "" + midiNoteNumber/2
                if (midiNoteNumber % 2 == 1) {
                    noteStr += ".5"
                }

                return(noteStr)
            }
            "Deutsch" -> {
                noteStr = toGerman[sharpName]!!
            }
            "English" -> {
                noteStr = toEnglish[sharpName]!!
            }
            "Solfege" -> {
                noteStr = toSolfege[sharpName]!!
            }
        }
      
        if (showOctave) {
            noteStr += octave
        }
        
        return noteStr
    }

    companion object {
        val toGerman = mapOf<String, String>(
            "A"  to "A",
            "A#" to "ais",
            "Bb" to "B",
            "B"  to "H",
            "C"  to "C",
            "C#" to "cis",
            "Db" to "des",
            "D"  to "D",
            "D#" to "dis",
            "Eb" to "es",
            "E"  to "E",
            "F"  to "F",
            "F#" to "fis",
            "Gb" to "ges",
            "G"  to "G",
            "G#" to "gis",
            "Ab" to "as",
        )

        /**    We should use \u266F for the sharp symbol, but this has a lot of
        extra space around it for some reason. So, for now, we will just
        use the # character.*/
        val toSolfege = mapOf<String, String>(
            "A"  to "La",
            "A#" to "La#",
            "Bb" to "Si\u266D",
            "B"  to "Si",
            "C"  to "Do",
            "C#" to "Do#",
            "Db" to "Re\u266D",
            "D"  to "Re",
            "D#" to "Re#",
            "Eb" to "Mi\u266D",
            "E"  to "Mi",
            "F"  to "Fa",
            "F#" to "Fa#",
            "Gb" to "Sol\u266D",
            "G"  to "Sol",
            "G#" to "Sol#",
            "Ab" to "La\u266D",
        )

        val toEnglish = mapOf<String, String>(
            "A"  to "A",
            "A#" to "A#",
            "Bb" to "B\u266D",
            "B"  to "B",
            "C"  to "C",
            "C#" to "C#",
            "Db" to "D\u266D",
            "D"  to "D",
            "D#" to "D#",
            "Eb" to "E\u266D",
            "E"  to "E",
            "F"  to "F",
            "F#" to "F#",
            "Gb" to "G\u266D",
            "G"  to "G",
            "G#" to "G#",
            "Ab" to "A\u266D",
        )

        val flatForNumber = mapOf<Int, String>(
             0 to "C",
             1 to "Db",
             2 to "D",
             3 to "Eb",
             4 to "E",
             5 to "F",
             6 to "Gb",
             7 to "G",
             8 to "Ab",
             9 to "A",
            10 to "Bb",
            11 to "B",
        )

        val sharpForNumber = mapOf<Int, String>(
             0 to "C",
             1 to "C#",
             2 to "D",
             3 to "D#",
             4 to "E",
             5 to "F",
             6 to "F#",
             7 to "G",
             8 to "G#",
             9 to "A",
            10 to "A#",
            11 to "B",
        )

        val numberForSharp = mapOf<String, Int>(
            "C"  to 0,
            "C#" to 1,
            "D"  to 2,
            "D#" to 3,
            "E"  to 4,
            "F"  to 5,
            "F#" to 6,
            "G"  to 7,
            "G#" to 8,
            "A"  to 9,
            "A#" to 10,
            "B"  to 11,
        )

        fun getNoteNumber(sharpName: String, octave: Int): Int {
            val noteNumber: Int = (octave * 12) + numberForSharp[sharpName]!! + 12
            return noteNumber
        }
    }
    fun getModifierNameForNoteNumber(midiNoteNumber: Int): String = flatForNumber[midiNoteNumber]!!

    fun getFlatNameForNoteNumber(midiNoteNumber: Int): String {
        val flatNumber: Int = midiNoteNumber % 12
        return flatForNumber[flatNumber]!!
    }
    
    fun getSharpNameForNoteNumber(midiNoteNumber: Int): String {
        val flatNumber: Int = midiNoteNumber % 12
        return sharpForNumber[flatNumber]!!
    }
    
    fun getOctaveForNoteNumber(midiNoteNumber: Int): Int {
        val octavePlusOne: Int = midiNoteNumber/12
        return octavePlusOne - 1
    }
}
