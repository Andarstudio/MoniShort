package app.monishort

import android.app.Application
import android.content.Context

class MoniShortApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }

    companion object {
        fun container(context: Context): AppContainer = (context.applicationContext as MoniShortApp).container
    }
}
