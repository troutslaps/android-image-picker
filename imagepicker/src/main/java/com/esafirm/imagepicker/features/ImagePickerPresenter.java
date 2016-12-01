package com.esafirm.imagepicker.features;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.features.camera.CameraModule;
import com.esafirm.imagepicker.features.camera.DefaultCameraModule;
import com.esafirm.imagepicker.features.camera.OnImageReadyListener;
import com.esafirm.imagepicker.features.common.BasePresenter;
import com.esafirm.imagepicker.features.common.FolderLoaderListener;
import com.esafirm.imagepicker.features.common.ImageLoaderListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.List;

public class ImagePickerPresenter extends BasePresenter<ImagePickerView> {

    private ImageLoader imageLoader;
    private CameraModule cameraModule = new DefaultCameraModule();
    private Handler handler = new Handler(Looper.getMainLooper());

    public ImagePickerPresenter(ImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public void abortLoad() {
        imageLoader.abortLoadImages();
    }

    public void loadAllImages(int pageSize) {
        if (!isViewAttached()) return;

        getView().showLoading(true);
        this.loadImagesForFolder(null, pageSize);
    }

    public void loadAllDeviceFolders(int pageSize) {
        if (!isViewAttached()) return;

        getView().showLoading(true);
        imageLoader.loadDeviceFolders(pageSize, new FolderLoaderListener() {
            @Override
            public void onFoldersLoaded(final List<Folder> folders, int currentPage, int
                    totalNumberOfPages, final int offset) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isViewAttached()) {
                            getView().showPageFetchCompleted(null, folders, offset);

                            if (folders.isEmpty()) {
                                getView().showEmpty();
                            } else {
                                getView().showLoading(false);
                            }

                        }
                    }
                });
            }


            @Override
            public void onFailed(final Throwable throwable) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isViewAttached()) {
                            getView().showError(throwable);
                        }
                    }
                });
            }
        });
    }

    public void loadImagesForFolder(String folder, int pageSize) {
        if (!isViewAttached()) return;

        getView().showLoading(true);
        imageLoader.loadDeviceImagesFromFolder(folder, pageSize, new ImageLoaderListener() {

            @Override
            public void onFailed(final Throwable throwable) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isViewAttached()) {
                            getView().showError(throwable);
                        }
                    }
                });
            }

            @Override
            public void onImagePageForFolderLoaded(final List<Image> images, final String folder,
                                                   int currentPage, int totalNumberOfPages, final
                                                   int offset) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isViewAttached()) {
                            getView().showPageFetchCompleted(images, null, offset);
                            getView().showLoading(false);

                        }
                    }
                });
            }
        });
    }

    public void onDoneSelectImages(List<Image> selectedImages) {
        if (selectedImages != null && selectedImages.size() > 0) {

            /** Scan selected images which not existed */
            for (int i = 0; i < selectedImages.size(); i++) {
                Image image = selectedImages.get(i);
                File file = new File(image.getPath());
                if (!file.exists()) {
                    selectedImages.remove(i);
                    i--;
                }
            }
            getView().finishPickImages(selectedImages);
        }
    }

    public void captureImage(Activity activity, ImagePickerConfig config, int requestCode) {
        Context context = activity.getApplicationContext();
        Intent intent = cameraModule.getCameraIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(R.string.error_create_image_file), Toast
                    .LENGTH_LONG).show();
            return;
        }
        activity.startActivityForResult(intent, requestCode);

    }

    public void finishCaptureImage(Context context, Intent data, final ImagePickerConfig config) {
        cameraModule.getImage(context, data, new OnImageReadyListener() {
            @Override
            public void onImageReady(List<Image> images) {
                if (config.isReturnAfterCapture()) {
                    getView().finishPickImages(images);
                } else {
                    getView().showCapturedImage();
                }
            }
        });
    }
}
