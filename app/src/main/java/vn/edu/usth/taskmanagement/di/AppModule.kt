package vn.edu.usth.taskmanagement.di

import androidx.room.Room
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.google.firebase.database.FirebaseDatabase
import vn.edu.usth.taskmanagement.data.local.AppDatabase
import vn.edu.usth.taskmanagement.data.local.SessionManager
import vn.edu.usth.taskmanagement.data.remote.source.AuthRemoteDataSource
import vn.edu.usth.taskmanagement.data.remote.source.TaskRemoteDataSource
import vn.edu.usth.taskmanagement.data.remote.source.WorkspaceRemoteDataSource
import vn.edu.usth.taskmanagement.data.remote.source.UserRemoteDataSource
import vn.edu.usth.taskmanagement.data.repository.AuthRepositoryImpl
import vn.edu.usth.taskmanagement.data.repository.FirebaseRepositoryImpl
import vn.edu.usth.taskmanagement.data.repository.TaskRepositoryImpl
import vn.edu.usth.taskmanagement.data.repository.WorkspaceRepositoryImpl
import vn.edu.usth.taskmanagement.domain.repository.AuthRepository
import vn.edu.usth.taskmanagement.domain.repository.FirebaseRepository
import vn.edu.usth.taskmanagement.domain.repository.TaskRepository
import vn.edu.usth.taskmanagement.domain.repository.WorkspaceRepository
import vn.edu.usth.taskmanagement.presentation.alltask.AllTasksViewModel
import vn.edu.usth.taskmanagement.presentation.calendar.CalendarViewModel
import vn.edu.usth.taskmanagement.presentation.group.GroupDetailViewModel
import vn.edu.usth.taskmanagement.presentation.home.HomeViewModel
import vn.edu.usth.taskmanagement.presentation.auth.LoginViewModel
import vn.edu.usth.taskmanagement.presentation.myday.MyDayViewModel
import vn.edu.usth.taskmanagement.presentation.task.TaskDetailViewModel
import vn.edu.usth.taskmanagement.service.CalendarSyncManager

val appModule = module {
    // Local Session
    single { SessionManager(androidContext()) }
    single { CalendarSyncManager(androidContext()) }

    // Network — Ktor HTTP Client
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }.apply {
            plugin(HttpSend).intercept { request ->
                val token = get<SessionManager>().getToken()
                if (token != null) {
                    request.headers.append("Authorization", "Bearer $token")
                }
                // Bypass Localtunnel warning page
                request.headers.append("Bypass-Tunnel-Reminder", "true")
                // Bypass Ngrok warning page
                request.headers.append("ngrok-skip-browser-warning", "true")
                
                execute(request)
            }
        }
    }

    // Local Database — Room
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "task_management_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    single { get<AppDatabase>().workspaceDao() }
    single { get<AppDatabase>().taskDao() }

    // Data Sources
    single { TaskRemoteDataSource(get(), get()) }
    single { WorkspaceRemoteDataSource(get(), get()) }
    single { AuthRemoteDataSource(get(), get()) }
    single { UserRemoteDataSource(get(), get()) }

    // Repositories
    single<WorkspaceRepository> { WorkspaceRepositoryImpl(get(), get(), get()) }
    single<TaskRepository> { TaskRepositoryImpl(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    // Firebase
    single { FirebaseDatabase.getInstance("https://taskmanagement-9b1cf-default-rtdb.asia-southeast1.firebasedatabase.app") }
    single<FirebaseRepository> { FirebaseRepositoryImpl(get()) }

    // ViewModels
    viewModel { HomeViewModel(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { CalendarViewModel(get(), get()) }
    viewModel { AllTasksViewModel(get(), get()) }
    viewModel { GroupDetailViewModel(get(), get(), get()) }
    viewModel { TaskDetailViewModel(get(), get(), get(), get()) }
    viewModel { MyDayViewModel(get(), get()) }
}
