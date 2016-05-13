package com.sssnowy.anacostiaparkapp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 2016dzhao on 5/13/2016.
 */
public class GalleryImage implements Parcelable {
    private String title, url;

    public GalleryImage(){

    }

    public GalleryImage(String title, String url){
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
