/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                  *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011 David A. Randolph                                    *
 *                                                                         *
 *   FILE: SonomeKey.java                                                  *
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
package opensource.hexiano;

import android.content.Context;

class SonomeKey(
	context: Context,
	radius: Int,
	center: Point,
	midiNoteNumber: Int,
	instrument: Instrument,
	keyNumber: Int
) : HexKey(context, radius, center, midiNoteNumber, instrument, keyNumber) {
	override protected fun getPrefs() {
		mKeyOrientation = mPrefs.getString("sonomeKeyOrientation", null);
	}

	override fun getColor(): Int {
		val sharpName = mNote.sharpName;
		return if (sharpName.contains("#")) {
			if (sharpName.contains("G")) {
				mBlackHighlightColor;
			} else {
				mBlackColor
			}	
		}
		else if (sharpName.contains("D")) {
			mWhiteHighlightColor
		} else mWhiteColor
	}
}
