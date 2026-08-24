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

/** Provides the shared HTTP cache, client, JSON converter, and Shuttle API. */
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

            if (request.url.pathSegments.lastOrNull() in LivePolledPaths) {
                // Live vehicle data must never be replayed from cache.
                request = request.newBuilder().header("Cache-Control", "no-store").build()
            } else if (!context.hasNetwork()) {
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
        val cacheSize = (5 * 1024 * 1024).toLong()
        val myCache = Cache(context.cacheDir, cacheSize)

        return OkHttpClient
            .Builder()
            .cache(myCache)
            .addInterceptor(cacheInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context,
    ): Retrofit {
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit
            .Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(context.getString(R.string.url_default))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideShuttleApi(retrofit: Retrofit): ShuttleApi = retrofit.create(ShuttleApi::class.java)
}

/** Live endpoints that must bypass the HTTP cache. */
private val LivePolledPaths = setOf("locations", "etas", "velocities")
