/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft 2013 Stephen Larroque                                        *
 *                                                                         *
 *   FILE: SustainKey.java                                                 *
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

import android.content.Context
import android.util.Log

class SustainKey(
	context: Context,
	radius: Int,
	center: Point,
	midiNoteNumber: Int,
	instrument: Instrument,
	keyNumber: Int
): ModifierKey(context, radius, center, 64, instrument, keyNumber) {
	override fun getColor(): Int = if (mSpecialColor != 0) mSpecialColor else mWhiteColor

	override fun play() {
		if (HexKeyboard.mSustainHold == true && getPressed() == true) {
			stop(true)
		} else {
			HexKeyboard.mSustain = true
			val pitchStr = mMidiNoteNumber.toString()
			Log.d("HexKey::play", pitchStr)
			pressed = true
		}
	}
	
	override fun play(pressure: Float) {
		this.play()
	}
	
	override fun stop(force: Boolean) {
		// TODO: find why not all notes stops sometimes when sustain is released (whether mSustainHold is on or off doesn't matter for this bug)
		if (this.getPressed() == true && (force == true or HexKeyboard.mSustainHold == false)) {
			val pitchStr = mMidiNoteNumber.toString()
			Log.d("HexKey::stop", pitchStr)
			pressed = false
			HexKeyboard.mSustain = false
			HexKeyboard.stopAll() // stop all previously sustained notes
		}
		return
	}
}
