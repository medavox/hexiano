package opensource.hexiano

import java.io.File

/**Sound to load in SoundPool, being a tuple of
 * [midiNoteNumber, velocity, file resource id int or string file path]*/
data class SoundLoadingTuple(
    val midiNoteNumber: Int,
    val velocity: Int,
    val resource: LoadingResource
) {
    sealed class LoadingResource {
        abstract fun getResource(): File

        class ExternalFilePath(val path: String) : LoadingResource() {
            override fun getResource(): File {
                TODO("Not yet implemented")
            }
        }
        class AndroidResourceId(val id: Int) : LoadingResource() {
            override fun getResource(): File {
                TODO("Not yet implemented")
            }
        }
    }
}
