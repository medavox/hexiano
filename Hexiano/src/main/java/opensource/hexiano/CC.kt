/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft 2013 Stephen Larroque                                        *
 *                                                                         *
 *   FILE: CC.java                                                         *
 *                                                                         *
 *   This file is part of Hexiano, an open-source project hosted at:       *
 *   https://github.com/lrq3000/hexiano                                    *
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

class CC(
    protected val mMidiCCNumber: Int,
    /**Just for reference, can be shown as a label on the key, but useless otherwise for the Note*/
    protected val mKeyNumber: Int
) {
    protected val mOctave: Int
    protected val mCCName: String?


    init {
        mCCName = getModifierNameForNoteNumber(mMidiCCNumber)
        mOctave = 1
    }
    
    fun getCCName(): String = mCCName + mOctave

    fun getMidiCCNumber(): Int = mMidiCCNumber

    fun getDisplayString(labelType: String?, showOctave: Boolean): String = when (labelType) {
        "None" -> ""
        "Key Number (DEV)" -> "" + mKeyNumber
        "MIDI Note Number" -> "CC$mMidiCCNumber"
         else -> {
             val name: String? = getModifierNameForNoteNumber(mMidiCCNumber)
             if (name?.isNotEmpty() == true) name else "CC?"
         }
    }

    fun getModifierNameForNoteNumber(midiNoteNumber: Int): String? = mModifierForNumber[midiNoteNumber]

    fun getCCNameForNoteNumber(midiNoteNumber: Int): String? {
        val flatNumber = midiNoteNumber % 12
        return mModifierForNumber[flatNumber]
    }

    companion object {
        fun getNoteNumber(modifierName: String): Int? = mNumberForModifier[modifierName]

        val mModifierForNumber = mapOf<Int, String> (
             1 to "Mod",
             7 to "Volume",
            10 to "Pan",
            11 to "Expression",
            64 to "Sustain",
        )

        val mNumberForModifier = mapOf<String, Int>(
            "Mod" to 1,
            "Volume" to 7,
            "Pan" to 10,
            "Expression" to 11,
            "Sustain" to 64,
        )
    }
}
