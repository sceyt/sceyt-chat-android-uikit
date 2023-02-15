package com.sceyt.sceytchatuikit.media.audio;

import java.io.File;
import java.util.UUID;

public class FileManager {
    public static File createFile(String extension, String directory) {
        File mediaDir = new File(directory);
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }

        return new File(mediaDir, "Audio_" + UUID.randomUUID() + "." + extension);
    }
}
