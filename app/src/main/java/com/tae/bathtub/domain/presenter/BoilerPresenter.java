package com.tae.bathtub.domain.presenter;

import com.tae.bathtub.data.local.Tap;

import java.util.List;

/**
 * Created by Marius on 18/04/2016.
 */
public interface BoilerPresenter {

    void initBoilerService();
    int getHotWater();
    int getColdWater();
//    void calculateTemperature();
    void fillBathtub(List<Tap> tap);
    String getServiceError(String error);

    void closeTap();
}
