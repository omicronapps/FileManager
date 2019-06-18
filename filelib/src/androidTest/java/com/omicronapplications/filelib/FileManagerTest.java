package com.omicronapplications.filelib;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    private void deleteTestFiles(boolean external) {
        mFileManager = new FileManager(mAppContext, external);
        mFileManager.changeDirTop();
        mFileManager.delete(TEST_FILE1);
        mFileManager.changeDir(TEST_DIR1);
        mFileManager.delete(TEST_FILE2);
        mFileManager.delete(TEST_FILE3);
        mFileManager.changeDirTop();
        mFileManager.delete(TEST_DIR1);
    }

    @Before
    public void setup() {
        mAppContext = InstrumentationRegistry.getTargetContext();
        deleteTestFiles(false);
        deleteTestFiles(true);
    }

    private void testFiles(boolean external) {
        mFileManager = new FileManager(mAppContext, external);
        File dir = mFileManager.getDir();

        assertEquals("changeDir", mFileManager.changeDir(dir), dir);
        String[] dirs = mFileManager.list();
        assertEquals("length", dirs.length, 0);
        File[] dirFiles = mFileManager.listFiles();
        assertEquals("length", dirFiles.length, 0);

        // Create new file
        mFileManager.createNewFile(TEST_FILE1);
        mFileManager.createNewFile(TEST_FILE2);
        mFileManager.createNewFile(TEST_FILE3);
        dirFiles = mFileManager.listFiles();
        assertEquals("length", dirFiles.length, 3);
        dirs = mFileManager.list();
        assertEquals("length", dirs.length, 3);

        // Delete
        for (String name : dirs) {
            assertTrue("delete", mFileManager.delete(name));
        }
        dirs = mFileManager.list();
        assertEquals("length", dirs.length, 0);
        dirFiles = mFileManager.listFiles();
        assertEquals("length", dirFiles.length, 0);
    }

    private void testDirs(boolean external) {
        mFileManager = new FileManager(mAppContext, external);
        File topDir = mFileManager.getDir();
        assertEquals("changeDir", mFileManager.changeDir(topDir), topDir);

        // Create dir
        File subDir1 = mFileManager.mkdir(TEST_DIR1);
        assertNotNull("mkdir", subDir1);
        assertEquals("changeDir", mFileManager.changeDir(TEST_DIR1), subDir1);

        // Create subdirs
        File subDir2 = mFileManager.mkdir(TEST_DIR2);
        assertNotNull("mkdir", subDir2);
        File subDir3 = mFileManager.mkdir(TEST_DIR3);
        assertNotNull("mkdir", subDir3);
        assertEquals("changeDir", mFileManager.changeDir(TEST_DIR3), subDir3);

        // Delete subdirs
        assertEquals("changeDir", mFileManager.changeDir(subDir1), subDir1);
        assertTrue("delete", mFileManager.delete(TEST_DIR2));
        assertTrue("delete", mFileManager.delete(TEST_DIR3));

        // Delete dir
        assertEquals("changeDir", mFileManager.changeDir(topDir), topDir);
        assertTrue("delete", mFileManager.delete(TEST_DIR1));
    }

    @Test
    public void testTopDirs() {
        mFileManager = new FileManager(mAppContext);

        // Internal
        File dir = mFileManager.changeDirTop(false);
        assertNotNull("null", dir);
        assertTrue("absolutePath", dir.getAbsolutePath().contains(mAppContext.getPackageName()));
        assertEquals("name", dir.getName(), "files");

        // External
        dir = mFileManager.changeDirTop(true);
        assertNotNull("null", dir);
        assertTrue("absolutePath", dir.getAbsolutePath().contains(mAppContext.getPackageName()));
        assertEquals("name", dir.getName(), "files");
    }

    @Test
    public void testInternalFiles() {
        testFiles(false);
    }

    @Test
    public void testExternalFiles() {
        testFiles(true);
    }

    @Test
    public void testInternalDirs() {
        testDirs(false);
    }
}
