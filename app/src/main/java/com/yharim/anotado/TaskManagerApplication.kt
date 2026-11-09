package com.yharim.anotado

import android.app.Application
import com.yharim.anotado.di.AppContainer

class TaskManagerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
