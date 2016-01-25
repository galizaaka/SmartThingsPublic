/**
 *  kvParent 0.0.8
 	
    0.0.8	other stuff to support the needs of the children
 	0.0.7	added fan run on delay
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
    name		: "kvParent",
    namespace	: "MikeMaxwell",
    author		: "Mike Maxwell",
    description	: "parent application for 'Keen Vent Manager'",
    category	: "My Apps",
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
	state.vParent = "0.0.8"
    //subscribe(tStat, "thermostatSetpoint", notifyZones) doesn't look like we need to use this
    subscribe(tStat, "thermostatMode", checkNotify)
    subscribe(tStat, "thermostatFanMode", checkNotify)
    subscribe(tStat, "thermostatOperatingState", checkNotify)
    subscribe(tStat, "heatingSetpoint", checkNotify)
    subscribe(tStat, "coolingSetpoint", checkNotify)

	checkNotify(null)
    
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
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){
		     section("Main Configuration"){
             		if (installed) app(name: "childZones", appName: "kvChild", namespace: "MikeMaxwell", title: "Create New Vent Zone...", multiple: true)
                   	input(
                        name			: "tStat"
                        ,title			: "Main Thermostat"
                        ,multiple		: false
                        ,required		: true
                        ,type			: "capability.thermostat"
                        ,submitOnChange	: false
                    )
					input(
            			name			: "tempSensors"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    input(
            			name			: "fanRunOn"
                		,title			: "Fan run on and set back notification delay:"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                		,options		: [["0":"No delay"],["60":"1 Minute"],["120":"2 Minutes"],["180":"3 Minutes"],["240":"4 Minutes"],["300":"5 Minutes"]]
                		,submitOnChange	: false
                   		,defaultValue	: "120"
            		)             
            }
            if (installed){
            	section("Reporting"){
         			href( "reporting"
						,title		: "Available reports..."
						,description: ""
						,state		: null
					)                
                }
                section (getVersionInfo()) { }
            }
	}
}

def reporting(){
	def report
	return dynamicPage(
    	name		: "reporting"
        ,title		: "Available zone reports"
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
            	report = "Configuration"
   				href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				) 
                report = "Current state"
                href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				)   
                report = "Last results"
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
    def hvacMap = getHVACstate()
    def mainHSP	= hvacMap.mainHSP
    def mainCSP	= hvacMap.mainCSP
    def mainMode = hvacMap.mainMode
    def mainOn = hvacMap.mainOn
    
    def reports = ""
    //def report
    //getZoneConfig()
    //getEndReport
	if (rptName == "Current state"){
    	cMethod = "getZoneState"
        def os = tStat.currentValue("thermostatOperatingState") ?: "No data available yet."
        def t = tempSensors.currentValue("temperature")
        reports = "Main system:\n\tmode: ${os}\n\tcooling set point: ${tempStr(mainCSP)}\n\theating set point: ${tempStr(mainHSP)}\n\tcurrent temp: ${tempStr(t)}\n\n"
    }
    if (rptName == "Configuration") cMethod = "getZoneConfig"
    if (rptName == "Last results") cMethod = "getEndReport"
    def sorted = childApps.sort{it.label}
    sorted.each{ child ->
    	//log.debug "getting child report for: ${child.label}"
        try {
    		def report = child."${cMethod}"()
        	reports = reports + child.label + ":${report}" + "\n"
        }
        catch(e){}
    }
    return reports
}

def getHVACstate(){
    def hvacMap = atomicState.hvacMap ?: null
    return hvacMap
    
    /*
    def hvacMap = getHVACstate()
    def mainHSP	= hvacMap.mainHSP
    def mainCSP	= hvacMap.mainCSP
    def mainMode = hvacMap.mainMode
    def mainOn = hvacMap.mainOn
    */
}

def checkNotify(evt){
	//log.debug "thermostat event- name: ${evt.name} value: ${evt.value} , description: ${evt.descriptionText}"

//get current states
    def csp = tStat.currentValue("coolingSetpoint").toFloat()
    def hsp = tStat.currentValue("heatingSetpoint").toFloat()
    def hvacMode = getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
    def mainOn = hvacMode != "idle"
    def delay = fanRunOn.toInteger()
    
    //get previous states
    def previousHVACmap  = atomicState.hvacMap ?: [mainCSP:csp,mainHSP:hsp,mainMode:hvacMode,mainOn:mainOn]
    def lastHSP = previousHVACmap.mainHSP.toFloat()
    def lastCSP = previousHVACmap.mainCSP.toFloat()
    def lastMode = previousHVACmap.mainMode
    
    //get state changes
    def isSetback = false
    if (mainOn){
    	isSetback = (hvacMode == "heating" && hsp < lastHSP ) || (hvacMode == "cooling" && csp > lastCSP)
    }
    
    def isSetPointChange = ((hsp != lastHSP) || (csp != lastCSP))
    
    def isModeChange = (evt == null || hvacMode != lastMode)
    
    //set states
    atomicState.hvacMap = [mainCSP:csp,mainHSP:hsp,mainMode:hvacMode,mainOn:mainOn,spChange:isSetPointChange,modeChange:isModeChange]
    
  
    
    //figure out the zone call
    if (isSetback && mainOn && delay != 0){
    	 log.trace "notify zones, scheduled in ${delay} seconds."
         runIn(delay,notifyZones)
    } else {
    	notifyZones()
    }
    
    //start pressure polling
    if (isModeChange && mainOn) runPoll()
     

    
    log.info "checkNotify-"
    log.trace "--hvacMode: ${hvacMode}, isChange: ${isModeChange}"
    log.trace "--isSetback: ${isSetback}"
    log.trace "--cooling setpoint: ${csp}" 
    log.trace "--heating set point: ${hsp}"
    
 
}

def runPoll(){
	log.trace "started vent polling"
    childApps.each {child ->
    	child.pollVents()
    }
    //atomicState.hvacMap = [mainCSP:csp,mainHSP:hsp,mainMode:hvacMode,mainOn:mainOn]
    def hvacMap = atomicState.hvacMap ?: null
    if (hvacMap == null) return
    if (hvacMap.mainOn) runIn(60,runPoll)
}

def notifyZones(){
    log.trace "notify zones- "
    childApps.each {child ->
    	child.zoneEvaluate("mainChange")
    }
}

def getNormalizedOS(os){
	def normOS = ""
    if (os == "heating" || os == "pending heat"){
    	normOS = "heating"
    } else if (os == "cooling" || os == "pending cool"){
    	normOS = "cooling"
    } else {
    	normOS = "idle"
    }
    //log.debug "normOS- in:${os}, out:${normOS}"
    return normOS
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

def getVersionInfo(){
	return "Versions:\n\tparent: ${state.vParent}\n\tchild: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
	//parent.updateVer(state.vChild)
    //state.vParent = "0.0.6"
    state.vChild = vChild
}

def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp) return "${temp.toString()}°${tc}"
    else return "No data available yet."
}
def getSelectedDevices(deviceList){
	def deviceIDS = []
    deviceList.each{ device ->
    	deviceIDS.add(device.id)
    }
	return deviceIDS
}
