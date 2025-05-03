package unipi.msss.foodback.home

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.commons.ViewModelEventsImpl
import unipi.msss.foodback.home.ui.HomeNavigationEvents

@InstallIn(ViewModelComponent::class)
@Module
abstract class HomeEventsModule {
    @Binds
    abstract fun bindHomeViewModelEvents(
        impl: ViewModelEventsImpl<HomeNavigationEvents>,
    ): ViewModelEvents<HomeNavigationEvents>
}