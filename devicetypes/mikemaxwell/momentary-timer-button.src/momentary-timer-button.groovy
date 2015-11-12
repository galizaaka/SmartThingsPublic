/*
	Timmer button
*/
metadata {
	definition (name: "Momentary Timer Button", namespace: "MikeMaxwell", author: "Mike Maxwell") {
		capability "Switch"
		capability "Momentary"
        capability "Switch Level"
	}

	// simulator metadata
	simulator {
	}
    preferences {
       	input( 
        	name		: "delay"
        	,type		: "enum"
            ,title		: "Delay time:"
            ,required	: true
            ,options	: [["30":"30 Seconds"],["60":"1 Minute"],["120":"2 Minutes"],["300":"5 Minutes"],["600":"10 Minutes"]]
        )
    }

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Start', action: "momentary.push", backgroundColor: "#ffffff" //, nextState: "running"
			state "on", label: 'Running', action: "momentary.push", backgroundColor: "#fe7f00"
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
    log.info "on..."
	push()
}

def off() {
	log.info "off..."
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "momentary", value: "pushed", isStateChange: true)
}
def setLevel(delayMinutes){
	def delay = delayMinutes * 60
    runDelay(delay)
}
def runDelay(seconds){
	log.info "running - delay:${seconds}"
	sendEvent(name: "switch", value: "on")
	runIn(seconds,off)
}