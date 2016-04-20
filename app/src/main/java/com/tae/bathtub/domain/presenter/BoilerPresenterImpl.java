package com.tae.bathtub.domain.presenter;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;

import com.tae.bathtub.data.api.ServiceCallback;
import com.tae.bathtub.data.api.model.Boiler;
import com.tae.bathtub.data.api.model.ErrorResponse;
import com.tae.bathtub.data.local.Bathtub;
import com.tae.bathtub.data.local.Tap;
import com.tae.bathtub.domain.interactor.BoilerInteractor;
import com.tae.bathtub.presentation.BathtubView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Marius on 18/04/2016.
 */
public class BoilerPresenterImpl implements BoilerPresenter {

    @Inject
    BoilerInteractor interactor;
    @Inject
    BathtubView view;

    private Boiler boiler;
    private Subscription openTapSubscription;
    private int level;
    private SparseArrayCompat<Float> levels;

    @Inject
    public BoilerPresenterImpl() {
    }

    @Override
    public void initBoilerService() {
        interactor.getBoiler(new ServiceCallback<Boiler, ErrorResponse>() {
            @Override
            public void onServiceResponse(Boiler response) {
                boiler = response;
            }

            @Override
            public void onServiceError(ErrorResponse error) {
                getServiceError(error.getError());
            }
        });
        levels = generateWaterLevels();
    }

    @Override
    public int getHotWater() {
        return boiler.getHot_water();
    }

    @Override
    public int getColdWater() {
        return boiler.getCold_water();
    }

    private void calculateTemperature(Bathtub bathtub) {
        int temp = 0;
        if (bathtub.areTwoTapsOpen(bathtub.getTaps())) {
            temp = bathtub.getTemperatureFromTaps(getColdWater(), getHotWater());
        } else {
            for (Tap tap : bathtub.getTaps()) {
                if (tap.isOpen()) {
                    if (tap.getType().equals(Tap.Type.COLD.name())) {
                        temp = getColdWater();
                    } else {
                        temp = getHotWater();
                    }
                }
            }
        }
        if (temp > 0) {
            bathtub.setTemperature(temp);
            view.displayTemperature(temp);
            view.setTemperatureIndicator(getIndicatorPosition(temp));
        }
    }

    private float getIndicatorPosition(int temp) {
        float indicator = 0;
        switch (temp) {
            case 10:
                indicator = -45;
                break;
            case 50:
                indicator = 45;
                break;
            case 30:
                indicator = 25;
                break;
        }
        return indicator;
    }

    /**
     * TO IMPROVE
     * This shoul not happend here.
     * As we are running the interval in another thread using rx, this should have been implemented
     * in the interactor and the values should have been returned using a callback as we are
     * doing in order to get the boiler from the server.
     * The solution is quite dirty: using handlers. Not nice :(
     * @param taps
     */
    @Override
    public void fillBathtub(final List<Tap> taps) {
        final Bathtub bathtub = new Bathtub();
        bathtub.setTaps(taps);
        for (Tap tap : taps) {
            Log.i("IS TAP OPEN?", "fillBathtub: tab is open " + tap.isOpen());
        }
        Log.i("OPEN TAP", "fillBathtub: tap is open");
        final Observable<Long> waterlevelObservable = Observable.interval(3, TimeUnit.SECONDS);  // Interval is 3 for testing purposes, should be 60
        openTapSubscription = waterlevelObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getBathtubObserver(taps, bathtub));
    }

    @NonNull
    private Observer<Long> getBathtubObserver(final List<Tap> taps, final Bathtub bathtub) {
        return new Observer<Long>() {
            @Override
            public void onCompleted() {
                Log.i("COMPLETE", "onCompleted: bathtub is full!");
                openTapSubscription.unsubscribe();
                Schedulers.shutdown();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        view.increaseWaterLevel(-220);
                        view.showToast("Bathub is full, taps are disabled, enjoy!");
                        view.setTemperatureIndicator(getIndicatorPosition(bathtub.getTemperature()));
                    }
                });

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Long interval) {
                if (!openTapSubscription.isUnsubscribed()) {
                    Log.i("LEVEL", "level count: " + level);
                    Log.i("LEVEL", "level in bathtub: " + bathtub.getLevel());
                    Log.i("LEVEL", "call: value to increase water level" + convertToNegative(getWaterLevelByInterval(interval)));
                    float tempWaterLevel = convertToNegative(getWaterLevelByInterval(interval));
                    if (bathtub.areTwoTapsOpen(taps)) {
                        level += 20;
                        tempWaterLevel *= 2;
                    } else {
                        for (Tap tap : bathtub.getTaps()) {
                            if (tap.isOpen()) {
                                level += 10;
                            }
                        }
                    }

                    final float waterLevel = tempWaterLevel;
                    bathtub.setLevel(level);
                    calculateTemperature(bathtub);

                    if (bathtub.getLevel() >= Bathtub.MAX_CAPACITY) {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                view.waterLevelOverflow(level >= Bathtub.MAX_CAPACITY);
                            }
                        });
                        onCompleted();
                    }
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            view.increaseWaterLevel(waterLevel);
                        }
                    });

                }
            }
        };
    }


    @Override
    public String getServiceError(String error) {
        return error;
    }

    @Override
    public void closeTap() {
        if (!openTapSubscription.isUnsubscribed()) {
            openTapSubscription.unsubscribe();
            Log.i("CLOSE TAP", "closeTap: tap is close!");
        }
    }

    private float getWaterLevelByInterval(Long interval) {
        return levels.get(interval.intValue());
    }

    private SparseArrayCompat<Float> generateWaterLevels() {
        SparseArrayCompat<Float> levels = new SparseArrayCompat<>();
        float increase = 0f;
        for (int i = 0; i < 15; i++) { //Using Rx instead of 15 was 3
//            increase += 73.3;
            increase += 14.6; // this float represents the value needed for the animation
            levels.put(i,increase);
        }
        return levels;
    }

    private float convertToNegative(float value) {
        return value * -1;
    }
}
