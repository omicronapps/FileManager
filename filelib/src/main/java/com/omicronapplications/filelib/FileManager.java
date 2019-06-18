package com.omicronapplications.filelib;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class FileManager {
    private static final String TAG = "FileManager";
    private final Context mContext;
    private boolean mExternal;
    private File mCurrentDir;

    public FileManager(Context context, boolean external) {
        mContext = context;
        mExternal = external;
        mCurrentDir = getTopDir();
    }

    public FileManager(Context context) {
        this(context, false);
    }

    public File createNewFile(String name) {
        File file = getFile(name);
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

    public boolean delete(String name) {
        File file = getFile(name);
        if (file != null) {
            return file.delete();
        } else {
            return false;
        }
    }

    public String[] list() {
        String[] dirs = null;
        if (isValidDir(mCurrentDir)) {
            dirs = mCurrentDir.list();
        }
        return dirs;
    }

    public File[] listFiles() {
        File[] dirFiles = null;
        if (isValidDir(mCurrentDir)) {
            dirFiles = mCurrentDir.listFiles();
        }
        return dirFiles;
    }

    public File mkdir(String dir) {
        File newDir = getFile(dir);
        if ((newDir != null) && newDir.mkdir()) {
            return newDir;
        } else {
            Log.e(TAG, "mkdir: failed " + newDir);
            return null;
        }
    }

    public boolean renameTo(File file, String name) {
        if (!isValidFile(file)) {
            Log.w(TAG, "renameTo: Illegal file " + file);
            return false;
        }
        if (!isValidName(name)) {
            Log.w(TAG, "renameTo: Illegal dir " + name);
            return false;
        }
        String path = file.getAbsolutePath();
        File dest = new File(path, name);
        return file.renameTo(dest);
    }

    public File getDir() {
        return mCurrentDir;
    }

    public File changeDir(File dir) {
        if (!isValidDir(dir)) {
            Log.w(TAG, "changeDir: Illegal dir " + dir);
        } else {
            mCurrentDir = dir;
        }
        return mCurrentDir;
    }

    public File changeDir(String dir) {
        File file = new File(mCurrentDir, dir);
        if (!isValidDir(file)) {
            Log.w(TAG, "changeDir: Illegal dir " + file);
        } else {
            mCurrentDir = file;
        }
        return mCurrentDir;
    }

    public File changeDirTop(boolean external) {
        mExternal = external;
        return changeDir(getTopDir());
    }

    public File changeDirTop() {
        return changeDir(getTopDir());
    }

    public File changeDirUp() {
        if (isTopDir()) {
            Log.w(TAG, "changeDirUp: Already at top dir " + mCurrentDir);
            return mCurrentDir;
        }
        String path = mCurrentDir.getAbsolutePath();
        int endIndex = path.lastIndexOf(File.separatorChar);
        if (endIndex == -1) {
            Log.w(TAG, "changeDirUp: No higher dir " + mCurrentDir);
            return mCurrentDir;
        }
        String upDir = path.substring(0, endIndex);
        File dir = new File(upDir);
        return changeDir(dir);
    }

    public File swapDirTop() {
        mExternal = !mExternal;
        return changeDir(getTopDir());
    }

    public boolean isTopDir() {
        return getTopDir().equals(getDir());
    }

    public File getFile(String name) {
        if (!isValidDir(mCurrentDir)) {
            Log.w(TAG, "getFile: No current dir " + mCurrentDir);
            return null;
        }
        if (!isValidName(name)) {
            Log.w(TAG, "getFile: Illegal file " + name);
            return null;
        }
        return new File(mCurrentDir, name);
    }

    public boolean isExternal() {
        return mExternal;
    }

    public boolean isExternal(String name) {
        boolean isExternal = false;
        if (!isValidName(name)) {
            Log.w(TAG, "isExternal: Illegal file " + name);
            return false;
        }
        File dir = mContext.getExternalFilesDir(null);
        if (dir != null && name != null) {
            String dirName = dir.getAbsolutePath();
            isExternal = name.startsWith(dirName);
        }
        return isExternal;
    }

    public String getFileName(String name) {
        String dirName = null;
        if (!isValidName(name)) {
            return null;
        }
        File dir = mContext.getExternalFilesDir(null);
        if (dir != null) {
            dirName = dir.getAbsolutePath();
            if (!name.startsWith(dirName)) {
                dir = mContext.getFilesDir();
                if (dir != null) {
                    dirName = dir.getAbsolutePath();
                }
            }
        }
        if ((dirName == null) || !name.startsWith(dirName)) {
            Log.w(TAG, "getFileName: Illegal path " + name);
            return null;
        }
        name = name.replaceFirst(dirName, "");
        if (name.startsWith(File.separator)) {
            name = name.replaceFirst(File.separator, "");
        }
        return name;
    }

    public File getTopDir() {
        File dir;
        if (mExternal) {
            dir = mContext.getExternalFilesDir(null);
        } else {
            dir = mContext.getFilesDir();
        }
        return dir;
    }

    public boolean startsWith(File file) {
        boolean startsWith = false;
        String name = file.getAbsolutePath();
        File dir = mContext.getExternalFilesDir(null);
        if (dir != null && name != null) {
            String dirName = dir.getAbsolutePath();
            startsWith = name.startsWith(dirName);
        }
        if (!startsWith) {
            dir = mContext.getFilesDir();
            if (dir != null && name != null) {
                String dirName = dir.getAbsolutePath();
                startsWith = name.startsWith(dirName);
            }
        }
        return startsWith;
    }

    private boolean isValidFile(File file) {
        return (file != null) && file.exists() && !file.isDirectory();
    }

    private boolean isValidDir(File dir) {
        return (dir != null) && dir.exists() && dir.isDirectory();
    }

    private boolean isValidName(String name) {
        return ((name != null) && !name.isEmpty());
    }
}
