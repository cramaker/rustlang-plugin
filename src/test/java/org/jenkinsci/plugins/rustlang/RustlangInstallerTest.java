package org.jenkinsci.plugins.rustlang;

import org.junit.Test;

import static org.jenkinsci.plugins.rustlang.RustlangInstaller.RustlangInstallable;
import static org.jenkinsci.plugins.rustlang.RustlangInstaller.RustlangRelease;
import static org.jenkinsci.plugins.rustlang.RustlangInstaller.InstallationFailedException;
import static org.junit.Assert.assertEquals;

public class RustlangInstallerTest {
    
    private static final RustlangInstallable LINUX_32 = createPackage("linux", "i686");
    private static final RustlangInstallable LINUX_64 = createPackage("linux", "x86_64");
    private static final RustlangInstallable FREEBSD_32 = createPackage("freebsd", "i686");
    private static final RustlangInstallable FREEBSD_64 = createPackage("freebsd", "x86_64");

    @Test(expected = InstallationFailedException.class)
    public void testUnsupportedOs() throws InstallationFailedException {
        // Given we have configured a release we want to install
        RustlangRelease release = createReleaseInfo();

        // When we try to get the install package for an OS which is not supported
        RustlangInstaller.getInstallCandidate(release, "Android", "armv7a");

        // Then an exception should be thrown
    }

    private static RustlangRelease createReleaseInfo() {
        RustlangRelease release = new RustlangRelease();
        release.variants = new RustlangInstallable[] {
                LINUX_32,
                LINUX_64,
        };
        return release;
    }

    private static RustlangInstallable createPackage(String os, String arch) {
        return createPackage(os, arch);
    }

}
