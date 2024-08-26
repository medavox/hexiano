package opensource.hexiano

/**Basically themes for the app */
enum class ColorTheme(
    val mBlankColor: Int,
    val mBlackColor: Int,
    val mBlackHighlightColor: Int,
    val mWhiteColor: Int,
    val mWhiteHighlightColor: Int,
    val mTextColor: Int,
    val mOutlineColor: Int,
    val mPressedColor: Int,
    var mSpecialColor: Int = 0 // For special keys (eg: Sustain)
) {
    Khaki(
        mBlankColor = 0xFF000000.toInt(), // Black.
        mBlackColor = 0xFF843C24.toInt(), // Brown.
        mBlackHighlightColor = 0xFFD2691E.toInt(), // Chocolate.
        //mBlackHighlightColor = 0xFFA0522D; // Sienna.
        mWhiteColor = 0xFFF0E68C.toInt(), // Khaki.
        mWhiteHighlightColor = 0xFFBDB76B.toInt(), // Dark khaki.
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFF000000.toInt(), // Black.
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFFFF9900.toInt(), // Golden yellow.
    ),
    Azure(
        mBlankColor = 0xFF000000.toInt(), // Black.
        mBlackColor = 0xFF4682B4.toInt(), // Steel blue.
        mBlackHighlightColor = 0xFF5F9EA0.toInt(), // Cadet blue.
        mWhiteColor = 0xFFF0FFFF.toInt(), // Azure.
        mWhiteHighlightColor = 0xFFAFEEEE.toInt(), // Pale turquoise.
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFF000000.toInt(), // Black.
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFF8B70B3.toInt(), // Purple.
    ),
    White( // TODO: Remove 'White' preference when appropriate.
        mBlankColor = 0xFF000000.toInt(), // Black.
        mBlackColor = 0xFF2F4F4F.toInt(), // Dark slate grey.
        mBlackHighlightColor = 0xFF708090.toInt(), // Slate grey.
        mWhiteColor = 0xFFFFFFFF.toInt(), // White.
        mWhiteHighlightColor = 0xFFC0C0C0.toInt(), // Silver.
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFF000000.toInt(), // Black.
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFF336699.toInt(), // Light blue.
    ),
    Silhouette(
        mBlankColor = 0xFFFFFFFF.toInt(), // White.
        mBlackColor = 0xFF000000.toInt(), // Black.
        mBlackHighlightColor = 0xFF696969.toInt(), // Dim grey.
        mWhiteColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mWhiteHighlightColor = 0xFFD3D3D3.toInt(), // Light grey.
        mOutlineColor = 0xFFFFFFFF.toInt(), // White.
        mTextColor = 0xFFFFFFFF.toInt(), // White.
        mPressedColor = 0xFFFFFFFF.toInt(), // White.
        mSpecialColor = 0xFF336699.toInt(), // Light blue.
    ),
    GreyAndWhite(
        mBlankColor = 0xFF000000.toInt(), // Black.
        mBlackColor = 0xFF555555.toInt(),
        mBlackHighlightColor = 0xFF666666.toInt(),
        mWhiteColor = 0xFFFFFFFF.toInt(), // White.
        mWhiteHighlightColor = 0xFFCCCCCC.toInt(),
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFF000000.toInt(), // Black.
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFF336699.toInt(), // Light blue.
    ),
    EbonyAndIvory(
        mBlankColor = 0xFF432620.toInt(), // WP:Piano case sample.
        //mBlankColor = 0xFF673a31; // WP:Piano case sample 2.
        //mBlackColor = 0xFF162632; // WT:Ebony.
        mBlackColor = 0xFF382c25.toInt(), // WP:Ebony sample.
        //mBlackColor = 0xFF544238; // Lightened WP:Ebony sample.
        mBlackHighlightColor = 0xFF382c25.toInt(), // WP:Ebony sample.
        mWhiteColor = 0xFFFFFFEE.toInt(), // Ivory.
        mWhiteHighlightColor = 0xFFFFFFEE.toInt(), // Ivory.
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFF666666.toInt(),
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFFFF9900.toInt(), // Golden yellow.
    ),
    Blank(
        mBlankColor = 0xFFFFFFFF.toInt(), // White.
        mBlackColor = 0xFFFFFFFF.toInt(), // White.
        mBlackHighlightColor = 0xFFFFFFFF.toInt(), // White.
        mWhiteColor = 0xFFFFFFFF.toInt(), // White.
        mWhiteHighlightColor = 0xFFFFFFFF.toInt(), // White.
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFFFFFFFF.toInt(), // White.
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFF336699.toInt(), // Light blue.
    ),
    /**Default: Azure.
     * Fail to default colour scheme if saved preference doesn't have a match*/
    Default(
        mBlankColor = 0xFF000000.toInt(), // Black.
        mBlackColor = 0xFF4682B4.toInt(), // Steel blue.
        mBlackHighlightColor = 0xFF5F9EA0.toInt(), // Cadet blue.
        mWhiteColor = 0xFFF0FFFF.toInt(), // Azure.
        mWhiteHighlightColor = 0xFFAFEEEE.toInt(), // Pale turquoise.
        mOutlineColor = 0xFF000000.toInt(), // Black.
        mTextColor = 0xFF000000.toInt(), // Black.
        mPressedColor = 0xFFA9A9A9.toInt(), // Dark grey.
        mSpecialColor = 0xFF8B70B3.toInt(), // Purple.
    )
}