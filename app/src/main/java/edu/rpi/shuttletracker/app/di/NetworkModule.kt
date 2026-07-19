package edu.rpi.shuttletracker.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.network.hasNetwork
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.remote.ApiUrlProvider
import edu.rpi.shuttletracker.data.remote.DataStoreApiUrlProvider
import edu.rpi.shuttletracker.data.remote.ShuttleApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideCacheInterceptor(
        @ApplicationContext context: Context,
    ): Interceptor =
        Interceptor { chain ->
            var request = chain.request()

            if (!context.hasNetwork()) {
                // 2 week cache for offline
                request =
                    request
                        .newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 14)
                        .removeHeader("Pragma")
                        .build()
            }
            chain.proceed(request)
        }

    @Provides
    @Singleton
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
            OkHttpClient
                .Builder()
                .cache(myCache)
                .addInterceptor(cacheInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        } else {
            OkHttpClient
                .Builder()
                .cache(myCache)
                .addInterceptor(cacheInterceptor)
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideDataStoreApiUrlProvider(
        userPreferences: UserPreferences,
        @ApplicationContext context: Context,
    ): DataStoreApiUrlProvider =
        DataStoreApiUrlProvider(
            userPreferences = userPreferences,
            defaultBaseUrl = context.getString(R.string.url_default),
        )

    @Provides
    @Singleton
    fun provideApiUrlProvider(provider: DataStoreApiUrlProvider): ApiUrlProvider = provider

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        apiUrlProvider: DataStoreApiUrlProvider,
    ): Retrofit {
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit
            .Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(apiUrlProvider.defaultBaseUrl)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideShuttleApi(retrofit: Retrofit): ShuttleApi = retrofit.create(ShuttleApi::class.java)
}
