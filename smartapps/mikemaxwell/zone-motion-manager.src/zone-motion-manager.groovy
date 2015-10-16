definition(
    name: "Zone Motion Manager",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "An example of parent/child SmartApps (this is the parent).",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Motion Control Zones", install: true, uninstall: true,submitOnChange: true) {
            section {
                    app(name: "childZones", appName: "zoneMotionChild", namespace: "MikeMaxwell", title: "Create New Motion Zone...", multiple: true)
            }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
            log.debug "child app: ${child.label}"
    }
}