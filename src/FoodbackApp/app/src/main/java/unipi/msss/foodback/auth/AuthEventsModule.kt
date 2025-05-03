package unipi.msss.foodback.auth

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import unipi.msss.foodback.auth.login.ui.LoginNavigationEvents
import unipi.msss.foodback.auth.signup.ui.SignUpNavigationEvents
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.commons.ViewModelEventsImpl

@InstallIn(ViewModelComponent::class)
@Module
abstract class AuthEventsModule {
    @Binds
    abstract fun bindLoginViewModelEvents(
        impl: ViewModelEventsImpl<LoginNavigationEvents>,
    ): ViewModelEvents<LoginNavigationEvents>
}

@Module
@InstallIn(ViewModelComponent::class)
object SignUpModule {

    @Provides
    @ViewModelScoped
    fun provideSignUpViewModelEvents(): ViewModelEvents<SignUpNavigationEvents> {
        return ViewModelEventsImpl()
    }
}
