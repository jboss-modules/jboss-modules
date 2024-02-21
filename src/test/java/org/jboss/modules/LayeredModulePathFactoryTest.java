package org.jboss.modules;

import org.jboss.modules.log.ModuleLogger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LayeredModulePathFactoryTest {

    private static final String OVERLAYS = ".overlays";
    private static final String OVERLAY_NAME = "overlay-1";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File layeringRoot;
    private File overlaysDir;
    private File metadataFile;
    private File overlayRoot;
    private final List<File> discoveredPaths = new ArrayList<>();

    private boolean overlaysDirNotReadableLogged = false;
    private boolean overlaysMetadataNotReadableLogged = false;
    private boolean overlayRootNotReadableLogged = false;

    @Before
    public void setup() throws IOException {
        boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        Assume.assumeTrue("This test requires POSIX compatible OS", isPosix);

        layeringRoot = temporaryFolder.getRoot();

        // layeringRoot/.overlays/
        overlaysDir = new File(layeringRoot, OVERLAYS);
        Assert.assertTrue(overlaysDir.mkdir());

        // layeringRoot/.overlays/.overlays file
        metadataFile = new File(overlaysDir, OVERLAYS);
        Assert.assertTrue(metadataFile.createNewFile());
        writeRefsFile(metadataFile);

        // layeringRoot/.overlays/overlay-1/
        overlayRoot = new File(overlaysDir, OVERLAY_NAME);
        Assert.assertTrue(overlayRoot.mkdir());

        // custom logger to detect warn messages being printed
        Module.setModuleLogger(new CustomLogger());
    }

    @Test
    public void testReadable() {
        LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);

        Assert.assertFalse(overlaysDirNotReadableLogged);
        Assert.assertFalse(overlaysMetadataNotReadableLogged);
        Assert.assertFalse(overlayRootNotReadableLogged);
    }

    @Test
    public void testUnreadableOverlays() throws IOException {
        // make directory non-readable
        Set<PosixFilePermission> origPermissions = Files.getPosixFilePermissions(overlaysDir.toPath());
        try {
            Set<PosixFilePermission> testPermissions = PosixFilePermissions.fromString("-w-------");
            Files.setPosixFilePermissions(overlaysDir.toPath(), testPermissions);

            LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
        } finally {
            Files.setPosixFilePermissions(overlaysDir.toPath(), origPermissions);
        }

        Assert.assertTrue(overlaysDirNotReadableLogged);
        Assert.assertFalse(overlaysMetadataNotReadableLogged);
        Assert.assertFalse(overlayRootNotReadableLogged);
    }

    @Test
    public void testUnreadableOverlaysMetadataFile() throws IOException {
        // make directory non-readable
        Set<PosixFilePermission> origPermissions = Files.getPosixFilePermissions(overlaysDir.toPath());
        try {
            Set<PosixFilePermission> testPermissions = PosixFilePermissions.fromString("-w-------");
            Files.setPosixFilePermissions(metadataFile.toPath(), testPermissions);

            try {
                LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
            } catch (RuntimeException e) {
                // ignore
            }
        } finally {
            Files.setPosixFilePermissions(overlaysDir.toPath(), origPermissions);
        }

        Assert.assertFalse(overlaysDirNotReadableLogged);
        Assert.assertTrue(overlaysMetadataNotReadableLogged);
        Assert.assertFalse(overlayRootNotReadableLogged);
    }

    @Test
    public void testUnreadableOverlayRoot() throws IOException {
        // make directory non-readable
        Set<PosixFilePermission> origPermissions = Files.getPosixFilePermissions(overlaysDir.toPath());
        try {
            Set<PosixFilePermission> testPermissions = PosixFilePermissions.fromString("-w-------");
            Files.setPosixFilePermissions(overlayRoot.toPath(), testPermissions);

            LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
        } finally {
            Files.setPosixFilePermissions(overlaysDir.toPath(), origPermissions);
        }

        Assert.assertFalse(overlaysDirNotReadableLogged);
        Assert.assertFalse(overlaysMetadataNotReadableLogged);
        Assert.assertTrue(overlayRootNotReadableLogged);
    }

    private static void writeRefsFile(File file) throws IOException {
        Files.write(file.toPath(), (OVERLAY_NAME + '\n').getBytes());
    }

    /**
     * This is a custom logger that detects if specific messages has been logged.
     */
    private class CustomLogger implements ModuleLogger {

        @Override
        public void trace(String message) {

        }

        @Override
        public void trace(String format, Object arg1) {

        }

        @Override
        public void trace(String format, Object arg1, Object arg2) {

        }

        @Override
        public void trace(String format, Object arg1, Object arg2, Object arg3) {

        }

        @Override
        public void trace(String format, Object... args) {

        }

        @Override
        public void trace(Throwable t, String message) {

        }

        @Override
        public void trace(Throwable t, String format, Object arg1) {

        }

        @Override
        public void trace(Throwable t, String format, Object arg1, Object arg2) {

        }

        @Override
        public void trace(Throwable t, String format, Object arg1, Object arg2, Object arg3) {

        }

        @Override
        public void trace(Throwable t, String format, Object... args) {

        }

        @Override
        public void greeting() {

        }

        @Override
        public void classDefineFailed(Throwable throwable, String className, Module module) {

        }

        @Override
        public void classDefined(String name, Module module) {

        }

        @Override
        public void providerUnloadable(String name, ClassLoader loader) {

        }

        @Override
        public void jaxpClassLoaded(Class<?> jaxpClass, Module module) {

        }

        @Override
        public void jaxpResourceLoaded(URL resourceURL, Module module) {

        }

        @Override
        public void overlaysDirectoryNotReadable(File file) {
            overlaysDirNotReadableLogged = true;
        }

        @Override
        public void overlaysMetadataNotReadable(File file) {
            overlaysMetadataNotReadableLogged = true;
        }

        @Override
        public void overlayRootNotReadable(File file) {
            overlayRootNotReadableLogged = true;
        }
    }
}
