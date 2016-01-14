/**
 *  kvChild
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
    name: "kvChild",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "child application for 'Keen Vent Manager', do not install directly.",
    category: "My Apps",
    parent: "MikeMaxwell:kvParent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "main")
}
def installed() {
	log.debug "Installed with settings: ${settings}"
	//initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}
def initialize() {
    subscribe(tempSensors, "temperature", ventHandler)
    //state.runMaps = []
    //log.info "stat state:${tStat.currentValue("thermostatOperatingState")} runMaps:${state.runMaps.size()}"
    //app.updateLabel("${settings.zoneName} Vent Zone") 
    
    
}
/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
	def installed = app.installationState == "COMPLETE"
    //log.info "app:${app.name} ${app.label}"
    //def zType = settings.zoneType
    //log.info "Installed:${installed} zoneType:${zType}"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){
		     section("Zone Devices"){
             		label(
                    	title		: "Name the zone"
                        ,required	: true
                    )
                    /*
					only stock device types work in the list below???
                    ticket submitted, as this should work, and seems to work for everyone except me...
					*/
                   input(
                        name			: "vents"
                        ,title			: "Keen vents in this Zone:"
                        ,multiple		: false
                        ,required		: true
                        //,type			: "device.KeenHomeSmartVent"
                        ,type			: "capability.switchLevel"
                        //,submitOnChange	: true
					)
                    //if (vents){
                    //	paragraph(getVentReport())
                    //}
					input(
            			name		: "tempSensors"
                		,title		: "Temp Sensors:"
                		,multiple	: false
                		,required	: true
                		,type		: "capability.temperatureMeasurement"
            		) 
                    /* out for now...
					input(
            			name		: "motionSensors"
                		,title		: "Motion Sensors:"
                		,multiple	: true
                		,required	: false
                		,type		: "capability.motionSensor"
            		)   
                    */
            }
            section("Zone Settings"){
					input(
            			name			: "minVo"
                		,title			: "Minimum vent opening"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options		: [["0":"0%"],["5":"5%"],["10":"10%"],["15":"15%"],["20":"20%"],["25":"25%"],["30":"30%"]]
                        ,defaultValue	: ["20"]
            		) 
					input(
            			name			: "maxVo"
                		,title			: "Maximum vent opening"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options		: [["50":"50%"],["55":"55%"],["60":"60%"],["65":"65%"],["70":"70%"],["80":"80%"],["100":"100%"]]
                        ,defaultValue	: ["100"]
            		) 
					input(
            			name			: "heatOffset"
                		,title			: "Zone heating offset, (above or below main thermostat)"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options		: [["-5":"-5°"],["-4":"-4°"],["-3":"-3°"],["-2":"-2°"],["-1":"-1°"],["0":"0°"],["1":"1°"],["2":"2°"],["3":"3°"],["4":"4°"],["5":"5°"]]
                        ,defaultValue	: ["0"]
            		) 
					input(
            			name			: "coolOffset"
                		,title			: "Zone cooling offset, (above or below main thermostat)"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options		: [["-5":"-5°"],["-4":"-4°"],["-3":"-3°"],["-2":"-2°"],["-1":"-1°"],["0":"0°"],["1":"1°"],["2":"2°"],["3":"3°"],["4":"4°"],["5":"5°"]]
                        ,defaultValue	: ["0"]
            		)                     
            }
	}
}
def appProps(){
	app.properties.each{ p ->
    	log.debug "appP:${p}"
    }
    return "whatevers..."
}
def getVentReport(){
	def report = []
    vents.each{ vent ->
    	def P = vent.currentState("pressure")
        def L = vent.currentState("level")
        def T = vent.currentState("temperature")
        def set = [P:[D:P.date.format("yyyy-MM-dd HH:mm:ss") ,V:P.value],L:[D:L.date.format("yyyy-MM-dd HH:mm:ss") ,V:L.value],T:[D:T.date.format("yyyy-MM-dd HH:mm:ss") ,V:T.value]]
        //log.debug "vent:${vent}"
        //vent.properties.each{ p ->
        //	log.info "property:${p}"
        //}
        //def set = [T:[D:T.date ,V:T.value]]
        report.add((vent.displayName):set)
    }
    return report.toString() ?: "nothing new..."
}

