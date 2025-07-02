package edu.rpi.shuttletracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.rpi.shuttletracker.data.network.ApiHelper
import edu.rpi.shuttletracker.data.network.ApiService
import edu.rpi.shuttletracker.data.repositories.UserPreferencesRepository
import edu.rpi.shuttletracker.util.FlattenTypeAdapterFactory
import edu.rpi.shuttletracker.util.hasNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val USER_PREFERENCES = "user_preferences"

@Module
@InstallIn(SingletonComponent::class)
object ShuttleTrackerModule {
    @Singleton
    @Provides
    fun provideCacheInterceptor(
        @ApplicationContext context: Context,
    ): Interceptor =
        Interceptor { chain ->
            var request = chain.request()

            if (!context.hasNetwork()) {
                // 2 week cache for offline
                request =
                    request.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 14)
                        .removeHeader("Pragma")
                        .build()
            }
            chain.proceed(request)
        }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        cacheInterceptor: Interceptor,
    ): OkHttpClient {
        // 5 mb of cache
        val cacheSize = (5 * 1024 * 1024).toLong()
        val myCache = Cache(context.cacheDir, cacheSize)

        return if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()

            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            OkHttpClient.Builder()
                .cache(myCache)
                .addInterceptor(cacheInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        } else {
            OkHttpClient.Builder()
                .cache(myCache)
                .addInterceptor(cacheInterceptor)
                .build()
        }
    }

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        userPreferencesRepository: UserPreferencesRepository,
    ): Retrofit {
        val gson =
            GsonBuilder()
                .registerTypeAdapterFactory(FlattenTypeAdapterFactory())
                .create()

        val url =
            runBlocking {
                return@runBlocking userPreferencesRepository.getBaseUrl().first()
            }

        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .baseUrl(url)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideApiHelper(apiHelper: ApiHelper): ApiHelper = apiHelper

    @Singleton
    @Provides
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler =
                ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() },
                ),
            migrations = listOf(SharedPreferencesMigration(context, USER_PREFERENCES)),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES) },
        )
}
