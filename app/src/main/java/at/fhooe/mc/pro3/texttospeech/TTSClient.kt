package at.fhooe.mc.pro3.texttospeech

/**
 * Enables modularization for a TTSClient.
 */
interface TTSClient {
    var doneFetching : Boolean
    fun fetchData(text : String)
    fun shutdown()

}