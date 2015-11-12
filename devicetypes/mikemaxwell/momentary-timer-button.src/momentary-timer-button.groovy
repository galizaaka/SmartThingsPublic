/*
	Timmer button v1
    When the device receives an on() command it will turn off and issue a button 1 pressed event after the delay as configured in preferences.
    When the device receives a setLevel() command, it will turn off and issue a button 1 pressed event using the delay time in minutes sent via setLevel.
*/
metadata {
	definition (name: "Momentary Timer Button", namespace: "MikeMaxwell", author: "Mike Maxwell") {
		capability "Switch"
		capability "Momentary"
        capability "Switch Level"
        capability "Button"
	}

	simulator {
	}
    preferences {
       	input( 
        	name		: "delay"
        	,type		: "enum"
            ,title		: "Delay time:"
            ,required	: true
            ,options	: [["30":"30 Seconds"],["60":"1 Minute"],["120":"2 Minutes"],["300":"5 Minutes"],["600":"10 Minutes"],["900":"15 Minutes"],["1800":"30 Minutes"],["3600":"1 Hour"],["7200":"2 Hours"]]
        )
    }

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Start', action: "momentary.push", backgroundColor: "#ffffff", icon: "st.illuminance.illuminance.dark"
			state "on", label: 'Running', action: "momentary.push", backgroundColor: "#fe7f00", icon: "st.Office.office6"
		}
		main "switch"
		details "switch"
	}
}

def parse(String description) {
}

def push() {
    def delay = settings.delay.toInteger() ?: 10
    runDelay(delay)
}

def on() {
    log.info "starting via on method"
	push()
}

def off() {
	log.info "off..."
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "button", value: "pushed", data:[buttonNumber: '1'], isStateChange: true)
}
def setLevel(delayMinutes){
	log.info "starting via setLevel method"
	def delay = delayMinutes * 60
    runDelay(delay)
}
def runDelay(seconds){
	log.info "on, running - delay:${seconds}"
	sendEvent(name: "switch", value: "on")
	runIn(seconds,off)
}