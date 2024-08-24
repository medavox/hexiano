/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                  *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011 David A. Randolph                                    *
 *                                                                         *
 *   FILE: JankoKey.java                                                   *
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

class JankoKey(
	context: Context,
	radius: Int,
	center: Point,
	midiNoteNumber: Int,
	instrument: Instrument,
	keyNumber: Int,
	private var mOctaveGroupNumber: Int
) : HexKey(context, radius, center, midiNoteNumber, instrument, keyNumber) {

	protected override fun getPrefs()
	{
		mKeyOrientation = mPrefs.getString("jankoKeyOrientation", null)
	}

	private fun inOddOctave(): Boolean = mOctaveGroupNumber % 2 != 0
	
	override fun getColor(): Int = if (mNote.sharpName.contains("#")) {
		if (inOddOctave()) mBlackHighlightColor else mBlackColor
	}
	else if (inOddOctave()) mWhiteHighlightColor else mWhiteColor
}
