package org.jenkinsci.plugins.rustlang;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RustlangInstallation extends ToolInstallation implements EnvironmentSpecific<RustlangInstallation>,
        NodeSpecific<RustlangInstallation> {

    @DataBoundConstructor
    public RustlangInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        String root = getHome();
        if (root != null) {
            env.put("RUSTROOT", root);
            env.put("PATH_CARGOROOT_BIN", new File(root, "cargo/bin").toString());
            env.put("PATH_RUSTCROOT_BIN", new File(root, "rustc/bin").toString());
            env.put("LIBRARY_PATH", new File(root, "rust-std-x86_64-unknown-linux-gnu/lib/rustlib/x86_64-unknown-linux-gnu/lib").toString());
        }
    }

    public RustlangInstallation forEnvironment(EnvVars environment) {
        return new RustlangInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public RustlangInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new RustlangInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    @Symbol("rust")
    public static class DescriptorImpl extends ToolDescriptor<RustlangInstallation> {

        @Override
        public String getDisplayName() {
            return "Rust";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new RustlangInstaller(null));
        }

        @Override
        public RustlangInstallation[] getInstallations() {
            return Jenkins.getInstance()
                    .getDescriptorByType(RustlangBuildWrapper.DescriptorImpl.class)
                    .getInstallations();
        }

        @Override
        public void setInstallations(RustlangInstallation... installations) {
            Jenkins.getInstance()
                    .getDescriptorByType(RustlangBuildWrapper.DescriptorImpl.class)
                    .setInstallations(installations);
        }

    }

}
