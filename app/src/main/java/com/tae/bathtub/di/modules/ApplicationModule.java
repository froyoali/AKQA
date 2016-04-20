package com.tae.bathtub.di.modules;

import android.content.Context;

import com.tae.bathtub.App;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Marius on 18/04/2016.
 */
@Module
public class ApplicationModule {

    @Singleton
    @Provides
    Context provideContext() {
        return App.getInstance();
    }
}
