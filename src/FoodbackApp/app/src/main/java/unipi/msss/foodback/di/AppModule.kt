package unipi.msss.foodback.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import unipi.msss.foodback.BuildConfig
import unipi.msss.foodback.data.network.http.FoodbackHttpClientBuilder
import io.ktor.client.HttpClient
import io.ktor.http.URLProtocol
import kotlinx.serialization.json.Json

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    fun providesJson(): Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Provides
    fun provideHttpClient(builder: FoodbackHttpClientBuilder): HttpClient = builder
        .protocol(URLProtocol.HTTPS)
        .host(BuildConfig.BASE_URL)
        .build()
}