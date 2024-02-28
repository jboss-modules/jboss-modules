package org.jboss.modules;

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
    }

    @Test
    public void testReadable() {
        LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
    }

    @Test
    public void testUnreadableOverlays() throws IOException {
        boolean expectedFailure = false;
        Set<PosixFilePermission> origPermissions = Files.getPosixFilePermissions(overlaysDir.toPath());
        try {
            // make directory non-readable
            Set<PosixFilePermission> testPermissions = PosixFilePermissions.fromString("-w-------");
            Files.setPosixFilePermissions(overlaysDir.toPath(), testPermissions);

            LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
        } catch (IllegalStateException ise) {
            expectedFailure = ise.getMessage().startsWith("Overlays directory exists but is not readable: ");
        } finally {
            Files.setPosixFilePermissions(overlaysDir.toPath(), origPermissions);
        }

        Assert.assertTrue(expectedFailure);
    }

    @Test
    public void testUnreadableOverlaysMetadataFile() throws IOException {
        boolean expectedFailure = false;
        Set<PosixFilePermission> origPermissions = Files.getPosixFilePermissions(overlaysDir.toPath());
        try {
            // make directory non-readable
            Set<PosixFilePermission> testPermissions = PosixFilePermissions.fromString("-w-------");
            Files.setPosixFilePermissions(metadataFile.toPath(), testPermissions);

            try {
                LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
            } catch (IllegalStateException ise) {
                expectedFailure = ise.getMessage().startsWith("Overlays metadata file exists but is not readable: ");
            }
        } finally {
            Files.setPosixFilePermissions(overlaysDir.toPath(), origPermissions);
        }

        Assert.assertTrue(expectedFailure);
    }

    @Test
    public void testUnreadableOverlayRoot() throws IOException {
        boolean expectedFailure = false;
        Set<PosixFilePermission> origPermissions = Files.getPosixFilePermissions(overlaysDir.toPath());
        try {
            // make directory non-readable
            Set<PosixFilePermission> testPermissions = PosixFilePermissions.fromString("-w-------");
            Files.setPosixFilePermissions(overlayRoot.toPath(), testPermissions);

            LayeredModulePathFactory.loadOverlays(layeringRoot, discoveredPaths);
        } catch (IllegalStateException ise) {
            expectedFailure = ise.getMessage().startsWith("Overlay root directory doesn't exists or is not readable: ");
        } finally {
            Files.setPosixFilePermissions(overlaysDir.toPath(), origPermissions);
        }

        Assert.assertTrue(expectedFailure);
    }

    private static void writeRefsFile(File file) throws IOException {
        Files.write(file.toPath(), (OVERLAY_NAME + '\n').getBytes());
    }

}
