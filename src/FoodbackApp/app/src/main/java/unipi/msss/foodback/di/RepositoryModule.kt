package unipi.msss.foodback.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import unipi.msss.foodback.auth.login.data.LoginRepository
import unipi.msss.foodback.auth.login.data.LoginRepositoryImpl
import unipi.msss.foodback.auth.signup.data.SignupRepository
import unipi.msss.foodback.auth.signup.data.SignupRepositoryImpl

@InstallIn(ViewModelComponent::class)
@Module
abstract class RepositoryModule {

    @Binds
    abstract fun bindLoginRepository(
        impl: LoginRepositoryImpl
    ): LoginRepository

    @Binds
    abstract fun bindSignupRepository(
        impl: SignupRepositoryImpl
    ): SignupRepository
}
