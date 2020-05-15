package at.fhooe.mc.pro3.texttospeech

/**
 * Enables observer pattern for UI-status updates.
 */
interface StatusNotifier {

    fun statusUpdate(status : Int, engine : Int, text : String)

    fun updateVisability(visibility : Int)
}