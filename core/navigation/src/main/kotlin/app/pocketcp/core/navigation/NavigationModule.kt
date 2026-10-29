package app.pocketcp.core.navigation

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Declares the `Set<FeatureEntry>` multibinding, so the graph stays valid even when no feature contributes. */
@Module
@InstallIn(SingletonComponent::class)
interface NavigationModule {
    @Multibinds
    fun featureEntries(): Set<FeatureEntry>
}
