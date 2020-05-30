package net.synapticweb.callrecorder.di;

import net.synapticweb.callrecorder.data.Repository;
import net.synapticweb.callrecorder.data.RepositoryImpl;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class RepositoryModule {
    @Binds
    abstract Repository provideRepository(RepositoryImpl implementation);
}
