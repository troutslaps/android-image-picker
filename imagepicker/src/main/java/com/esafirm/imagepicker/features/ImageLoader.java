package com.esafirm.imagepicker.features;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.esafirm.imagepicker.features.common.FolderLoaderListener;
import com.esafirm.imagepicker.features.common.ImageLoaderListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private final String SORT_PAGINATION = " DESC LIMIT %d OFFSET %d";
    private Context context;
    private ExecutorService executorService;
    private String currentFolder;

    public ImageLoader(Context context) {
        this.context = context;
    }

    private final String[] projection = new String[]{MediaStore.Images.Media._ID, // image ID
            MediaStore.Images.Media.DISPLAY_NAME, // name
            MediaStore.Images.Media.DATA, // path
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME // folder name
    };

    private final String[] folderProjection = new String[]{"DISTINCT " + MediaStore.Images.Media
            .BUCKET_ID, // folder id
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME, // folder name
            MediaStore.Images.Media.DATA, // path
            MediaStore.Images.Media._ID, // image ID
            MediaStore.Images.Media.DISPLAY_NAME // image name
    };



    public void loadDeviceFolders(int pageSize, final FolderLoaderListener listener) {
        currentFolder = null;
        getExecutorService().execute(new FolderLoadPaginatedRunnable(pageSize, listener));
    }


    public void loadDeviceImagesFromFolder(String folder, int pageSize, final ImageLoaderListener listener) {
        currentFolder = folder;
        getExecutorService().execute(new ImageLoadPaginatedRunnable(pageSize, listener, folder));
    }


    public void abortLoadImages() {
        currentFolder = null;
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }




    private class FolderLoadPaginatedRunnable implements Runnable {
        private FolderLoaderListener listener;
        private int pageSize;

        private FolderLoadPaginatedRunnable(int pageSize, FolderLoaderListener listener) {
            this.listener = listener;
            this.pageSize = pageSize;
        }

        @Override
        public void run() {
            String sortBy = SORT_PAGINATION;
            int numOfPages = 1;
            int count = 0;
            int offset;
            Cursor countCursor = context.getContentResolver().query(MediaStore.Images.Media
                    .EXTERNAL_CONTENT_URI, new String[]{"count(" + "DISTINCT " + MediaStore
                    .Images.Media.BUCKET_ID + ")"}, null, null, MediaStore.Images.Media.DATE_ADDED);
            if (countCursor.getCount() == 0) {
                countCursor.close();
                listener.onFailed(new NullPointerException());
            } else {
                countCursor.moveToFirst();
                count = countCursor.getInt(0);
                countCursor.close();
            }
            countCursor.close();
            numOfPages = (int) Math.ceil((double)count / pageSize);

            for (int page = 0; page < numOfPages; page++) {
                offset = page * pageSize;
                Cursor folderCursor = context.getContentResolver().query(MediaStore.Images.Media
                        .EXTERNAL_CONTENT_URI, folderProjection, MediaStore.Images.Media.BUCKET_ID +
                        " IS NOT NULL) GROUP BY (" + MediaStore.Images.Media.BUCKET_ID, null,
                        MediaStore.Images.Media.DATE_ADDED + String.format(sortBy, pageSize,
                                offset));
                List<Folder> folders = new ArrayList<>();
                if (folderCursor.moveToFirst()) {
                    do {

                        String bucketDisplayName = folderCursor.getString(folderCursor
                                .getColumnIndex(folderProjection[1]));
                        String path = folderCursor.getString(folderCursor.getColumnIndex
                                (folderProjection[2]));
                        long id = folderCursor.getLong(folderCursor.getColumnIndex(projection[3]));
                        String imageName = folderCursor.getString(folderCursor.getColumnIndex
                                (folderProjection[4]));
                        Folder folder = new Folder(bucketDisplayName);
                        ArrayList<Image> images = new ArrayList<>();
                        Image image = new Image(id, imageName, path, false);
                        images.add(image);
                        folder.setImages(images);
                        folders.add(folder);
                        System.out.println("FOLDER NAME = " +
                                bucketDisplayName + ", path = " +
                                path);

                    } while (folderCursor.moveToNext());
                }
                folderCursor.close();
                listener.onFoldersLoaded(folders, page, numOfPages, offset);
            }
        }
    }

    private class ImageLoadPaginatedRunnable implements Runnable {
        private ImageLoaderListener listener;
        private int pageSize;
        private String folder;

        public ImageLoadPaginatedRunnable(int pageSize, ImageLoaderListener listener,
                                          String folder) {
            this.listener = listener;
            this.pageSize = pageSize;
            this.folder = folder;
        }


        @Override
        public void run() {
            int count = 0;
            Cursor countCursor = context.getContentResolver().query(MediaStore.Images.Media
                    .EXTERNAL_CONTENT_URI, new String[]{"count(*)"}, null, null, MediaStore
                    .Images.Media.DATE_ADDED);
            if (countCursor.getCount() == 0) {
                countCursor.close();
                listener.onFailed(new NullPointerException());
            } else {
                countCursor.moveToFirst();
                count = countCursor.getInt(0);
                countCursor.close();
            }

            int numOfPages = (int) Math.ceil(count / pageSize);

            for (int page = 0; page < numOfPages; page++) {
                if(folder != null && !folder.equals(currentFolder))
                {
                    break;
                }
                int offset = page * pageSize;
                Cursor pageCursor = context.getContentResolver().query(MediaStore.Images.Media
                        .EXTERNAL_CONTENT_URI, projection, folder != null ? MediaStore.Images
                        .Media.BUCKET_DISPLAY_NAME + " = '" + folder + "'" : null, null, MediaStore
                        .Images.Media.DATE_ADDED + String.format(SORT_PAGINATION, pageSize,
                        offset));

                List<Image> temp = new ArrayList<>(pageCursor.getCount());
                if (pageCursor.moveToFirst()) {
                    do {
                        long id = pageCursor.getLong(pageCursor.getColumnIndex(projection[0]));
                        String name = pageCursor.getString(pageCursor.getColumnIndex
                                (projection[1]));
                        String path = pageCursor.getString(pageCursor.getColumnIndex
                                (projection[2]));


                        File file = new File(path);
                        if (file.exists()) {
                            Image image = new Image(id, name, path, false);
                            temp.add(image);


                        }

                    } while (pageCursor.moveToNext());
                }
                pageCursor.close();
                listener.onImagePageForFolderLoaded(temp, folder, page, numOfPages, offset);
            }
        }
    }
}
