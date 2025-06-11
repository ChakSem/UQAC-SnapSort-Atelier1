package com.example.snapsort.di

import com.example.snapsort.data.repository.NetworkRepository
import com.example.snapsort.data.repository.TransferRepository
import com.example.snapsort.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetNetworkStatusUseCase(
        networkRepository: NetworkRepository
    ): GetNetworkStatusUseCase = GetNetworkStatusUseCase(networkRepository)

    @Provides
    @Singleton
    fun provideConnectToWifiUseCase(
        networkRepository: NetworkRepository
    ): ConnectToWifiUseCase = ConnectToWifiUseCase(networkRepository)

    @Provides
    @Singleton
    fun provideFindServerUseCase(
        networkRepository: NetworkRepository
    ): FindServerUseCase = FindServerUseCase(networkRepository)

    @Provides
    @Singleton
    fun provideGetAvailableFoldersUseCase(
        imageRepository: com.example.snapsort.data.repository.ImageRepositoryImpl
    ): GetAvailableFoldersUseCase = GetAvailableFoldersUseCase(imageRepository)

    @Provides
    @Singleton
    fun provideGetImagesFromFolderUseCase(
        imageRepository: com.example.snapsort.data.repository.ImageRepositoryImpl
    ): GetImagesFromFolderUseCase = GetImagesFromFolderUseCase(imageRepository)

    @Provides
    @Singleton
    fun provideFilterImagesByDateRangeUseCase(): FilterImagesByDateRangeUseCase =
        FilterImagesByDateRangeUseCase()

    @Provides
    @Singleton
    fun provideTransferImagesUseCase(
        transferRepository: TransferRepository
    ): TransferImagesUseCase = TransferImagesUseCase(transferRepository)

    @Provides
    @Singleton
    fun provideValidateImagesForTransferUseCase(): ValidateImagesForTransferUseCase =
        ValidateImagesForTransferUseCase()
}