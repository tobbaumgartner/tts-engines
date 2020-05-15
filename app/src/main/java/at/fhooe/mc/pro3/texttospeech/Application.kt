package at.fhooe.mc.pro3.texttospeech

import android.content.Context

abstract class Application {

    companion object {

        private lateinit var context: Context
        const val ENGINE_GOOGLE = 1
        const val ENGINE_AMAZON = 2
        const val ENGINE_MICROSOFT = 3
        const val STATUS_LOG = 1
        const val STATUS_PING = 2
        const val MUTE_VIEW = 4
        const val SHOW_VIEW = 5
        fun setContext(con: Context) {
            context=con
        }

        fun getContext() : Context {
            return context
        }
    }
}