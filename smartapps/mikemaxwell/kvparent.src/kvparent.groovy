/**
 *  kvParent 0.0.4
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
    name: "kvParent",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "parent application for 'Keen Vent Manager'",
    category: "My Apps",
    iconUrl		: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url	: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan@2x.png"
)

preferences {
	page(name: "main")
    page(name: "reporting")
    page(name: "report")
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
    subscribe(tStat, "thermostatOperatingState", notifyZones)
    subscribe(tStat, "thermostatSetpoint", notifyZones)
    
 
    
    //subscribe(tStat, "thermostatHeatingSetpoint", setPointHandeler)
    /*
    state.runMaps = []
    state.runTimes = []
    state.lastDPH = 0
    state.endTime = ""
    state.startTime = ""
    state.endTemp = ""
    state.startTemp = ""
    state.crntDtemp = ""
	state.estDtime = ""
	state.lastCalibrationStart = ""
    log.info "stat state:${tStat.currentValue("thermostatOperatingState")} runMaps:${state.runMaps.size()}"
    //app.updateLabel("${settings.zoneName} Vent Zone") 
    */
    
}
/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
	def installed = app.installationState == "COMPLETE"
    //def zType = settings.zoneType
    //log.info "Installed:${installed} zoneType:${zType}"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){
		     section("Main Configuration"){
             		app(name: "childZones", appName: "kvChild", namespace: "MikeMaxwell", title: "Create New Vent Zone...", multiple: true)
                   	input(
                        name		: "tStat"
                        ,title		: "Main Thermostat"
                        ,multiple	: false
                        ,required	: true
                        ,type		: "capability.thermostat"
                    )
					input(
            			name		: "tempSensors"
                		,title		: "Thermostat temperature sensor:"
                		,multiple	: false
                		,required	: true
                		,type		: "capability.temperatureMeasurement"
            		) 
                    
            }
            if (installed){
            	section("Reporting"){
         			href( "reporting"
						,title		: "Available reports..."
						,description: ""
						,state		: null
						//,params		: [method:"addCommand",title:"Add Command"]
					)                
                }
            }
	}
}
def reporting(){
	def report
	return dynamicPage(
    	name		: "reporting"
        ,title		: "Available reports"
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
            	/*
            	report = "Zone Target"
   				href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				) 
                */
                report = "Zone States"
                href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				)   
            }
   }
}

def report(params){
	def reportName = params.rptName
	return dynamicPage(
    	name		: "report"
        ,title		: reportName
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
   				paragraph(getReport(reportName))
            }
   }
}

def getReport(rptName){
	def cMethod
    def reports = ""
    //def report
	if (rptName == "Zone States") cMethod = "getZoneState"
    //def sortedApps = childApps.sort()
	childApps.each{child ->
    	def report = child."${cMethod}"()
        //def report = child.name
        reports = reports + child.label + ":${report}" + "\n"
    }
    
    return reports
}


def notifyZones(evt){
	//log.debug "notifyZones- name:${evt.name} value:${evt.value} , description:${evt.descriptionText}"
    def setPoint
	def state = tStat.currentValue("thermostatOperatingState")
    
    if (state == "heating"){
    	setPoint = tStat.currentValue("heatingSetpoint").toInteger()
    } else if (state == "cooling"){
    	setPoint = tStat.currentValue("coolingSetpoint").toInteger()
    }
    
	if (evt.name == "thermostatOperatingState"){
		if (state == "heating" || state == "cooling"){
            log.info "notifying children of system operating state change..."
        	childApps.each {child ->
        		child.systemOn(setPoint,state)    
    		}
    	} else if (state == "idle"){
    		childApps.each {child ->
        		child.systemOff()
    		}
    	} else {
    		log.info "notifyZones- ignored:${state}"
    	}
    } else if (state == "heating" || state == "cooling"){
        //updateZoneSetpoint()
        log.info "notifying children of main set point change..."
        childApps.each {child ->
        		child.systemOn(setPoint,state)
    	}
    }
}

def statHandler(evt){
	log.info "event:${evt.value}"

    def key = evt.date.format("yyyy-MM-dd HH:mm:ss")
    def v  = evt.value
    def evtTime = evt.date.getTime()
    if (v == "heating"){
        state.lastCalibrationStart = key
        state.startTime = evtTime
        state.startTemp = tempSensors.currentValue("temperature")
        log.info "start -time:${state.startTime} -temp:${state.startTemp}"
    	if (!state.lastDPH){
        	state.lastDPH = 0	
        } else {
        	state.crntDtemp = tStat.currentValue("heatingSetpoint") -  state.startTemp
            state.estDtime = state.crntDtemp / state.lastDPH
            
        }
    } else if (v == "idle" && state.startTime) {
    	//end
        state.endTime = evtTime
        def BigDecimal dTime = (state.endTime - state.startTime) / 3600000
        state.endTemp = tempSensors.currentValue("temperature")
        log.info "end -time:${state.endTime} -temp:${state.endTemp}"
        if (state.runTimes.size == 0){
        	state.runTimes = ["${key}":"runTime:${dTime} startTemp:${state.startTemp} endTemp:${state.endTemp}"]
        } else {
        	state.runTimes << ["${key}":"runTime:${dTime} startTemp:${state.startTemp} endTemp:${state.endTemp}"]
        }
        if (state.endTime > state.startTime && state.endTemp > state.startTemp ){
        	def BigDecimal dTemp  = (state.endTemp - state.startTemp)
            
            def BigDecimal dph = dTemp / dTime
            if (dTime >= 0.5) {
               	def value = ["CurrentDPH":"${dph}","lastDPH":"${state.lastDPH}" ,"ActualRunTime":"${dTime}","EstimatedRunTime":"${state.estDtime}" ,"ActualTempRise":"${dTemp}","EstimatedTempRise":"${state.crntDtemp}"]
        		log.info "${value}"
            	if (state.runMaps.size == 0){
            		state.runMaps = ["${key}":"${value}"]
            	} else {
            		state.runMaps << ["${key}":"${value}"]
            	}
            }
            state.lastDPH = dph
            state.endTime = ""
            state.startTime = ""
            state.endTemp = ""
            state.startTemp = ""
        }
        
    }
}
/*
       if (cmdMaps) cmdMaps.put((nextIDX),cmd)
        else state.customCommands = [(nextIDX):cmd]
*/

