/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *   Copyright © 2012 James Haigh                                          *
 *   Copyright © 2011 David A. Randolph                                    *
 *                                                                         *
 *   FILE: HexKey.java                                                     *
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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

abstract class HexKey(
	context: Context,
	radius: Int,
	private val mCenter: Point,
	midiNoteNumber: Int,
	private val mInstrument: Instrument,
	mKeyNumber: Int
) {
	companion object {
		lateinit var mPrefs: SharedPreferences
		var mKeyOrientation: String? = null
		var mKeyOverlap = false
		var mKeyCount = 0
		var colorTheme:ColorTheme = ColorTheme.Default
		var mRadius:Int = 0

		fun getKeyOrientation(context: Context): String? {
			mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			val layoutPref = mPrefs.getString("layout", null);
			if (layoutPref.equals("Sonome")) { // Sonome
				mKeyOverlap = false;
				return mPrefs.getString("sonomeKeyOrientation", null);
			}
			else if (layoutPref.equals("Janko")) { // Janko
				mKeyOverlap = false;
				return mPrefs.getString("jankoKeyOrientation", null);
			}
			else { // Jammer
				return mPrefs.getString("jammerKeyOrientation", null);
			}
		}
	}
	lateinit var mTop: Point
	lateinit var mBottom: Point
	lateinit var mUpperLeft: Point
	lateinit var mUpperRight: Point
	lateinit var mLowerLeft: Point
	lateinit var mLowerRight: Point
	lateinit var mMiddleLeft: Point
	lateinit var mMiddleRight: Point
	val mPaint = Paint()
	val mPressPaint = Paint()
	val mOverlayPaint = Paint()
	val mTextPaint = Paint()
	val mBlankPaint = Paint()

	var mStreamId = mutableListOf<Int>(-1)
    private var mPressed = false // If key is pressed or not
    private var mDirty = true; // Used to check if a key state has changed, and if so to paint the new state on screen (functional code like play and stop are called on touch events in HexKeyboard)
	private var sound_loaded = false;
	protected var mNoSound = false;
    
    protected var mNote: Note = Note(midiNoteNumber, mKeyNumber); // keyNumber is just for reference to show as a label on the key, useless otherwise
    protected var mMidiNoteNumber: Int = mNote.getMidiNoteNumber();
    protected var mCC: CC? = null

	init {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		getPrefs();

		setColors();
		mRadius = radius;

		setCriticalPoints();

		mPressPaint.setColor(colorTheme.mPressedColor)
        mPressPaint.setAntiAlias(true);
        mPressPaint.setStyle(Paint.Style.FILL);
        mPressPaint.setStrokeWidth(2f)
        
		mPaint.setColor(getColor());
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(2f)
       
		mOverlayPaint.setColor(colorTheme.mOutlineColor)
        mOverlayPaint.setAntiAlias(true);
        mOverlayPaint.setStyle(Paint.Style.STROKE);
        mOverlayPaint.setStrokeWidth(2f)
        
		mTextPaint.setColor(colorTheme.mTextColor)
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(20f)
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        
		mBlankPaint.setColor(colorTheme.mBlankColor)
        mBlankPaint.setStyle(Paint.Style.FILL);
		
		mKeyCount++;
	}

	open fun getPrefs(){}

	protected fun setColors() {
		// Colours have been left from the historically used AndroidWorld library.
		// Format: int beginning with 0xFF and then a RGB HTML/hexadecimal code (3 layers, such as FF9900 with FF for red, 99 for green and 00 for blue)
		val colorPref = mPrefs.getString("colorScheme", null);
		colorTheme = if (colorPref.equals("Khaki")) {
			ColorTheme.Khaki
		} else if (colorPref.equals("Azure")) {
			ColorTheme.Azure
		} else if (colorPref.equals("White") ||
			colorPref.equals("Black & White") ||
			/*...renamed to...*/ colorPref.equals("Slate")
		) {
			ColorTheme.White
		} else if (colorPref.equals("Silhouette")) {
			ColorTheme.Silhouette
		} else if (colorPref.equals("Grey & White")) {
			ColorTheme.GreyAndWhite
		} else if (colorPref.equals("Ebony & Ivory")) {
			ColorTheme.EbonyAndIvory
		} else if (colorPref.equals("Blank")) {
			ColorTheme.Blank
		} else {
			ColorTheme.Default
		}
	}
	
	abstract fun getColor(): Int

	protected fun getHexagonPath(): Path {
		if (mKeyOrientation.equals("Horizontal")) {
			return getHorizontalHexagonPath();
		}
		return getVerticalHexagonPath();
	}
	
    protected fun getVerticalHexagonPath(): Path {
        val hexy = Path()
        val angle = Math.PI / 3
        val increment = Math.PI / 3
        hexy.moveTo((mRadius * cos(angle).toFloat()),
                    (mRadius * sin(angle)).toFloat())
        for(i in 1 .. 6) {
            hexy.lineTo((mRadius * cos(angle + i * increment)).toFloat(),
                        (mRadius * sin(angle + i * increment)).toFloat())
        }
        
        hexy.close();
        
        return hexy;
    }
	
    protected fun getHorizontalHexagonPath(): Path {
        val hexy = Path();
        val angle = Math.PI / 2;
        val increment = Math.PI / 3;
        hexy.moveTo((mRadius * cos(angle)).toFloat(),
                    (mRadius * sin(angle)).toFloat());
        for(i in 1 .. 6) {
            hexy.lineTo((mRadius * cos(angle + i * increment)).toFloat(),
                        (mRadius * sin(angle + i * increment)).toFloat());
        }
        
        hexy.close();
        
        return hexy;
    }

    // Check if the key is correctly initialized, else we will show it as unloaded (pressed) in paint()
    fun checkInit(): Boolean = !this.sound_not_loaded(); // Sets mDirty if just loaded.

    /** Paint this Polygon into the given graphics */
    fun paint(canvas: Canvas) {
    	this.checkInit(); // Sets mDirty if just loaded.
    	
    	// Check if something has changed for this key using mDirty: if nothing, then we don't need to repaint
    	if (!mDirty) {
    		return
    	}
    
  		val hexPath = getHexagonPath();

  		// If the key is void (neither a note nor a CC/Modifier) then just don't paint it (will be all-black on screen)
		if (this.mMidiNoteNumber < 1) { // (this.mMidiNoteNumber < 21 || this.mMidiNoteNumber > 108)
			// Key shaping/painting
    		hexPath.offset(mCenter.x.toFloat(), mCenter.y.toFloat());
    		canvas.drawPath(hexPath, mBlankPaint); // all blank (black)
		}
		// Else if the key is pressed OR not yet initialized, show a greyed space
		else if (mPressed || !this.checkInit()) {
			// Key shaping/painting
    		hexPath.offset(mCenter.x.toFloat(), mCenter.y.toFloat());
    		canvas.drawPath(hexPath, mPressPaint); // Background (greyed) color
    		canvas.drawPath(hexPath, mOverlayPaint); // Contour
    	}
		// Else the key is released and is either a note or CC/Modifier, we paint it with a label and the corresponding color
    	else {
    		val labelPref: String? = mPrefs.getString("labelType", null)
			val cc = mCC
    		var label = cc?.getDisplayString(labelPref, true) ?: mNote.getDisplayString(labelPref, true)
    		// If the note exists but there's no sound available, append an (X) to the label
    		if (mNoSound && mNote != null) {
    			label += " (X)";
    		}

    		// Key shaping/painting
    		hexPath.offset(mCenter.x.toFloat(), mCenter.y.toFloat());
    		canvas.drawPath(hexPath, mPaint); // Background (normal) color
    		canvas.drawPath(hexPath, mOverlayPaint); // Contour
    		
    		// Label printing
    		val bounds = Rect();
    		mTextPaint.getTextBounds(label, 0, label.length, bounds); // get label size
    		val labelHeight: Int = bounds.bottom - bounds.top; // place the label (depending on the size)
    		val x = mCenter.x;
    		val y: Int = mCenter.y + Math.abs(labelHeight/2);
    		canvas.drawText(label, x.toFloat(), y.toFloat(), mTextPaint); // print the label on the key
    	}
    	
    	mDirty = false;
    }
    
	fun sound_not_loaded(): Boolean {
		if (sound_loaded == true) {
			return false;
		} else {
			// Load sound only if it's a note (CC keys won't load any sound)
			if (this.mNote != null && mInstrument != null && mInstrument.mRootNotes.containsKey(mMidiNoteNumber)) {
				val index = mInstrument.mRootNotes.get(mMidiNoteNumber);
				sound_loaded = mInstrument.mSounds.containsKey(index);

				// Set mDirty if just loaded (to force refresh the painting of the key next time)
				if (sound_loaded == true) {
					mNoSound = false;
					mDirty = true;
				} else {
					mNoSound = true; // If the sound cannot be loaded, maybe the sound file is not available for this note
				}
			// Else we just tell the sound is OK (even if there's no sound for this key, it may be a Modifier/CC key)
			} else {
				// In any other case, we know this key will have no sound
				// (either because it's a Modifier/CC key or because this a note but Instrument.mRootNotes does not contain the sound for this note)
				mNoSound = true;
				sound_loaded = true;
			}
			return !sound_loaded;
		}
	}

    fun getPressed(): Boolean = mPressed
   
    fun setPressed(state: Boolean) {
    	if (state != mPressed) {
    	    mPressed = state;
    	    mDirty = true;
    	}
    }
    
	override fun toString(): String {
		val str = StringBuilder("HexKey: ")
		str.append("Center: (" + mCenter.x + ", " + mCenter.y + ")")
		return str.toString()
	}

	/** Set the touch area for the keys*/
	private fun setCriticalPoints() {
		if (mKeyOrientation.equals("Horizontal") || mKeyOverlap) {
			setHorizontalCriticalPoints();
		} else {
			setVerticalCriticalPoints();
		}
	}

	/** Set the touch area for the key when disposition is horizontal (hexagon is vertical)
	 This is a regular hexagon, composed of 6 points: the vertex of the center rectangle + two tips*/
	private fun setHorizontalCriticalPoints() {
		// Compute a scaled-down touch area if mTouchScale is set (to create gaps between keys to avoid false triggers)
		val radius:Int = mRadius * HexKeyboard.mTouchScale / 100;

		// Compute the tips of the hexagon (since it's vertical, the tips are at the top and bottom)
		mTop = Point(mCenter.x, mCenter.y - radius);
		mBottom = Point(mCenter.x, mCenter.y + radius);

		// Compute the center rectangle's vertexes 
		val angle = Math.PI / 6;
		mUpperRight = Point((mCenter.x + radius * cos(angle)).toInt(),
				(mCenter.y - radius * sin(angle)).toInt())
		mLowerRight = Point((mCenter.x + radius * cos(angle)).toInt(),
				(mCenter.y + radius * sin(angle)).toInt())
		mLowerLeft = Point((mCenter.x - radius * cos(angle)).toInt(),
				(mCenter.y + radius * sin(angle)).toInt())
		mUpperLeft = Point((mCenter.x - radius * cos(angle)).toInt(),
				(mCenter.y - radius * sin(angle)).toInt())

		// Debug message
		Log.d("setHorizontlCritPoints",
				"Center: " + mCenter.toString() +
				" Radius: " + mRadius +
				"Critical points: " +
				mUpperRight.toString() +
				mLowerRight.toString() +
				mBottom.toString() + 
				mLowerLeft.toString() +
				mUpperLeft.toString() +
				mTop.toString()
				); // Coordinates will be given clockwise
	}
	
	/** Set the touch area for the key when disposition is vertical (hexagon is horizontal)
	 This is a regular hexagon, composed of 6 points: the vertex of the center rectangle + two tips*/
	private fun setVerticalCriticalPoints() {
		// Compute a scaled-down touch area if mTouchScale is set (to create gaps between keys to avoid false triggers)
		val radius: Int = mRadius * HexKeyboard.mTouchScale / 100;

		// Compute the tips of the hexagon (since it's horizontal, the tips are at the middle left and middle right)
		mMiddleLeft = Point(mCenter.x - radius, mCenter.y);
		mMiddleRight = Point(mCenter.x + radius, mCenter.y);

		// Compute the center rectangle's vertexes
		mLowerLeft = Point(mCenter.x - radius/2, 
				mCenter.y + (Math.round(sqrt(3.0) * radius)/2).toInt());
		mLowerRight = Point(mCenter.x + radius/2, 
				mCenter.y + (Math.round(sqrt(3.0) * radius)/2).toInt());
		mUpperLeft = Point(mCenter.x - radius/2, 
				mCenter.y - (Math.round(sqrt(3.0) * radius)/2).toInt());
		mUpperRight = Point(mCenter.x + radius/2, 
				mCenter.y - (Math.round(sqrt(3.0) * radius)/2).toInt());

		// Debug message
		Log.d("setVerticalCritPoints",
				"Center: " + mCenter.toString() +
				" Radius: " + mRadius +
				"Critical points: " +
				mUpperRight.toString() +
				mMiddleRight.toString() +
				mLowerRight.toString() +
				mLowerLeft.toString() +
				mMiddleLeft.toString() +
				mUpperLeft.toString()
				); // Coordinates will be given clockwise
	}

	/** Check if a (touch) point should trigger this key (contained inside the critical points)*/
	fun contains(pos: Point): Boolean {
		return this.contains(pos.x, pos.y);
	}

	/** Check if a (touch) point should trigger this key (contained inside the critical points)*/
	fun contains(x: Int, y: Int): Boolean {
		if (mKeyOverlap) {
			return overlapContains(x, y);
		} else if (mKeyOrientation.equals("Horizontal")) {
			return horizontalContains(x, y);
		} else {
			return verticalContains(x, y);
		}
	}

	/** Special functionality to trigger two nearby keys with one touch point*/
	fun overlapContains(x: Int, y: Int): Boolean {
		Log.e("HexKey::overlapContains", "Not supported by layout!");
		return false;
	}

	/** Check if a (touch) point should trigger this key (contained inside the critical points)*/
	fun horizontalContains(x: Int, y: Int): Boolean {
		/*
		 * 
		 * We split the hexagon into three areas to determine if the specified coordinates
		 * are included. These are a "center-cut" rectangle, where most positive examples are
		 * expected, and a top and bottom triangle:
		 * 
		      /\
		     /  \    <--- Top triangle
		    /    \
		   |------|
		   |      |  <--- Center cut
		   |      |
		   |------|
		    \    /
		     \  /    <--- Bottom triangle
		      \/
		  
		  */
		if (x >= mLowerLeft.x && x <= mLowerRight.x &&
			y >= mUpperLeft.y && y <= mLowerLeft.y) {
			Log.d("HexKey.horizntlContains", "Center cut");
			return true; // Center cut.
		}
		if (x < mUpperLeft.x || x > mUpperRight.x ||
			y < mTop.y || y > mBottom.y) {
			return false; // Air ball.
		}
		if (y <= mUpperLeft.y) { //Could be in top "triangle."
			if (x <= mTop.x) {
				// We are in left half of the top triangle if the
				// slope formed by the line from the (x, y) to the top
				// vertex is >= the slope from the upper-left vertex to the
				// top vertex. We take the negative because the y-coordinate's
				// sign is reversed.
				val sideSlope: Double = (-1.0) * (mTop.y - mUpperLeft.y)/(mTop.x - mUpperLeft.x)
				val pointSlope: Double = (-1.0) * (mTop.y - y)/(mTop.x - x)
				
				Log.d("HexKey.horizntlContains", "Upper-left side slope: " + sideSlope);
				Log.d("HexKey.horizntlContains", "Upper-left point slope: " + pointSlope);
				
				if (pointSlope >= sideSlope) {
					return true;
				}
			}
			else {
				// We are in right half of the top triangle if the
				// slope formed by the line from the (x, y) to the top
				// vertex is <= (more negative than) the slope from the
				// upper-left vertex to the top vertex. We take the
				// negative because the y-coordinate's sign is reversed.
				val sideSlope: Double = (-1.0) * (mTop.y - mUpperRight.y)/(mTop.x - mUpperRight.x)
				val pointSlope: Double = (-1.0) * (mTop.y - y)/(mTop.x - x);
				Log.d("HexKey.horizntlContains", "Lower-left side slope: " + sideSlope);
				Log.d("HexKey.horizntlContains", "Lower-left point slope: " + pointSlope);
				if (pointSlope <= sideSlope)
				{
					return true;
				}
			}
		}
		else { // Could be in bottom triangle
			if (x <= mBottom.x) {
				// We are in left half of the lower triangle if the
				// slope formed by the line from the (x, y) to the bottom
				// vertex is <= (more negative than) the slope from the
				// lower-left vertex to the bottom vertex. We take the
				// negative because the y-coordinate's sign is reversed.
				val sideSlope: Double = (-1.0) * (mLowerLeft.y - mBottom.y)/(mLowerLeft.x - mBottom.x)
				val pointSlope: Double = (-1.0) * (y - mBottom.y)/(x - mBottom.x)
				Log.d("HexKey.horizntlContains", "Lower-left side slope: " + sideSlope);
				Log.d("HexKey.horizntlContains", "Lower-left point slope: " + pointSlope);
				if (pointSlope <= sideSlope) {
					return true;
				}
			}
			else { // Check right half
				// We are in right half of the lower triangle if the
				// slope formed by the line from the (x, y) to the bottom
				// vertex is >= the slope from the lower-left vertex to 
				// the bottom vertex. We take the negative because the 
				// y-coordinate's sign is reversed.
				val sideSlope: Double = (-1.0) * (mLowerRight.y - mBottom.y)/(mLowerRight.x - mBottom.x)
				val pointSlope: Double = (-1.0) * (y - mBottom.y)/(x - mBottom.x)
				Log.d("HexKey.horizntlContains", "Lower-right side slope: " + sideSlope);
				Log.d("HexKey.horizntlContains", "Lower-right point slope: " + pointSlope);
				if (pointSlope >= sideSlope) {
					return true;
				}
			}
		}
		
		return false;
	}

	/** Check if a (touch) point should trigger this key (contained inside the critical points)*/
	fun verticalContains(x: Int, y: Int): Boolean {
		/*
		 * 
		 * We split the hexagon into three areas to determine if the specified coordinates
		 * are included. These are a "center-cut" rectangle, where most positive examples are
		 * expected, and a right and left triangle:
		        ______
               /|    |\
		      / |    | \
		      \ |    | /
		       \|____|/
		  
		  */
		if (x >= mLowerLeft.x && x <= mLowerRight.x &&
			y >= mUpperLeft.y && y <= mLowerLeft.y) {
			Log.d("HexKey.verticalContains", "Center cut");
			return true; // Center cut.
		}
		if (x < mMiddleLeft.x || x > mMiddleRight.x ||
			y < mUpperLeft.y || y > mLowerLeft.y) {
			return false; // Air ball.
		}
		if (x <= mUpperLeft.x) { // Could be in left "triangle."
			if (y <= mMiddleLeft.y) {
				// We are in upper half of the left triangle if the
				// slope formed by the line from the (x, y) to the upper-left
				// vertex is >= the slope from the middle-left vertex to the
				// upper-left vertex. We take the negative because the y-coordinate's
				// sign is reversed.
				val sideSlope: Double = (-1.0) *
						(mUpperLeft.y - mMiddleLeft.y)/(mUpperLeft.x - mMiddleLeft.x)
				val pointSlope: Double = (-1.0) * (mUpperLeft.y - y)/(mUpperLeft.x - x);
				
				Log.d("HexKey.verticalContains", "Upper-left side slope: " + sideSlope);
				Log.d("HexKey.verticalContains", "Upper-left point slope: " + pointSlope);
				
				if (pointSlope >= sideSlope) {
					return true;
				}
			}
			else {
				// We may be in the lower half of the left triangle.
				val sideSlope: Double = (-1.0) *
						(mLowerLeft.y - mMiddleLeft.y)/(mLowerLeft.x - mMiddleLeft.x);
				val pointSlope: Double = (-1.0) * (mMiddleLeft.y - y)/(mMiddleLeft.x - x);
				Log.d("HexKey.verticalContains", "Lower-left side slope: " + sideSlope);
				Log.d("HexKey.verticalContains", "Lower-left point slope: " + pointSlope);
				if (pointSlope >= sideSlope) {
					return true;
				}
			}
		}
		else { // Could be in right triangle
			if (y <= mMiddleRight.y) {
				// We are in upper half of the right triangle if the
				// slope formed by the line from the (x, y) to the upper-right
				// vertex is <= the slope from the middle-right
				// vertex to the upper-upper vertex. We take the negative because 
				// the y-coordinate's sign is reversed.
				val sideSlope: Double = (-1.0) *
						(mUpperRight.y - mMiddleRight.y)/(mUpperRight.x - mMiddleRight.x);
				val pointSlope: Double = (-1.0) *(mUpperRight.y - y)/(mUpperRight.x - x);
				Log.d("HexKey.verticalContains", "Upper-right side slope: " + sideSlope);
				Log.d("HexKey.verticalContains", "Upper-right point slope: " + pointSlope);
				if (pointSlope <= sideSlope) {
					return true;
				}
			}
			else {
				val sideSlope: Double = (-1.0) *
						(mLowerRight.y - mMiddleRight.y)/(mLowerRight.x - mMiddleRight.x);
				val pointSlope: Double = (-1.0) *(mMiddleRight.y - y)/(mMiddleRight.x - x);
				Log.d("HexKey.verticalContains", "Lower-right side slope: " + sideSlope);
				Log.d("HexKey.verticalContains", "Lower-right point slope: " + pointSlope);
				if (pointSlope <= sideSlope)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/** Check if a point is visible onscreen*/
	protected fun isPointVisible(P: Point): Boolean {
		if (P.x >= 0 && P.y >= 0 && P.x < HexKeyboard.mDisplayWidth && P.y < HexKeyboard.mDisplayHeight) {
			return true;
		} else {
			return false;
		}
	}
	
	/** Check if a key is visible onscreen by computing approximate boundaries
	 TODO: try to find a more precise way to compute the boundaries?
		   Some kittyCornerKey may be missing (but didn't witness such a case in my tests)
	*/
	fun isKeyVisible(): Boolean {
		// Computing the lower and higher bound in x and y dimensions
		val LeftmostTop: Point = if(::mMiddleLeft.isInitialized && mMiddleLeft.x < mLowerLeft.x) mMiddleLeft else mUpperLeft
		val LeftmostBottom: Point = if(::mMiddleLeft.isInitialized && mMiddleLeft.x < mLowerLeft.x) mMiddleLeft else mLowerLeft

		val RightmostTop: Point = if(::mMiddleRight.isInitialized && mMiddleRight.x > mLowerRight.x) mMiddleRight else mUpperRight
		val RightmostBottom: Point = if(::mMiddleRight.isInitialized && mMiddleRight.x > mLowerRight.x) mMiddleRight else mLowerRight

		val LowestBottom: Point = if (::mBottom.isInitialized && mBottom.y < mLowerLeft.y) mBottom else mLowerLeft
		val HighestTop: Point = if (::mTop.isInitialized && mTop.y > mUpperLeft.y) mTop else mUpperLeft
		
		// DEBUG: Print the computed boundaries
		// Log.d("HexKey::isKeyVisible", "HexKey boundaries: "+Integer.toString(mKeyNumber)+" DW:"+Integer.toString(HexKeyboard.mDisplayWidth)+" DH:"+Integer.toString(HexKeyboard.mDisplayHeight)+" T:"+Integer.toString(HighestTop.x)+";"+Integer.toString(HighestTop.y)+" B:"+Integer.toString(LowestBottom.x)+";"+Integer.toString(LowestBottom.y)+" LT:"+Integer.toString(LeftmostTop.x)+";"+Integer.toString(LeftmostTop.y)+" LB:"+Integer.toString(LeftmostBottom.x)+";"+Integer.toString(LeftmostBottom.y)+" RT:"+Integer.toString(RightmostTop.x)+";"+Integer.toString(RightmostTop.y)+" RB:"+Integer.toString(RightmostBottom.x)+";"+Integer.toString(RightmostBottom.y));
		
		// Computing visibility: if the coordinates of at least one of the lowest/highest bound point is inside the screen resolution, then the key is visible
		// Note: we need to check the coordinates of Points, not just x and y
		// (that's why we store the point and not just the x or y coordinate),
		// else the computation will be flawed (a point may have an x coordinate in the correct range,
		// but not y, which would place the point off-screen, below the screen)
		if ( this.isPointVisible(LeftmostTop)
				|| this.isPointVisible(LeftmostBottom)
				|| this.isPointVisible(RightmostTop)
				|| this.isPointVisible(RightmostBottom)
				|| this.isPointVisible(HighestTop)
				|| this.isPointVisible(LowestBottom)
				) {
			return true;
		} else {
			return false;
		}
	}
	
	open fun play() {
		// By default without argument, play a key with the maximum pressure
		this.play(HexKeyboard.mMaxPressure);
	}
	
	open fun play(pressure: Float) {
		// Play the new stream sound first before stopping the old one, avoids clipping (noticeable gap in sound between two consecutive press on the same note)
		// Note about sound clipping when first stopping previous stream then playing new stream:
		// it probably happens because there's some delay with SoundPool commands of about 100ms,
		// which means that the sound player has a small gap of time where there is absolutely no sound
		// (if only one same key is pressed several times),
		// thus the sound manager stops the sound driver, and then quickly reopens it to play the new sound,
		// which produces the sound clipping/popping/clicking.
		// The solution: start the new sound first and then stop the old one. Only one drawback:
		// it consumes a thread for nothing (may stop another note when we reach the maximum number in the pool).
		val newStreamId: MutableList<Int> = mInstrument.play(mMidiNoteNumber, pressure.toInt()/*FIXME: loss of precision*/).toMutableList()
		if (newStreamId[0] == -1) {return;} // May not yet be loaded.

		// Stop the previous stream sound
		if (mStreamId[0] != -1) {
			// If sustain, we want to force-stop the previous sound of this key
			// (else it will be a kind of reverb, plus we will get weird stuff like disabling sustain won't stop all sounds since we will loose the streamId for the keys we pressed twice!)
			if (HexKeyboard.mSustain == true || HexKeyboard.mSustainAlwaysOn == true) {
				// TODO: since the previous sound is stopped just before playing,
				//  a new bug appeared: sometimes when a key is pressed twice quickly,
				//  a clearly audible sound clipping happens!
				this.stop(true);
			// Else we don't stop the sound, just drop the Id
			// (the sound will just play until it ends then the stream will be closed) - useful for a future Reverb!
			} else {
				this.stop(); // better always stop if there is already a stream playing, no matter the reason
				//Log.e("HexKey::play", mMidiNoteNumber + ": Already playing and no sustain! Should not happen!");
			}
			// Else else, without Sustain, stop() will be called automatically upon release of the key
		}

		// Update old stream with the new one
		mStreamId = newStreamId;
		// Change the state and drawing of the key
		val pitchStr: String = mMidiNoteNumber.toString()
		Log.d("HexKey::play", pitchStr);
		this.setPressed(true);
		return;
	}
	
	fun stop() {
		this.stop(false);
	}

	// Function called everytime a key press is released
	// (and also called by play() to stop previous streams, particularly if sustain is enabled)
	open fun stop(force: Boolean) {
		if (mStreamId[0] == -1) {return;} // May not have been loaded when played.
		// Force stop the sound (don't just show the unpressed state drawing) if either we provide the force argument, or if sustained is disabled
		if (force == true or (HexKeyboard.mSustain == false && HexKeyboard.mSustainAlwaysOn == false)) {
			mInstrument.stop(mStreamId.toIntArray())
			mStreamId.clear()
			mStreamId.add(-1)
		} // Else, we will just change the drawing of the key (useful for ModifierKeys since they don't have any sound stream to stop, just the visual state of their key)
		// Change the state and drawing of the key
		val pitchStr = mMidiNoteNumber.toString()
		Log.d("HexKey::stop", pitchStr);
		this.setPressed(false);
		return;
	}
}
