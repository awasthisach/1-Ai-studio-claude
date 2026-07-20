package com.vvf.smartmanager.core.di

import android.content.Context
import androidx.work.WorkManager
import com.vvf.smartmanager.data.repository.DuplicateRepositoryImpl
import com.vvf.smartmanager.data.repository.FileRepositoryImpl
import com.vvf.smartmanager.data.repository.OcrRepositoryImpl
import com.vvf.smartmanager.data.repository.SearchRepositoryImpl
import com.vvf.smartmanager.data.repository.StorageRepositoryImpl
import com.vvf.smartmanager.data.repository.VaultRepositoryImpl
import com.vvf.smartmanager.domain.repository.DuplicateRepository
import com.vvf.smartmanager.domain.repository.FileRepository
import com.vvf.smartmanager.domain.repository.OcrRepository
import com.vvf.smartmanager.domain.repository.SearchRepository
import com.vvf.smartmanager.domain.repository.StorageRepository
import com.vvf.smartmanager.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    // androidx.hilt:hilt-work only provides HiltWorkerFactory support for @HiltWorker classes —
    // it does NOT automatically bind WorkManager itself. Without this, IndexingScheduler's
    // constructor injection of WorkManager fails to compile (Dagger [MissingBinding]) the
    // moment anything actually tries to use it (confirmed by audit: nothing did until now).
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindDuplicateRepository(impl: DuplicateRepositoryImpl): DuplicateRepository

    @Binds
    @Singleton
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository
}