def ventHandler(evt){
	log.info "ventHandler- current zone temp:${evt.floatValue}, running:${state.running}, zone setPoint:${state.setPoint}"
    //def T1 = state.T1
    //def P1 = state.P1
    //def T2 = evt.floatValue
    //if (T1 && T2 && P1) log.info "adjusted pressure:${(P1 * T2)/T1}"

	if (state.running && state.setPoint){
    	if (evt.floatValue >= state.setPoint){
        	vents.setLevel(minVo.toInteger())
            state.running = false
            log.info "zone set point reached, setting vents to:${minVo.toInteger()}%"
        }
    }
}

def systemOn(setPoint,hvacMode){
	def cTemp = tempSensors.currentValue("temperature")
    //state.T1 = cTemp.toFloat()
    //state.P1 = vents.currentValue("pressure").toFloat()
    state.hvacMode = hvacMode
    
	if (hvacMode == "heating"){
    	state.setPoint = setPoint + heatOffset.toInteger()
    	if (cTemp < state.setPoint){
    		state.running = true
    		vents.setLevel(maxVo.toInteger())
    		log.info "System heat on, vents set to:${maxVo.toInteger()}"
    	} else {
    		state.running = false
    		log.info "System on, nothing to do, heating set point already met"
    	}         
    } else if (hvacMode == "cooling"){
    	state.setPoint = setPoint + coolOffset.toInteger()
    	if (cTemp >= state.setPoint){
    		state.running = true
    		vents.setLevel(maxVo.toInteger())
    		log.info "System cool on, vents set to:${maxVo.toInteger()}"
    	} else {
    		state.running = false
    		log.info "System on, nothing to do, cooling set point already met"
    	}         
    } else {
    	//something pithy here...
    }
    
    log.info "systemOn- mode:${hvacMode}, main setPoint:${setPoint}, zone setPoint:${state.setPoint}, current zone temp:${cTemp}"
     
}

def systemOff(){
	log.info "systemOff"
	state.running = false
}

def statHandler(evt){
	if (state.runMaps.size() < 10) {
		log.info "event:${evt.value}"
    	def key = evt.date.format("yyyy-MM-dd HH:mm:ss")
    	def v  = evt.value
    	def evtTime = evt.date.getTime()
    	if (v == "heating"){
    		//start
        	state.lastCalibrationStart = key
        	state.startTime = evtTime
        	state.startTemp = tempSensors.currentValue("temperature")
        	log.info "start -time:${state.startTime} -temp:${state.startTemp}"
    	} else if (v == "idle" && state.startTime) {
    		//end
        	state.endTime = evtTime
        	state.endTemp = tempSensors.currentValue("temperature")
        	log.info "end -time:${state.endTime} -temp:${state.endTemp}"
        
        	if (state.endTime > state.startTime && state.endTemp > state.startTemp ){
        		def BigDecimal dTemp  = (state.endTemp - state.startTemp)
            	def BigDecimal dTime = (state.endTime - state.startTime) / 3600000
            	def BigDecimal dph = dTemp / dTime
        		def value = ["dph":"${dph}" ,"dTime":"${dTime}" ,"dTemp":"${dTemp}", "vo":"${vents.currentValue("level")}"]
        		log.info "${value}"
            	if (state.runMaps.size == 0){
            		state.runMaps = ["${key}":"${value}"]
            	} else {
            		state.runMaps << ["${key}":"${value}"]
            	}
            	state.endTime = ""
            	state.startTime = ""
            	state.endTemp = ""
            	state.startTemp = ""
        	}
        }
    }
}
