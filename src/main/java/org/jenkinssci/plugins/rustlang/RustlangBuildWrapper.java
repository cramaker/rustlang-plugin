package org.jenkinsci.plugins.rustlang;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Map;

public class RustlangBuildWrapper extends BuildWrapper {

    private final String rustVersion;

    @DataBoundConstructor
    public RustlangBuildWrapper(String rustVersion) {
        this.rustVersion = rustVersion;
    }

    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        RustlangInstallation installation = getRustInstallation();
        if(installation != null) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(build.getBuildVariables());

            // Get the Rust version for this node, installing it if necessary
            installation = installation.forNode(Computer.currentComputer().getNode(), listener).forEnvironment(env);
        }

        // Apply the RUSTROOT and Rust binaries to PATH
        final RustlangInstallation install = installation;
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                if (install != null) {
                    EnvVars envVars = new EnvVars();
                    install.buildEnvVars(envVars);
                    env.putAll(envVars);
                }
            }
        };
    }

    private RustlangInstallation getRustInstallation() {
        for (RustlangInstallation i : ((DescriptorImpl) getDescriptor()).getInstallations()) {
            if (i.getName().equals(rustVersion)) {
                return i;
            }
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @CopyOnWrite
        private volatile RustlangInstallation[] installations = new RustlangInstallation[0];

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.SetUpRustTools();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public RustlangInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(RustlangInstallation... installations) {
            this.installations = installations;
            save();
        }

    }

}
