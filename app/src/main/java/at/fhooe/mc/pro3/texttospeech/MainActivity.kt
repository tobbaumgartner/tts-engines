package at.fhooe.mc.pro3.texttospeech

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

import java.util.*
import android.media.MediaPlayer
import android.media.AudioManager
import android.view.View
import java.io.IOException


/**
 * Main activity that controles the engines.
 */
class MainActivity : AppCompatActivity(), StatusNotifier {

    /**
     * Instance of iternal TTS engine.
     */
    lateinit var ttsIntern : TextToSpeech

    val TAG = "MAIN"

    private lateinit var googleClient : GoogleCloudTTSClient
    private lateinit var pollyClient : PollyTTSClient
    private lateinit var azureClient : AzureTTSClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Application.setContext(applicationContext)

        setUpTTSIntern()

        googleClient = GoogleCloudTTSClient(this)
        pollyClient = PollyTTSClient(this)
        azureClient = AzureTTSClient(this)

        main_activity_button_tts.setOnClickListener {
            updateVisability(Application.SHOW_VIEW)
            val data = main_activity_textinput.text.toString()
            googleClient.doneFetching = false
            pollyClient.doneFetching = false
            azureClient.doneFetching = false
            googleClient.fetchData(data)
            pollyClient.fetchData(data)
            azureClient.fetchData(data)
        }

        main_activity_imageview_google.setOnClickListener {
            playCloudAudio(Application.ENGINE_GOOGLE)
        }
        main_activity_imageview_amazon.setOnClickListener {
            playCloudAudio(Application.ENGINE_AMAZON)
        }
        main_activity_imageview_microsoft.setOnClickListener {
            playCloudAudio(Application.ENGINE_MICROSOFT)
        }
        main_activity_imageview_internal.setOnClickListener {
            playTTSIntern()
        }
    }

    override fun statusUpdate(status: Int, engine: Int, text: String) {
        when(status) {
            Application.STATUS_LOG -> main_activity_textview_statusupdate.text = text
            Application.STATUS_PING -> {
                when(engine) {
                    Application.ENGINE_GOOGLE -> main_activity_textview_responsegoogle.text = text
                    Application.ENGINE_AMAZON -> main_activity_textview_responseamazon.text = text
                    Application.ENGINE_MICROSOFT -> main_activity_textview_responseazure.text = text
                }
            }
        }

    }
    override fun updateVisability(visibility: Int) {
        when(visibility) {
            Application.SHOW_VIEW -> main_activity_progressBar.visibility = View.VISIBLE
            Application.MUTE_VIEW -> main_activity_progressBar.visibility = View.INVISIBLE
            Application.ENGINE_GOOGLE -> {
                googleClient.doneFetching = true
                if(pollyClient.doneFetching && azureClient.doneFetching) {
                    main_activity_progressBar.visibility = View.INVISIBLE
                }
            }
            Application.ENGINE_AMAZON -> {
                pollyClient.doneFetching = true
                if(googleClient.doneFetching && azureClient.doneFetching) {
                    main_activity_progressBar.visibility = View.INVISIBLE
                }
            }
            Application.ENGINE_MICROSOFT -> {
                azureClient.doneFetching = true
                if(pollyClient.doneFetching && googleClient.doneFetching) {
                    main_activity_progressBar.visibility = View.INVISIBLE
                }
            }
        }
    }

    /**
     * Setting up internal TTS-Engine.
     */
    private fun setUpTTSIntern() {
        ttsIntern = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if(status == TextToSpeech.SUCCESS) {
                val result = ttsIntern.setLanguage(Locale.ENGLISH)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG,"Error at internal TTS language.")
                } else {
                    main_activity_button_tts.isEnabled = true
                }
            } else {
                Log.e(TAG,"Init of internal TTS failed.")
            }
        })
    }

    /**
     * Play Google-MP3.
     */
    private fun playCloudAudio(engine : Int) {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        try {

            // Set media player's data source to previously obtained URL.
            when(engine) {
                Application.ENGINE_GOOGLE -> mediaPlayer.setDataSource(applicationContext.filesDir.path.toString() +"googleCloud.mp3")
                Application.ENGINE_AMAZON -> mediaPlayer.setDataSource(applicationContext.filesDir.path.toString() +"amazonPolly.mp3")
                Application.ENGINE_MICROSOFT -> mediaPlayer.setDataSource(applicationContext.filesDir.path.toString() +"azure.wav")
            }

        } catch (e: IOException) {
            Log.e("MainActivity", "Unable to set data source for the media player! " + e.message)
        }
        // Prepare the MediaPlayer asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync()

        // Set the callback to start the MediaPlayer when it's prepared.
        mediaPlayer.setOnPreparedListener { mep -> mep.start() }

        // Set the callback to release the MediaPlayer after playback is completed.
        mediaPlayer.setOnCompletionListener { mep -> mep.release() }
    }


    /**
     * Play internal TTS.
     */
    private fun playTTSIntern() {
        val beforetime = System.currentTimeMillis()
        ttsIntern.speak(main_activity_textinput.text.toString(),TextToSpeech.QUEUE_FLUSH,null,"TEST")
        val aftertime = System.currentTimeMillis()
        main_activity_textview_responseinternal.text = (aftertime - beforetime).toString() + " ms"
    }

    /**
     * Stop all engines.
     */
    override fun onDestroy() {
        ttsIntern.stop()
        ttsIntern.shutdown()

        googleClient.shutdown()
        pollyClient.shutdown()
        azureClient.shutdown()

        super.onDestroy()
    }
}
