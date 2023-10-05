package io.github.arononak.githubactionswearos

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.github.arononak.githubactionswearos.presentation.NotificationController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.Locale

interface GithubService {
    @GET("/repos/{owner}/{repo}/actions/runs")
    suspend fun fetchWorkflowRuns(
        @Header("X-GitHub-Api-Version") xGitHubApiVersion: String = "2022-11-28",
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("Authorization") authorization: String? = "application/vnd.github+json",
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): WorkflowRunsResponse

    data class WorkflowRunsResponse(val totalCount: Int, val workflowRuns: List<Run>) {
        data class Run(val id: Long, val status: String, val conclusion: String) {
            override fun toString(): String {
                return "$status $conclusion"
                    .lowercase(Locale.ROOT)
                    .replace("null", "")
                    .replace("_", " ")
                    .trim()
            }
        }
    }
}

fun createRetrofit(): Retrofit {
    val loggingInterceptor = HttpLoggingInterceptor()
        .setLevel(HttpLoggingInterceptor.Level.BODY)

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setLenient()
        .create()

    return Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()
}

class SettingsRepository(app: Application) {
    private val prefs = app.getSharedPreferences("github-actions-prefs", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) = prefs.edit { putString("token", value) }

    var owner: String
        get() = prefs.getString("owner", "") ?: ""
        set(value) = prefs.edit { putString("owner", value) }

    var repo: String
        get() = prefs.getString("repo", "") ?: ""
        set(value) = prefs.edit { putString("repo", value) }

    var refreshTime: Int
        get() = prefs.getInt("refreshTime", 5)
        set(value) = prefs.edit { putInt("refreshTime", value) }

    fun fetch() = Settings(token, owner, repo, refreshTime)

    data class Settings(
        val token: String,
        val owner: String,
        val repo: String,
        val refreshTime: Int,
    )
}

class GithubRepository {
    private val githubService: GithubService by lazy {
        createRetrofit().create(GithubService::class.java)
    }

    suspend fun fetchStatus(settings: SettingsRepository.Settings): String? {
        val (token, owner, repo) = settings

        val authorization = if (token.isEmpty()) null else "Bearer $token"

        return try {
            val runs = githubService.fetchWorkflowRuns(
                authorization = authorization,
                owner = owner,
                repo = repo,
            )

            runs.workflowRuns.first().toString()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            null
        }
    }
}

class GithubViewModel(private val app: Application) : AndroidViewModel(app) {
    private val settingsRepository = SettingsRepository(app)
    private val githubRepository = GithubRepository()

    val state = MutableStateFlow(State())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val settings = settingsRepository.fetch()
                state.value = state.value.copy(settings = settings)

                refreshData(settings)
                delay(settings.refreshTime * 1000L)
            }
        }
    }

    fun onChangedOwner(owner: String) {
        state.value.settings ?: return

        state.value = state.value.copy(settings = state.value.settings!!.copy(owner = owner))
        settingsRepository.owner = owner
    }

    fun onChangedRepo(repo: String) {
        state.value.settings ?: return

        state.value = state.value.copy(settings = state.value.settings!!.copy(repo = repo))
        settingsRepository.repo = repo
    }

    fun onChangedToken(token: String) {
        state.value.settings ?: return

        state.value = state.value.copy(settings = state.value.settings!!.copy(token = token))
        settingsRepository.token = token
    }

    fun onRefreshTimeChanged(refreshTime: Int) {
        state.value.settings ?: return

        state.value =
            state.value.copy(settings = state.value.settings!!.copy(refreshTime = refreshTime))
        settingsRepository.refreshTime = refreshTime
    }

    private suspend fun refreshData(settings: SettingsRepository.Settings) {
        if (settings.owner.isNotEmpty() && settings.repo.isNotEmpty()) {
            val previousStatus = state.value.status
            val currentStatus = githubRepository.fetchStatus(settings)

            if (previousStatus == "in progress") {
                if (currentStatus?.contains("completed") == true) {
                    val success = currentStatus.contains("success")

                    NotificationController.showCompletedBuild(app, success)
                }
            }

            val newStatus = currentStatus ?: "Loading"
            state.value = state.value.copy(status = newStatus)
        }
    }

    data class State(
        val status: String = "Loading",
        val settings: SettingsRepository.Settings? = null,
    )
}
