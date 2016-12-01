package com.esafirm.imagepicker.features.common;

import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.util.List;

/**
 * Created by mmcervantes on 29/11/2016.
 */

public interface FolderLoaderListener {
    void onFoldersLoaded(List<Folder> folders, int currentPage, int
            totalNumberOfPages, int offset);
    void onFailed(Throwable throwable);
}
