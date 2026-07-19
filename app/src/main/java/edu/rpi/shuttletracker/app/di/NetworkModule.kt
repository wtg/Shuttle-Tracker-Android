package edu.rpi.shuttletracker.app.di

import android.content.Context
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.rpi.shuttletracker.BuildConfig
import edu.rpi.shuttletracker.R
import edu.rpi.shuttletracker.core.network.hasNetwork
import edu.rpi.shuttletracker.core.network.normalizeBaseUrl
import edu.rpi.shuttletracker.data.local.preferences.UserPreferences
import edu.rpi.shuttletracker.data.remote.ShuttleApi
import edu.rpi.shuttletracker.data.remote.dto.RouteDto
import edu.rpi.shuttletracker.data.remote.dto.RouteDtoDeserializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        userPreferences: UserPreferences,
        @ApplicationContext context: Context,
    ): Retrofit {
        val gson =
            GsonBuilder()
                .registerTypeAdapter(RouteDto::class.java, RouteDtoDeserializer())
                .create()

        val storedUrl =
            runBlocking {
                return@runBlocking userPreferences.getBaseUrl().first()
            }

        val url =
            normalizeBaseUrl(storedUrl)
                ?: checkNotNull(normalizeBaseUrl(context.getString(R.string.url_default))) {
                    "The default API URL must be a valid HTTP(S) base URL"
                }

        return Retrofit
            .Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .baseUrl(url)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideShuttleApi(retrofit: Retrofit): ShuttleApi = retrofit.create(ShuttleApi::class.java)
}
