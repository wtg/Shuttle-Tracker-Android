package edu.rpi.shuttletracker.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.rpi.shuttletracker.data.remote.RetrofitShuttleRemoteDataSource
import edu.rpi.shuttletracker.data.remote.ShuttleRemoteDataSource
import edu.rpi.shuttletracker.data.repository.DefaultShuttleRepository
import edu.rpi.shuttletracker.data.repository.ShuttleRepository
import javax.inject.Singleton

/** Binds data interfaces to their production implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindShuttleRemoteDataSource(implementation: RetrofitShuttleRemoteDataSource): ShuttleRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindShuttleRepository(implementation: DefaultShuttleRepository): ShuttleRepository
}
