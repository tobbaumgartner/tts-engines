package at.fhooe.mc.pro3.texttospeech

import android.content.ContentValues.TAG
import android.util.Log
import android.view.View
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.texttospeech.v1.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class GoogleCloudTTSClient(private val notifier: StatusNotifier) : TTSClient{

    /**
     * Instance of connection to Google Cloud TTS. Authentication is done via credential.json file
     */
    var gcTTS : TextToSpeechClient
    override var doneFetching: Boolean = false
    init {
        val textToSpeechSettings = TextToSpeechSettings.newBuilder()
            .setCredentialsProvider(
                FixedCredentialsProvider
                    .create(
                        ServiceAccountCredentials
                            .fromStream((Application.getContext().resources.openRawResource(R.raw.credential)))))
            .build()
        gcTTS = TextToSpeechClient.create(textToSpeechSettings)
    }

    override fun fetchData(text: String) {
        val beforetime = System.currentTimeMillis()
        val input = SynthesisInput.newBuilder()
            .setText(text)
            .build()

        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode("en-US")
            .setSsmlGender(SsmlVoiceGender.NEUTRAL)
            .build()

        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.MP3)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gcTTS.synthesizeSpeech(input,voice,audioConfig)
                withContext(Dispatchers.Main) {
                    notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_GOOGLE,"Google Cloud response received!")
                    val audioContents = response.audioContent
                    Log.i("TEST", audioContents.toString())
                    val outputStream = FileOutputStream(File(Application.getContext().filesDir.path.toString() +"googleCloud.mp3"))
                    outputStream.write(audioContents.toByteArray())
                    notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_GOOGLE,"Google Cloud response stored as MP3!")
                    doneFetching = true
                    val aftertime = System.currentTimeMillis()
                    notifier.statusUpdate(Application.STATUS_PING,Application.ENGINE_GOOGLE,(aftertime - beforetime).toString() + " ms")
                    notifier.updateVisability(Application.ENGINE_GOOGLE)

                }
            } catch (e : Exception
            ) {
                Log.e(TAG,"Error in Google Request!")
                notifier.statusUpdate(Application.STATUS_LOG,Application.ENGINE_GOOGLE,"Error in Google-Cloud request.")
                notifier.updateVisability(Application.MUTE_VIEW)
            }
        }
    }

    override fun shutdown() {
        gcTTS.shutdown()
        gcTTS.close()
    }
}

