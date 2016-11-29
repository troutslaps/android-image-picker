package com.esafirm.imagepicker.features.common;

import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.util.List;

public interface ImageLoaderListener {
    void onImagePageLoaded(List<Image> images, List<Folder> folders, int currentPage, int
            totalNumberOfPages, int offset);

    void onImageLoaded(List<Image> images, List<Folder> folders);

    void onFailed(Throwable throwable);
}
