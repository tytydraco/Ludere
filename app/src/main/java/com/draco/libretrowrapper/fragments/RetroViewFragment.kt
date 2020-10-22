package com.draco.libretrowrapper.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.draco.libretrowrapper.R
import com.draco.libretrowrapper.utils.PrivateData
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.CountDownLatch

class RetroViewFragment : Fragment() {
    /* Essential objects */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var privateData: PrivateData

    /* Panic dialog for if we crash */
    private lateinit var panicDialog: AlertDialog

    /* Emulator objects */
    var retroView: GLRetroView? = null

    /* Latch that gets decremented when the GLRetroView renders a frame */
    val retroViewReadyLatch = CountDownLatch(1)

    /* Store all observable subscriptions */
    private val compositeDisposable = CompositeDisposable()

    /* Shared preference keys */
    private val fastForwardEnabledString = "fast_forward_enabled"
    private val audioEnabledString = "audio_enabled"

    override fun onAttach(context: Context) {
        super.onAttach(context)

        /* Setup essential objects */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        privateData = PrivateData(context)

        /* Prepare the SRAM bytes if the file exists */
        var saveBytes = byteArrayOf()
        if (privateData.save.exists()) {
            val saveInputStream = privateData.save.inputStream()
            saveBytes = saveInputStream.readBytes()
            saveInputStream.close()
        }

        /* Create the GLRetroView */
        retroView = GLRetroView(
            context,
            privateData.core.absolutePath,
            privateData.rom.absolutePath,
            saveRAMState = saveBytes,
            shader = GLRetroView.SHADER_SHARP,
            variables = getCoreVariables()
        )

        /* Also start tracking any errors we come across */
        val errorDisposable = retroView!!
            .getGLRetroErrors()
            .subscribe {
                val errorMessage = when (it) {
                    GLRetroView.ERROR_LOAD_LIBRARY -> R.string.panic_message_load_core
                    GLRetroView.ERROR_LOAD_GAME -> R.string.panic_message_load_game
                    GLRetroView.ERROR_GL_NOT_COMPATIBLE -> R.string.panic_message_gles
                    else -> null
                }

                /* Fatal error, panic accordingly */
                if (errorMessage != null)
                    activity?.runOnUiThread { panic(errorMessage) }
            }
        compositeDisposable.add(errorDisposable)

        /* Prepare skeleton of panic dialog */
        panicDialog = AlertDialog.Builder(context)
            .setTitle(getString(R.string.panic_title))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.button_exit)) { _, _ -> activity?.finishAffinity() }
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_retroview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Hook the GLRetroView to the fragment lifecycle */
        lifecycle.addObserver(retroView!!)

        /* Center the view in the parent container */
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        retroView!!.layoutParams = params

        (view as FrameLayout).addView(retroView)

        /* Start tracking the frame state of the GLRetroView */
        val renderDisposable = retroView!!
            .getGLRetroEvents()
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered)
                    retroViewReadyLatch.countDown()
            }
        compositeDisposable.add(renderDisposable)

        Thread {
            retroViewReadyLatch.await()

            /* Restore emulator settings from last launch */
            restoreSettings()
        }.start()
    }

    private fun panic(errorResId: Int) {
        with (panicDialog) {
            setMessage(getString(errorResId))
            show()
        }
    }

    private fun getCoreVariables(): Array<Variable> {
        /* Parse the variables string into a Variable array */
        val variables = arrayListOf<Variable>()
        val rawVariablesString = getString(R.string.config_variables)
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
    }

    private fun restoreSettings() {
        retroView!!.fastForwardEnabled = sharedPreferences.getBoolean(fastForwardEnabledString, false)
        retroView!!.audioEnabled = sharedPreferences.getBoolean(audioEnabledString, true)
    }

    private fun saveSettings() {
        with (sharedPreferences.edit()) {
            putBoolean(fastForwardEnabledString, retroView!!.fastForwardEnabled)
            putBoolean(audioEnabledString, retroView!!.audioEnabled)
            apply()
        }
    }

    override fun onStop() {
        /* Save emulator settings for next launch */
        saveSettings()

        /* Save SRAM to the disk only if the emulator was able to render a frame */
        if (retroViewReadyLatch.count == 0L) {
            with(privateData.save.outputStream()) {
                write(retroView?.serializeSRAM())
                close()
            }
        }

        super.onStop()
    }

    override fun onDestroy() {
        /* Dismiss the panic dialog to avoid leaking the window */
        if (panicDialog.isShowing)
            panicDialog.dismiss()

        compositeDisposable.dispose()
        super.onDestroy()
    }
}