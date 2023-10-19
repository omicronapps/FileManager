package com.omicronapplications.filelib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileManager extends BroadcastReceiver {
    public static final int STORAGE_ROOT = -1;
    public static final int STORAGE_INTERNAL = 0;
    public static final int STORAGE_EXTERNAL = 1;
    public static final int SORT_NONE = 0;
    public static final int SORT_ASCENDING = 1;
    public static final int SORT_DESCENDING = 2;
    private static final String TAG = "FileManager";
    private final Context mContext;
    private IMountCallback mCallback;
    private int mStorage;
    private File mCurrentDir;

    public FileManager(Context context, int storage) {
        mContext = context;
        mStorage = storage;
        mCurrentDir = getTopDir();
    }

    public FileManager(Context context) {
        this(context, STORAGE_ROOT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int count = getStorageCount(false);
        if (mCallback == null || action == null) {
            return;
        }
        switch (action) {
            case Intent.ACTION_MEDIA_EJECT:
            case Intent.ACTION_MEDIA_MOUNTED:
            case Intent.ACTION_MEDIA_UNMOUNTED:
            case Intent.ACTION_MEDIA_REMOVED:
                mCallback.onMediaChanged(count);
                break;
            default:
                break;
        }
    }

    public void setCallback(IMountCallback callback) {
        mCallback = callback;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        mContext.registerReceiver(this, filter);
    }

    public void unsetCallback() {
        mContext.unregisterReceiver(this);
        mCallback = null;
    }

    public int getStorageCount(boolean notify) {
        int count = 0;

        File internal = mContext.getFilesDir();
        if (internal != null) {
            count++;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null) {
                count += externals.length;;
            }
        } else {
            File external = mContext.getExternalFilesDir(null);
            if (external != null) {
                count++;
            }
        }

        if (mStorage >= count) {
            Log.w(TAG, "checkStorage: media removed: " + mStorage);
            mStorage = STORAGE_ROOT;
            mCurrentDir = getTopDir();
            if (notify && mCallback != null) {
                mCallback.onMediaChanged(count);
            }
        }
        return count;
    }

    public int getStorageCount() {
        return getStorageCount(true);
    }

    public File createNewFile(String name) {
        File file = getFullPath(null, name);
        if (file != null) {
            try {
                if (!file.createNewFile()) {
                    Log.e(TAG, "Failed to create file " + mCurrentDir.getPath() + File.separator + name);
                    file = null;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return file;
    }

    public boolean delete(String path, String name) {
        File file = getFullPath(path, name);
        if (file.exists()) {
            return file.delete();
        } else {
            return false;
        }
    }

    public File[] list() {
        return list(SORT_NONE);
    }

    public File[] list(int order) {
        File[] dirs;
        if (isValidDir(mCurrentDir)) {
            dirs = mCurrentDir.listFiles();
            if (dirs != null) {
                if (order == SORT_ASCENDING || order == SORT_DESCENDING) {
                    Arrays.sort(dirs, new IgnoreCaseComparator());
                }
                if (order == SORT_DESCENDING) {
                    Arrays.sort(dirs, Collections.reverseOrder());
                }
            }
        } else {
            List<File> top = getTopDirs();
            dirs = top.toArray(new File[0]);
        }
        return dirs;
    }

    public File mkdir(String dir) {
        File newDir = getFullPath(null, dir);
        if ((newDir != null) && newDir.mkdir()) {
            return newDir;
        } else {
            Log.e(TAG, "mkdir: failed " + newDir);
            return null;
        }
    }

    public boolean renameTo(File file, String name) {
        if (!isValidFile(file) && !isValidDir(file)) {
            Log.w(TAG, "renameTo: Illegal file: " + file);
            return false;
        }
        if (!isValidName(name)) {
            Log.w(TAG, "renameTo: Illegal dir: " + name);
            return false;
        }
        String path = file.getParent();
        File dest = new File(path, name);
        return file.renameTo(dest);
    }

    public File getDir() {
        return mCurrentDir;
    }

    public File changeDir(File dir) {
        if (dir != null && !isValidDir(dir)) {
            Log.w(TAG, "changeDir: Illegal dir: " + dir);
        } else if (dir == null) {
            mStorage = STORAGE_ROOT;
            mCurrentDir = null;
        } else {
            mStorage = inStorage(dir.getAbsolutePath());
            mCurrentDir = dir;
        }
        return mCurrentDir;
    }

    public File changeDir(String path, String name) {
        File file = getFullPath(path, name);
        if (!isValidDir(file)) {
            Log.w(TAG, "changeDir: Illegal dir: " + file);
        } else {
            mStorage = inStorage(file.getAbsolutePath());
            mCurrentDir = file;
        }
        return mCurrentDir;
    }

    public File changeDirRoot() {
        mStorage = STORAGE_ROOT;
        mCurrentDir = null;
        return mCurrentDir;
    }

    public File changeDirTop(int storage) {
        File dir = null;
        int count = getStorageCount();
        if (storage < count) {
            mStorage = storage;
            dir = changeDir(getTopDir());
        } else {
            Log.w(TAG, "changeDirTop: illegal storage: " + storage);
        }
        return dir;
    }

    public File changeDirTop() {
        return changeDir(getTopDir());
    }

    public File changeDirUp() {
        if (isRootDir()) {
            Log.w(TAG, "changeDirUp: Already at root dir: " + mCurrentDir);
            return mCurrentDir;
        } else if (isTopDir()) {
            return changeDirRoot();
        }
        String path = mCurrentDir.getAbsolutePath();
        int endIndex = path.lastIndexOf(File.separatorChar);
        if (endIndex == -1) {
            Log.w(TAG, "changeDirUp: No higher dir: " + mCurrentDir);
            return mCurrentDir;
        }
        String upDir = path.substring(0, endIndex);
        File dir = new File(upDir);
        return changeDir(dir);
    }

    public boolean isRootDir() {
        return (mStorage == STORAGE_ROOT);
    }

    public boolean isTopDir() {
        File dir = getTopDir();
        if (dir == null) {
            return false;
        }
        return dir.equals(getDir());
    }

    public File getFile(String path, String name) {
        return getFullPath(path, name);
    }

    public int getStorage() {
        return mStorage;
    }

    public int inStorage(String name) {
        int storage = STORAGE_ROOT;
        if (!isValidName(name)) {
            Log.w(TAG, "inStorage: Illegal file: " + name);
            return storage;
        }

        File dir = mContext.getFilesDir();
        if (dir != null) {
            if (name.startsWith(dir.getAbsolutePath())) {
                storage = STORAGE_INTERNAL;
            }
        }

        if (storage == STORAGE_ROOT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null && externals.length > 0) {
                for (int i = 0; i < externals.length; i++) {
                    dir = externals[i];
                    if (dir != null && name.startsWith(dir.getAbsolutePath())) {
                        storage = STORAGE_EXTERNAL + i;
                        break;
                    }
                }
            }
        } else if (storage == STORAGE_ROOT) {
            dir = mContext.getExternalFilesDir(null);
            if (dir != null) {
                if (name.startsWith(dir.getAbsolutePath())) {
                    storage = STORAGE_EXTERNAL;
                }
            }
        }

        return storage;
    }

    public String getPathAndName(String name) {
        String dirName = null;
        if (!isValidName(name)) {
            Log.w(TAG, "getPathAndName: Illegal file: " + name);
            return null;
        }
        int storage = inStorage(name);
        File dir = getTopDir(storage);
        if (dir != null) {
            dirName = dir.getAbsolutePath();
        }
        if (dirName == null || !name.startsWith(dirName)) {
            Log.w(TAG, "getPathAndName: Illegal path: " + name);
            return null;
        }
        name = name.replaceFirst(dirName, "");
        if (name.startsWith(File.separator)) {
            name = name.replaceFirst(File.separator, "");
        }
        return name;
    }

    public String getPath(String name) {
        String path = getPathAndName(name);
        int index = path.lastIndexOf(File.separatorChar);
        if (index != -1) {
            path = path.substring(0, index);
        } else {
            path = null;
        }
        return path;
    }

    public List<File> getTopDirs() {
        List<File> dirs = new ArrayList<>();
        File internal = mContext.getFilesDir();
        if (internal != null) {
            dirs.add(internal);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null) {
                Collections.addAll(dirs, externals);
            }
        } else {
            File external = mContext.getExternalFilesDir(null);
            if (external != null) {
                dirs.add(external);
            }
        }

        return dirs;
    }

    public File getTopDir(int storage) {
        getStorageCount();
        File dir = null;
        if (storage == STORAGE_ROOT) {
            dir = null;
        } else if (storage == STORAGE_INTERNAL) {
            dir = mContext.getFilesDir();
        } else if (storage >= STORAGE_EXTERNAL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                int offset = storage - STORAGE_EXTERNAL;
                File[] externals = mContext.getExternalFilesDirs(null);
                if (externals != null && externals.length > offset && externals[offset] != null) {
                    dir = externals[offset];
                } else {
                    Log.w(TAG, "getTopDir: storage not available: " + storage);
                }
            } else if (storage > STORAGE_EXTERNAL) {
                Log.w(TAG, "getTopDir: Not supported in SDK version: " + Build.VERSION.SDK_INT);
            } else {
                dir = mContext.getExternalFilesDir(null);
            }
        } else {
            Log.w(TAG, "getTopDir: storage not supported: " + storage);
        }
        return dir;
    }

    public File getTopDir() {
        return getTopDir(mStorage);
    }

    private boolean startsWith(File file) {
        boolean startsWith = false;
        String name = file.getAbsolutePath();
        if (name == null) {
            return startsWith;
        }
        File dir = mContext.getFilesDir();
        if (dir != null) {
            startsWith = name.startsWith(dir.getAbsolutePath());
        }
        dir = mContext.getExternalFilesDir(null);
        if (!startsWith && dir != null) {
            startsWith = name.startsWith(dir.getAbsolutePath());
        }
        if (!startsWith && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null && externals.length > 1 && externals[1] != null) {
                dir = externals[1];
                startsWith = name.startsWith(dir.getAbsolutePath());
            }
        }
        return startsWith;
    }

    private File getFullPath(String path, String name) {
        File file = null;
        if (path != null && name != null) {
            file = new File(path, name);
        } else if (path != null) {
            file = new File(path);
        } else if (mCurrentDir != null && name != null) {
            file = new File(mCurrentDir, name);
        } else if (mCurrentDir != null) {
            file = mCurrentDir;
        } else {
            Log.w(TAG, "getFullPath: Illegal dir");
        }
        return file;
    }

    private static boolean isValidFile(File file) {
        return (file != null) && file.exists() && !file.isDirectory();
    }

    private static boolean isValidDir(File dir) {
        return (dir != null) && dir.exists() && dir.isDirectory();
    }

    private static boolean isValidName(String name) {
        return (name != null) && !name.isEmpty();
    }

    private static class IgnoreCaseComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            String s1 = o1.getName().toLowerCase();
            String s2 = o2.getName().toLowerCase();
            return s1.compareTo(s2);
        }
    }
}
