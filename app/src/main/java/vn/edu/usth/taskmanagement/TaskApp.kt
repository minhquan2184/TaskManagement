package vn.edu.usth.taskmanagement

import vn.edu.usth.taskmanagement.di.appModule

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin


class TaskApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@TaskApp)
            modules(appModule)
        }
    }
}

