package at.fhooe.mc.pro3.texttospeech

import android.view.View
import com.microsoft.cognitiveservices.speech.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.Writer

class AzureTTSClient(private val notifier: StatusNotifier) : TTSClient {
    /**
     * 32-hex subscription key of azure service (required for authentication, billing, ...)
     */
    private val azureSpeechSubscriptionKey = ""

    /**
     * region of azure service; e.g. 'westeurope'
     */
    private val azureServiceRegion = ""

    /**
     * file name of .wav containing the synthesized speech; e.g. 'azure.wav'
     */
    private val azureAudioFileName = ""
    private var synth : SpeechSynthesizer
    private var config : SpeechConfig
    override var doneFetching: Boolean = false
    init {
        config = SpeechConfig.fromSubscription(azureSpeechSubscriptionKey, azureServiceRegion)
        val x = config.speechSynthesisVoiceName
        FileOutputStream(File(Application.getContext().filesDir.path.toString() + azureAudioFileName))
        val audioOutput = com.microsoft.cognitiveservices.speech.audio.AudioConfig.fromWavFileInput(Application.getContext().filesDir.path.toString() + azureAudioFileName)

        synth = SpeechSynthesizer(config, audioOutput)
    }

    override fun fetchData(text: String) {
        //val audioOutput = com.microsoft.cognitiveservices.speech.audio.AudioConfig.fromWavFileInput(Application.getContext().filesDir.path.toString() + azureAudioFileName)
        val audioOutput = com.microsoft.cognitiveservices.speech.audio.AudioConfig.fromWavFileInput(Application.getContext().filesDir.path.toString() + azureAudioFileName)
        synth = SpeechSynthesizer(config, audioOutput)

        val beforetime = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = synth.SpeakTextAsync(text)
                val result = task.get()
                withContext(Dispatchers.Main) {
                    if (result.reason == ResultReason.SynthesizingAudioCompleted) {

                        notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_MICROSOFT,"Microsoft Azure response received!")
                        notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_MICROSOFT,"Microsoft Azure response stored as WAV!")
                        notifier.updateVisability(Application.ENGINE_MICROSOFT)

                    } else if (result.reason == ResultReason.Canceled) {
                        notifier.updateVisability(Application.MUTE_VIEW)
                        val cancellation = SpeechSynthesisCancellationDetails.fromResult(result)
                        notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_MICROSOFT,"Azure CANCELED: Reason=" + cancellation.reason)

                        if (cancellation.reason == CancellationReason.Error) {
                            notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_MICROSOFT,"Azure CANCELED: ErrorCode=" + cancellation.errorCode)
                            notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_MICROSOFT,"Azure CANCELED: ErrorDetails=" + cancellation.errorDetails)
                        }
                    }
                    val aftertime = System.currentTimeMillis()
                    notifier.statusUpdate(Application.STATUS_PING,Application.ENGINE_MICROSOFT,(aftertime - beforetime).toString() + " ms")
                }

            } catch (e : Exception) {
                notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_MICROSOFT,"Error in Microsoft-Azure request.")
                notifier.updateVisability(Application.MUTE_VIEW)
            }
        }
    }

    override fun shutdown() {
        synth.close()
        config.close()

    }


}