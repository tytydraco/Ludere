package com.draco.ludere.utils

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.draco.ludere.R
import com.draco.ludere.models.Storage
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable

class RetroView(private val context: Context) {
    private val storage = Storage.getInstance(context)
    private val compositeDisposable = CompositeDisposable()

    private val init = MutableLiveData(false)
    fun getInit(): LiveData<Boolean> = init

    private val ready = MutableLiveData(false)
    fun getReady(): LiveData<Boolean> = ready

    private val error = MutableLiveData(false)
    fun getError(): LiveData<Boolean> = error

    var view: GLRetroView? = null

    init {
        val retroViewData = GLRetroViewData(context).apply {
            coreFilePath = "libcore.so"
            gameFileBytes = context.resources.openRawResource(R.raw.rom).use { it.readBytes() }
            shader = GLRetroView.SHADER_SHARP
            variables = getCoreVariables()

            if (storage.sram.exists()) {
                storage.sram.inputStream().use {
                    saveRAMState = it.readBytes()
                }
            }
        }

        view = GLRetroView(context, retroViewData)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        view!!.layoutParams = params

        init.postValue(true)

        val renderDisposable = view!!
            .getGLRetroEvents()
            .takeUntil { ready.value == true }
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered &&
                        ready.value == false)
                    ready.postValue(true)
            }
        compositeDisposable.add(renderDisposable)

        val errorDisposable = view!!
            .getGLRetroErrors()
            .subscribe {
                view = null
                error.postValue(true)
            }
        compositeDisposable.add(errorDisposable)
    }

    private fun getCoreVariables(): Array<Variable> {
        val variables = arrayListOf<Variable>()
        val rawVariablesString = context.getString(R.string.config_variables)
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
    }
}