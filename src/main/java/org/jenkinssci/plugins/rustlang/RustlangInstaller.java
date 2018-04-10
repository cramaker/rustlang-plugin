package org.jenkinsci.plugins.rustlang;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.DownloadService;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.VersionNumber;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Installs the Rust programming language tools by downloading the archive for the detected OS/architecture combo. */
public class RustlangInstaller extends DownloadFromUrlInstaller {

    @DataBoundConstructor
    public RustlangInstaller(String id) {
        super(id);
    }

    // This is essentially the parent implementation, but we override it so we can pass Node into getInstallable()
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException,
            InterruptedException {
        FilePath expectedPath = preferredLocation(tool, node);

        Installable installable;
        try {
            installable = getInstallable(node);
        } catch (InstallationFailedException e) {
            throw new InstallationFailedException(Messages.CouldNotInstallRust(e.getMessage()));
        }

        if (installable == null) {
            log.getLogger().println(Messages.UnrecognisedReleaseId(id));
            return expectedPath;
        }

        if (isUpToDate(expectedPath, installable)) {
            return expectedPath;
        }

        String message = Messages.InstallingRustOnNode(installable.url, expectedPath, node.getDisplayName());
        if (expectedPath.installIfNecessaryFrom(new URL(installable.url), log, message)) {
            expectedPath.child(".timestamp").delete(); // we don't use the timestamp
            FilePath base = findPullUpDirectory(expectedPath);
            if (base != null && base != expectedPath)
                base.moveAllChildrenTo(expectedPath);
            // leave a record for the next up-to-date check
            new File (expectedPath + "/local").mkdir();
            String installScript = expectedPath + "/install.sh --prefix=" + expectedPath + "/local";
            ProcessBuilder runInstallScript = new ProcessBuilder(installScript);
            Process runInstall = runInstallScript.start();
            expectedPath.child(".installedFrom").write(installable.url, "UTF-8");
        }

        return expectedPath;
    }

    private Installable getInstallable(Node node) throws IOException, InterruptedException {
        // Get the Rust release that we want to install
        RustlangRelease release = getConfiguredRelease();
        if (release == null) {
            return null;
        }

        // Gather properties for the node to install on
        String[] properties = node.getChannel().call(new GetSystemProperties("os.arch", "os.name"));

	        // Get the best matching install candidate for this node
        return getInstallCandidate(release, properties[0], properties[1]);
    }

    @VisibleForTesting
    static RustlangInstallable getInstallCandidate(RustlangRelease release, String osArch, String osName)
            throws InstallationFailedException {
        String platform = getPlatform(osName);
        String architecture = getArchitecture(osArch);

        // Sort and search for an appropriate variant
        List<RustlangInstallable> variants = Arrays.asList(release.variants);
        Collections.sort(variants);
        for (RustlangInstallable i : variants) {
            if (i.os.equals(platform) && i.arch.equals(architecture)) {
                return i;
            }
        }

        throw new InstallationFailedException(Messages.NoInstallerForOs(release.name, osArch, osName));
    }

    private RustlangRelease getConfiguredRelease() {
        for (RustlangRelease r : ((DescriptorImpl) getDescriptor()).getInstallableReleases()) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<RustlangInstaller> {
        public String getDisplayName() {
            return Messages.InstallFromWebsite();
        }

        // Used by config.groovy to show a human-readable list of releases
        public List<RustlangRelease> getInstallableReleases()  {
            return RustlangReleaseList.all().get(RustlangReleaseList.class).toList();
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == RustlangInstallation.class;
        }
    }

    @Extension
    public static final class RustlangReleaseList extends DownloadService.Downloadable {

        // Public for JSON deserialisation
        public List<RustlangRelease> releases;

        public RustlangReleaseList() {
            super(RustlangInstaller.class);
        }

        /** @return A list of available Rust releases, obtained by parsing the JSON received from the update centre. */
        public List<RustlangRelease> toList() {
            JSONObject root;
            try {
                root = getData();
                if (root == null) {
                    // JSON file has not been downloaded by Jenkins yet
                    return null;
                }
            } catch (IOException e) {
                return null;
            }

            Map<String, Class> classMap = new HashMap<String, Class>();
            classMap.put("releases", RustlangRelease.class);
            classMap.put("variants", RustlangInstallable.class);
            return ((RustlangReleaseList) JSONObject.toBean(root, RustlangReleaseList.class, classMap)).releases;
        }
    }

    // Needs to be public for JSON deserialisation
    public static class RustlangRelease {
        public String id;
        public String name;
        public RustlangInstallable[] variants;
    }

    // Needs to be public for JSON deserialisation
    public static class RustlangInstallable extends Installable implements Comparable<RustlangInstallable> {
        public String os;
        public String arch;

        public int compareTo(RustlangInstallable o) {
            // Otherwise we don't really care; sort by OS name
            return os.compareTo(o.os);
        }

        @Override
        public String toString() {
            return String.format("RustlangInstallable[arch=%s, os=%s]", arch, os);
        }
    }

    /** @return The OS value used in a Rust archive filename, for the given {@code os.name} value. */
    private static String getPlatform(String os) throws InstallationFailedException {
        String value = os.toLowerCase(Locale.ENGLISH);
        if (value.contains("freebsd")) {
            return "freebsd";
        }
        if (value.contains("linux")) {
            return "linux-gnu";
        }
        if (value.contains("os x")) {
            return "darwin";
        }
        if (value.contains("windows")) {
            return "windows";
        }
        throw new InstallationFailedException(Messages.UnsupportedOs(os));
    }

    /** @return The CPU architecture value used in a Rust archive filename, for the given {@code os.arch} value. */
    private static String getArchitecture(String arch) throws InstallationFailedException {
        String value = arch.toLowerCase(Locale.ENGLISH);
        if (value.contains("amd64") || value.contains("86_64")) {
            return "x86_64";
        }
        if (value.contains("86")) {
            return "i686";
        }
        throw new InstallationFailedException(Messages.UnsupportedCpuArch(arch));
    }

    /** Returns the values of the given Java system properties. */
    private static class GetSystemProperties extends MasterToSlaveCallable<String[], InterruptedException> {
        private static final long serialVersionUID = 1L;

        private final String[] properties;

        GetSystemProperties(String... properties) {
            this.properties = properties;
        }

        public String[] call() {
            String[] values = new String[properties.length];
            for (int i = 0; i < properties.length; i++) {
                values[i] = System.getProperty(properties[i]);
            }
            return values;
        }
    }

    // Extend IOException so we can throw and stop the build if installation fails
    static class InstallationFailedException extends IOException {
        InstallationFailedException(String message) {
            super(message);
        }
    }

}
