package unipi.msss.foodback.home

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.commons.ViewModelEventsImpl
import unipi.msss.foodback.home.ui.TastingNavigationEvents

@InstallIn(ViewModelComponent::class)
@Module
abstract class TastingEventsModule {

    @Binds
    abstract fun bindTastingViewModelEvents(
        impl: ViewModelEventsImpl<TastingNavigationEvents>,
    ): ViewModelEvents<TastingNavigationEvents>

}
