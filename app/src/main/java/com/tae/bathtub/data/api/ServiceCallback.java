package com.tae.bathtub.data.api;

/**
 * Created by Marius on 18/04/2016.
 */
public interface ServiceCallback<R,E> {
    void onServiceResponse(R response);
    void onServiceError(E error);
}
