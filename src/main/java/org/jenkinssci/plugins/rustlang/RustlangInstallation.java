package org.jenkinsci.plugins.rustlang;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
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
            env.put("PATH+RUSTROOT_BIN", new File(root, "bin").toString());
        }
    }

    public RustlangInstallation forEnvironment(EnvVars environment) {
        return new RustlangInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public RustlangInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new RustlangInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<RustlangInstallation> {

        @Override
        public String getDisplayName() {
            return "Rust";
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
