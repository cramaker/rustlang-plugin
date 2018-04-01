package org.jenkinsci.plugins.rustlang;

def f = namespace(lib.FormTagLib)

def installationsDefined = descriptor.installations.length != 0
def title = installationsDefined ? _("Rust version") :
    _("No setup will be done, as no Rust installations have been defined in the Jenkins system config")

f.entry(title: title) {
    if (installationsDefined) {
        select(class:"setting-input", name:".rustVersion") {
            descriptor.installations.each {
                f.option(selected: it.name == instance?.rustInstallation?.name, value: it.name, it.name)
            }
        }
    }
}
