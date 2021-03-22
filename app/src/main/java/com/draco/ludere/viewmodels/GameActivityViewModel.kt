package com.draco.ludere.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.draco.ludere.utils.GamePad
import com.draco.ludere.utils.GamePadConfig
import com.draco.ludere.utils.RetroView
import io.reactivex.disposables.CompositeDisposable

class GameActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val compositeDisposable = CompositeDisposable()

    val gamePadConfig = GamePadConfig(context, context.resources)

    fun subscribeGamePad(gamePad: GamePad, retroView: RetroView) {
        gamePad.subscribe(compositeDisposable, retroView.view)
    }

    fun dispose() {
        compositeDisposable.dispose()
    }
}