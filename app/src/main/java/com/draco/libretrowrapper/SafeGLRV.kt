package com.draco.libretrowrapper

import android.os.Handler
import android.os.Looper
import com.swordfish.libretrodroid.GLRetroView
import io.reactivex.disposables.CompositeDisposable

class SafeGLRV(
    val unsafeGLRetroView: GLRetroView,
    compositeDisposable: CompositeDisposable
) {
    var isSafe = false

    init {
        /* Wait 100ms after first frame has rendered to declare readiness */
        val renderDisposable = unsafeGLRetroView
            .getGLRetroEvents()
            .takeUntil { isSafe }
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered)
                    Handler(Looper.getMainLooper()).postDelayed({
                        isSafe = true
                    }, 100)
            }
        compositeDisposable.add(renderDisposable)
    }

    /* Handle the GLRetroView safely */
    fun safe(handler: (retroView: GLRetroView) -> Unit) {
        Thread {
            while (!isSafe)
                Thread.sleep(50)
            handler(unsafeGLRetroView)
        }.start()
    }
}