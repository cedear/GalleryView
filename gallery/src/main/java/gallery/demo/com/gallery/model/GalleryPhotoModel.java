package gallery.demo.com.gallery.model;

import android.support.annotation.DrawableRes;


public class GalleryPhotoModel {

    public Object photoSource;

    public GalleryPhotoModel(@DrawableRes int drawableRes) {
        this.photoSource = drawableRes;
    }

    public GalleryPhotoModel(String path) {
        this.photoSource = path;
    }

}
