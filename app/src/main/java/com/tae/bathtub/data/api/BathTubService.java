package com.tae.bathtub.data.api;

import com.tae.bathtub.data.api.model.Boiler;

import retrofit2.http.GET;
import retrofit2.http.Headers;
import rx.Observable;

/**
 * Created by Marius on 18/04/2016.
 */
public interface BathTubService {

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @GET(NetworkConstants.BOILER_END_POINT)
    Observable<Boiler> getBoiler();

//    Call<Boiler> getBoiler();
}
