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
        class ExternalFilePath(val path: String) : LoadingResource()
        class AndroidResourceId(val id: Int) : LoadingResource()
    }
}
