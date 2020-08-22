# FileManager

> File management tools

## Description

FileManager is an example of a minimalistic helper class for file management on Android.

FileManager is used in [AndPlug](https://play.google.com/store/apps/details?id=com.omicronapplications.andplug) music player application for Android devices.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Testing](#testing)
- [Usage](#usage)
- [Example](#example)
- [Credits](#credits)
- [Release History](#release-history)
- [License](#license)

## Prerequisites

- [Android 4.0.3](https://developer.android.com/about/versions/android-4.0.3) (API Level: 15) or later (`ICE_CREAM_SANDWICH_MR1`)
- [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 4.0.0 or later (`gradle:4.0.0`)

## Installation

1. Check out a local copy of FileManager repository
2. Build library with Gradle, using Android Studio or directly from the command line

## Testing

FileManager includes a set of instrumented unit tests.

### Instrumented tests

Located under `filelib/src/androidTest`.

These tests are run on a hardware device or emulator, and verifies correct operation of the `FileManager` implementation.

## Usage

FileManager is controlled through the following class:
- `FileManager` - File management tools class

## Example

Create new `FileManager` instance, using application external storage 1:

```
FileManager fileManager = new FileManager(getApplicationContext(), FileManager.STORAGE_EXTERNAL_1);
```

Create and enter new directory: 

```
File dir = fileManager.mkdir("test_dir");
dir = fileManager.changeDir("test_dir");
```

List files in current folder:

```
File[] dirs = fileManager.listFiles();
for (File file : dirs) {
    // ...
}
```

Get current directory and create new file:

```
File dir = fileManager.getDir();
File file = fileManager.createNewFile("test.txt");
```

Get and rename file:

```
File file = fileManager.getFile("test.txt);
fileManager.renameTo(file, "file.txt");
```

Delete file and change to top directory: 

```
fileManager.delete("file.txt");
File dir = fileManager.changeDirTop();
```

Change to application internal storage:

```
File dir = fileManager.changeDirTop(FileManager.STORAGE_INTERNAL);
```

## Credits

Copyright (C) 2019-2020 [Fredrik Claesson](https://github.com/omicronapps)

## Release History

- 1.0.0 Initial release
- 1.1.0 Support for additional external storage device, where available (Android KitKat 4.4 and later only)
  Detection and callbacks for mounting and removal of external storage devices
- 1.2.0 Migrated to AndroidX
- 1.3.0 Added support for sorting files

## License

FileManager is licensed under [GNU LESSER GENERAL PUBLIC LICENSE](LICENSE).
