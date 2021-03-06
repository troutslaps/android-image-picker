package com.esafirm.imagepicker.features.common;

import com.esafirm.imagepicker.features.ImageLoader;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.util.List;

/**
 * Created by mmcervantes on 29/11/2016.
 */

public interface ImageLoaderListener {
    void onImagePageForFolderLoaded(List<Image> images, String folder, int currentPage, int
            totalNumberOfPages, int offset);
    void onFailed(Throwable throwable);
}
