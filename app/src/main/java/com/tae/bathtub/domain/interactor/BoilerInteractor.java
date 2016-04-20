package com.tae.bathtub.domain.interactor;

import com.tae.bathtub.data.api.ServiceCallback;
import com.tae.bathtub.data.api.model.Boiler;
import com.tae.bathtub.data.api.model.ErrorResponse;

/**
 * Created by Marius on 18/04/2016.
 */
public interface BoilerInteractor {
    void getBoiler(ServiceCallback<Boiler, ErrorResponse> callback);
}
