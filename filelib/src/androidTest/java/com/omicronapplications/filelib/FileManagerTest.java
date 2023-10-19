package com.omicronapplications.filelib;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FileManagerTest {
    private static final String TEST_FILE1 = "TestFile.1";
    private static final String TEST_FILE2 = "OtherFile.2";
    private static final String TEST_FILE3 = "SomeFile.3";
    private static final String TEST_DIR1 = "TestDir.1";
    private static final String TEST_DIR2 = "OtherDir.2";
    private static final String TEST_DIR3 = "SomeDir.3";

    private Context mAppContext;
    private FileManager mFileManager;
    private Callback mCallback;

    private static class Callback implements IMountCallback {
        @Override
        public void onMediaChanged(int count) {}
    }

    private void deleteTestFiles(int storage) {
        mFileManager = new FileManager(mAppContext, storage);
        mFileManager.changeDirTop();
        mFileManager.delete(null, TEST_FILE1);
        mFileManager.delete(null, TEST_FILE2);
        mFileManager.delete(null, TEST_FILE3);
        mFileManager.changeDir(null, TEST_DIR1);
        mFileManager.delete(null, TEST_DIR2);
        mFileManager.delete(null, TEST_DIR3);
        mFileManager.changeDirTop();
        mFileManager.delete(null, TEST_DIR1);
    }

    @Before
    public void setup() {
        mAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        deleteTestFiles(FileManager.STORAGE_INTERNAL);
        deleteTestFiles(FileManager.STORAGE_EXTERNAL);
        deleteTestFiles(FileManager.STORAGE_EXTERNAL + 1);
        mCallback = new Callback();
    }

    private void testFiles(int storage) {
        mFileManager = new FileManager(mAppContext, storage);
        mFileManager.setCallback(mCallback);
        File dir = mFileManager.getDir();

        assertEquals("changeDir", mFileManager.changeDir(dir), dir);
        File[] dirs = mFileManager.list();
        assertEquals("length", dirs.length, 0);
        File[] dirFiles = mFileManager.list(FileManager.SORT_NONE);
        assertEquals("length", dirFiles.length, 0);

        // Create new file
        mFileManager.createNewFile(TEST_FILE1);
        mFileManager.createNewFile(TEST_FILE2);
        mFileManager.createNewFile(TEST_FILE3);
        dirFiles = mFileManager.list(FileManager.SORT_NONE);
        assertEquals("length", dirFiles.length, 3);
        dirs = mFileManager.list();
        assertEquals("length", dirs.length, 3);

        // Delete
        for (File name : dirs) {
            assertTrue("delete", mFileManager.delete(null, name.getName()));
        }
        dirs = mFileManager.list();
        assertEquals("length", dirs.length, 0);
        dirFiles = mFileManager.list(FileManager.SORT_NONE);
        assertEquals("length", dirFiles.length, 0);
    }

    private void testDirs(int storage) {
        mFileManager = new FileManager(mAppContext, storage);
        mFileManager.setCallback(mCallback);
        File topDir = mFileManager.getDir();
        assertEquals("changeDir", mFileManager.changeDir(topDir), topDir);

        // Create dir
        File subDir1 = mFileManager.mkdir(TEST_DIR1);
        assertNotNull("mkdir", subDir1);
        assertEquals("changeDir", mFileManager.changeDir(null, TEST_DIR1), subDir1);

        // Create subdirs
        File subDir2 = mFileManager.mkdir(TEST_DIR2);
        assertNotNull("mkdir", subDir2);
        File subDir3 = mFileManager.mkdir(TEST_DIR3);
        assertNotNull("mkdir", subDir3);
        assertEquals("changeDir", mFileManager.changeDir(null, TEST_DIR3), subDir3);

        // Delete subdirs
        assertEquals("changeDir", mFileManager.changeDir(subDir1), subDir1);
        assertTrue("delete", mFileManager.delete(null, TEST_DIR2));
        assertTrue("delete", mFileManager.delete(null, TEST_DIR3));

        // Delete dir
        assertEquals("changeDir", mFileManager.changeDir(topDir), topDir);
        assertTrue("delete", mFileManager.delete(null, TEST_DIR1));
    }

    @Test
    public void testTopDirs() {
        mFileManager = new FileManager(mAppContext);
        mFileManager.setCallback(mCallback);

        // Root
        File dir = mFileManager.changeDirRoot();
        assertNull("null", dir);
        dir = mFileManager.changeDirTop(FileManager.STORAGE_ROOT);
        assertNull("null", dir);
        List<File> dirs = mFileManager.getTopDirs();
        assertTrue("dirs", dirs.size() >= 3);
        assertTrue("root", mFileManager.isRootDir());
        for (File file : dirs) {
            assertEquals("name", "files", file.getName());
        }

        // Root -> Internal
        mFileManager.changeDir(new File(dirs.get(0).getAbsolutePath()));
        assertEquals("name", FileManager.STORAGE_INTERNAL, mFileManager.getStorage());
        // Root -> External 1
        mFileManager.changeDirRoot();
        mFileManager.changeDir(new File(dirs.get(1).getAbsolutePath()));
        assertEquals("name", FileManager.STORAGE_EXTERNAL, mFileManager.getStorage());
        // Root -> External 2
        mFileManager.changeDirRoot();
        mFileManager.changeDir(new File(dirs.get(2).getAbsolutePath()));
        assertEquals("name", FileManager.STORAGE_EXTERNAL + 1, mFileManager.getStorage());

        // Internal
        dir = mFileManager.changeDirTop(FileManager.STORAGE_INTERNAL);
        assertNotNull("null", dir);
        assertTrue("top", mFileManager.isTopDir());
        assertTrue("absolutePath", dir.getAbsolutePath().contains(mAppContext.getPackageName()));
        assertEquals("name", "files", dir.getName());
        dir = mFileManager.changeDirUp();
        assertNull("null", dir);

        // External 1
        dir = mFileManager.changeDirTop(FileManager.STORAGE_EXTERNAL);
        assertNotNull("null", dir);
        assertTrue("top", mFileManager.isTopDir());
        assertTrue("absolutePath", dir.getAbsolutePath().contains(mAppContext.getPackageName()));
        assertEquals("name", "files", dir.getName());
        dir = mFileManager.changeDirUp();
        assertNull("null", dir);

        // External 2
        dir = mFileManager.changeDirTop(FileManager.STORAGE_EXTERNAL + 1);
        assertNotNull("null", dir);
        assertTrue("top", mFileManager.isTopDir());
        assertTrue("absolutePath", dir.getAbsolutePath().contains(mAppContext.getPackageName()));
        assertEquals("name", "files", dir.getName());
        dir = mFileManager.changeDirUp();
        assertNull("null", dir);
    }

    @Test
    public void testInternalFiles() {
        testFiles(FileManager.STORAGE_INTERNAL);
    }

    @Test
    public void testExternalFiles1() {
        testFiles(FileManager.STORAGE_EXTERNAL);
    }

    @Test
    public void testExternalFiles2() {
        testFiles(FileManager.STORAGE_EXTERNAL + 1);
    }

    @Test
    public void testInternalDirs() {
        testDirs(FileManager.STORAGE_INTERNAL);
    }

    @Test
    public void testExternalDirs1() {
        testDirs(FileManager.STORAGE_EXTERNAL);
    }

    @Test
    public void testExternalDirs2() {
        testDirs(FileManager.STORAGE_EXTERNAL + 1);
    }
}
