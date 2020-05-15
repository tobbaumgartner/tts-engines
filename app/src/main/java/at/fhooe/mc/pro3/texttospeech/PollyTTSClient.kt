package at.fhooe.mc.pro3.texttospeech

import android.util.Log
import android.view.View
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.polly.AmazonPollyPresigningClient
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

class PollyTTSClient(private val notifier: StatusNotifier) : TTSClient{
    override var doneFetching: Boolean = false
    /**
     * Cognito pool ID. Pool needs to be unauthenticated pool with
     * Amazon Polly permissions.
     */
    private val COGNITO_POOL_ID = ""

    /**
     * Region of Amazon Polly.
     */
    private val MY_REGION = Regions.EU_WEST_1

    /**
     * Instance of connection to AWS.
     */
    var pollyClient : AmazonPollyPresigningClient


    init {
        // Initialize the Amazon Cognito credentials provider.
        val credentialsProvider = CognitoCachingCredentialsProvider(
            Application.getContext(),
            COGNITO_POOL_ID,
            MY_REGION
        )
        // Create a pollyClient that supports generation of presigned URLs.
        pollyClient = AmazonPollyPresigningClient(credentialsProvider)
        // Create describe voices request.
        //val describeVoicesRequest = DescribeVoicesRequest()

        // Synchronously ask Amazon Polly to describe available TTS voices.
        //val describeVoicesResult = pollyClient.describeVoices(describeVoicesRequest)
        //val voices = describeVoicesResult.voices
    }

    override fun fetchData(text: String) {
        val beforetime = System.currentTimeMillis()
        // Create speech synthesis request.
        val synthesizeSpeechPresignRequest = SynthesizeSpeechPresignRequest()
            // Set the text to synthesize.
            .withText(text)
            // Select voice for synthesis.
            //.withVoiceId(voices[0].id) // "Joanna"
            .withVoiceId("Joanna")
            // Set format to MP3.
            .withOutputFormat(OutputFormat.Mp3)

        // Get the presigned URL for synthesized speech audio stream.
        val pollyRequest = CoroutineScope(Dispatchers.IO).launch {
            try {
                val presignedSynthesizeSpeechUrl =  pollyClient.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest)
                Log.i("TEST", presignedSynthesizeSpeechUrl.toString())

                val connection = presignedSynthesizeSpeechUrl.openConnection()
                connection.connect()
                val inputStream = BufferedInputStream(presignedSynthesizeSpeechUrl.openStream())
                withContext(Dispatchers.Main) {
                    notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_AMAZON,"Amazon Polly response received!")
                }

                val outputStream = FileOutputStream(File(Application.getContext().filesDir.path.toString() +"amazonPolly.mp3"))
                inputStream.copyTo(outputStream)

                withContext(Dispatchers.Main) {
                    notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_AMAZON,"Amazon Polly response stored as MP3!")

                    val aftertime = System.currentTimeMillis()
                    notifier.statusUpdate(Application.STATUS_PING,Application.ENGINE_AMAZON,(aftertime - beforetime).toString() + " ms")
                    notifier.updateVisability(Application.ENGINE_AMAZON)
                }
            } catch (e : Exception) {
                e.printStackTrace()
                notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_AMAZON,"Error in Amazon-Polly request.")
                notifier.updateVisability(Application.MUTE_VIEW)
            }
        }
    }

    override fun shutdown() {
        pollyClient.shutdown()
    }
}