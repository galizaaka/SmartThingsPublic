/**
 *  zoneMotionChild v 1.0 2015-09-27
 *
 *  Copyright 2015 Mike Maxwell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "zoneMotionChild",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "Triggers Simulated Motion Sensor using multiple physical motion sensors.",
    category: "My Apps",
    parent: "MikeMaxwell:Zone Motion Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Zone configuration") {
		input(
            	name		: "motionSensors"
                ,title		: "Motion Sensors:"
                ,multiple	: true
                ,required	: true
                ,type		: "capability.motionSensor"
            )
            input(
            	name		: "simMotion"
                ,title		: "Virtual Motion Sensor:"
                ,multiple	: false
                ,required	: true
                ,type		: "device.simulatedMotionSensor"
            )
            input(
            	name		: "zoneEnableContacts"
                ,title		: "Contact Sensors that can enable this zone:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.contactSensor"
            )            
            input(
            	name		: "zoneEnableMotions"
                ,title		: "Motions Sensors that can enable this zone:"
                ,multiple	: true
                ,required	: false
                ,type		: "capability.motionSensor"
            )     
           input(
            	name		: "zoneEnableTimeout"
                ,title		: "Timeout for zone enable:"
                ,multiple	: false
                ,required	: false
                ,type		: "enum"
                ,options	: [10:"*10 seconds",15:"15 seconds",30:"30 seconds",45:"45 seconds",60:"1 minute",120:"2 minutes",180:"3 minutes"]
            )            
            input(
            	name		: "activateOnAll"
                ,title		: "Activate Zone on:"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: [0:"Any Sensor",1:"All Sensors"]
            )
            input(
            	name		: "activationWindow"
                ,title		: "Activation window time (used for All Sensors Option):"
                ,multiple	: false
                ,required	: false
                ,type		: "enum"
                ,options	: [1:"1 second",2:"*2 seconds",3:"3 seconds",4:"4 seconds",5:"5 seconds",6:"6 seconds",7:"7 seconds",8:"8 seconds",9:"9 seconds",10:"10 seconds"]
            )
			/*
			input(
            	name		: "lights"
                ,title		: "Select switch for testing..."
                ,multiple	: false
                ,required	: false
                ,type		: "capability.switch"
            )
            */
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
	subscribe(motionSensors, "motion", motionHandler)
    //state.activeDevices = motionSensors.size()
    //log.info "activeDevices:${state.activeDevices}"
}
def motionHandler(evt) {
    def activateOnAll = settings.activateOnAll == "1"
    def evtTime = evt.date.getTime()
	if (evt.value == "active") {
    	//log.debug "fired: ${evt.displayName}"
    	//log.debug "activteOnAll: ${activateOnAll}"    
        if (activateOnAll) {
    		if (allActive(evtTime)) {
            	activateZone()
            }
            /*
    		motionOK = motionSensors.currentState("motion").every{s-> s.value == "active" && (evtTime - s.date.getTime()) < window}
			if (motionOK) {
				activateZone()
			} else {
        		//log.warn "some sensors in motion"
            	stateMap.each{ s -> 
                	//show the sensors that did fire...
            		if (s.value < window) {
                		log.warn "${s.key} in motion..."
                	}
            	}
        	}
            */
        // activeOnAll == false
        } else {
            if (isZoneEnabled(evtTime)){
            	activateZone()
            }
        }
    //evt.value == something else
	} else {
    	//log.debug "evt.value:${evt.value}"
    	if (activateOnAll || allInactive()) {
			inactivateZone()
		}
	}
}
def isZoneEnabled(evtTime){
	//log.debug "isZoneEnabled - time:${evtTime}"
	def state = true
    def timeout = (settings.zoneEnableTimeout ?: 10).toInteger() * 1000
	//zoneEnableTimeout
	//zoneEnableContacts
    //zoneEnableMotions
    if (zoneEnableMotions){
    	state =	zoneEnableMotions.currentState("motion").any{s-> s.value == "active" && (evtTime - s.date.getTime()) < timeout}
        log.info "isZoneEnabled - zoneEnableMotions:${state}"
    } else
    if (zoneEnableContacts){
		state = zoneEnableContacts.currentState("contact").any{s-> s.value == "open" && (evtTime - s.date.getTime()) < timeout}  
        log.info "isZoneEnabled - zoneEnableContacts:${state}"
    }
    log.info "isZoneEnabled - final:${state}"
    return state
}
def allActive(evtTime){
	def state
    def window = (settings.activationWindow ?: 2).toInteger() * 1000
	state = motionSensors.currentState("motion").every{s-> s.value == "active" && (evtTime - s.date.getTime()) < window}
	log.info "allActive:${state}"
	return state
}
def inactivateZone(){
	if (simMotion.currentValue("motion") != "inactive") {
		log.info "Zone: ${simMotion.displayName} is inactive."
   		simMotion.inactive()
    }
}
def activateZone(){
	if (simMotion.currentValue("motion") != "active") {
		log.info "Zone: ${simMotion.displayName} is active."
   		simMotion.active()
    }
}
def allInactive () {
	def state	 
    state = motionSensors.currentValue("motion").contains("active")
    
    //if (motionSensors.currentValue("motion").contains("active")){
    //	state = false 
    //}
    log.debug "allInactive: ${state}"
	return state
}

