package org.jboss.modules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class LayeredModulePathFactoryTest {

    private static final String OVERLAYS = ".overlays";
    private static final String OVERLAY_NAME = "overlay-1";

    @TempDir
    public Path temporaryFOlder;

    private File layeringRoot;
    private File overlaysDir;
    private File metadataFile;
    private File overlayRoot;
    private final List<File> discoveredPaths = new ArrayList<>();

    @BeforeEach
    public void setup() throws IOException {
        boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        assumeTrue(isPosix, "This test requires POSIX compatible OS");

        layeringRoot = temporaryFOlder.toFile();

        // layeringRoot/.overlays/
        overlaysDir = new File(layeringRoot, OVERLAYS);
        assertTrue(overlaysDir.mkdir());

        // layeringRoot/.overlays/.overlays file
        metadataFile = new File(overlaysDir, OVERLAYS);
        assertTrue(metadataFile.createNewFile());
        writeRefsFile(metadataFile);

        // layeringRoot/.overlays/overlay-1/
        overlayRoot = new File(overlaysDir, OVERLAY_NAME);
        assertTrue(overlayRoot.mkdir());
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

        assertTrue(expectedFailure);
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

        assertTrue(expectedFailure);
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

        assertTrue(expectedFailure);
    }

    private static void writeRefsFile(File file) throws IOException {
        Files.write(file.toPath(), (OVERLAY_NAME + '\n').getBytes());
    }

}
