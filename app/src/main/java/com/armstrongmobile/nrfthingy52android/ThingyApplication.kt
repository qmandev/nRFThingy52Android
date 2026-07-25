package com.armstrongmobile.nrfthingy52android

import android.app.Application
import com.armstrongmobile.nrfthingy52android.di.AppContainer

// Process init and owner of the composition root, the counterpart to iOS's @main ThingyApp entry
// point (plan §3). Holding AppContainer here gives the scanner and detail ViewModels one place to
// resolve their transport from, without a DI framework.
class ThingyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
