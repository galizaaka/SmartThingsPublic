/**
 *  kvChild 0.0.8
 
 	0.0.8	actually got zone update support working
 			fixed bug, zone not restarting
    		added app version info on the bottom of the parent page
            added dynamic max opening selection
    0.0.6a	added interim debugging
    0.0.6	added options on what to do when the zone is disabled...
    0.0.5	added disable switch option
    0.0.4	basic reporting
    0.0.3 	added dynamic zone change support while system is running
    		added support for main set point updates while system is running
    0.0.2	added F/C unit detection and display
    
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


//dynamic page methods
def main(){
	def installed = app.installationState == "COMPLETE"
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
                    ,multiple		: true
                    ,required		: true
                    //,type			: "device.KeenHomeSmartVent"
                    ,type			: "capability.switchLevel"
                    ,submitOnChange	: soc
				)
 				input(
            		name		: "tempSensors"
                	,title		: "Temp Sensors:"
                	,multiple	: false
                	,required	: true
                	,type		: "capability.temperatureMeasurement"
                    ,submitOnChange	: soc
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
                    ,options		: minVoptions()
                    ,submitOnChange	: true
            	) 
                if (minVo){
				input(
            		name			: "maxVo"
                	,title			: "Maximum vent opening"
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
                    ,options		: maxVoptions()
                    ,defaultValue	: ["100"]
                    ,submitOnChange	: soc
            	) 
                }
				input(
            		name			: "heatOffset"
                	,title			: "Heating offset, (above or below main thermostat)"
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
                    ,options 		: zoneTempOptions()
                    ,defaultValue	: ["0"]
                    ,submitOnChange	: soc
            	) 
				input(
            		name			: "coolOffset"
                	,title			: "Cooling offset, (above or below main thermostat)"
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
                    ,options 		: zoneTempOptions()
                    ,defaultValue	: ["0"]
                    ,submitOnChange	: soc
            	)
            }
            section("Zone options"){
            
            	input(
            		name			: "ventCloseWait"
                	,title			: "Close vents at cycle completion?"
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
                	,options		: [["-1":"Do not close"],["0":"Immediately"],["60":"After 1 Minute"],["120":"After 2 Minutes"],["300":"After 5 Minutes"]]
                	,submitOnChange	: false
                   	,defaultValue	: "-1"
            	)
                
            	def zcsTitle = zoneControlSwitch ? "Optional zone disable switch: when on, zone is enabled, when off, zone is disabled " : "Optional zone disable switch"
                input(
            		name			: "zoneControlSwitch"
                	,title			: zcsTitle 
                	,multiple		: false
                	,required		: false
                	,type			: "capability.switch"
                    ,submitOnChange	: true
            	)  
                /*
                if (zoneControlSwitch){
                   	def zioTitle = zoneInactiveOptions ? "When zone is disabled, set vents to:" : "When zone is disabled, vents will not be changed"
                   	input(
            			name			: "zoneInactiveOptions"
                		,title			: zioTitle
                		,multiple		: false
                		,required		: false
                		,type			: "enum"
                       	,options		: minVoptions()
                       	,submitOnChange	: true
            		)
                    if (zoneInactiveOptions){
                   		input(
            				name			: "zoneInactivateWhen"
                			,title			: "When zone is disabled, close vents..."
                			,multiple		: false
                			,required		: false
                			,type			: "enum"
                       		,options		: [["0":"Do not close vents"],["120":"2 minutes after cycle end"],["-1":"immediately"]]
                       		,submitOnChange	: true
                           	,defaultValue	: "120"
            			)                        
                    }
                }
                */
            }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	state.vChild = "0.0.8"
    parent.updateVer(state.vChild)
    subscribe(tempSensors, "temperature", tempHandler)
    subscribe(vents, "pressure", getAdjustedPressure)
    
    subscribe(vents, "level", levelHandler)
    subscribe(zoneControlSwitch,"switch",zoneDisableHandeler)
    
	zoneEvaluate("init")
}

//zone control methods

def zoneEvaluate(params){
    //get current main state
    //[mainCSP:csp,mainHSP:hsp,mainMode:hvacMode,mainOn:mainOn,spChange:isSetPointChange]
    def hvacMap = parent.getHVACstate()
    if (hvacMap == null) return
    def mainCSP	= hvacMap.mainCSP
    def mainHSP	= hvacMap.mainHSP
    def mainMode = hvacMap.mainMode
    def mainOn = hvacMap.mainOn
    def spChange = hvacMap.spChange
    def modeChange = hvacMap.modeChange
    
	def running = state.running
    def zoneDisablePending = state.zoneDisablePending ?: false
    
	def zoneTemp = tempSensors.currentValue("temperature").toFloat()
    def zoneDisabled = false
    if (zoneControlSwitch) zoneDisabled = zoneControlSwitch.currentValue("switch") == "off" 
    
    //get change states
    def tempChanged = false
    def mainChanged = false
    def voChanged = false
    def initChanged = false
    def zoneDisableChanged = false
    
    log.debug "zoneEvaluate- parameters: ${params}"
    //parse params
    if (params){
    	switch (params){
        	case "zoneDisable" :
                zoneDisableChanged = true
                zoneDisablePending = zoneDisabled
            	break
        	case "mainChange" :
            	mainChanged = true
            	break
        	case "tempChange" :
            	tempChanged = true
            	break
        	case "init" :
            	initChanged = true
            	break
    	}
    }
    
    //get current zone settings
    def csp = mainCSP + coolOffset.toInteger()
    def hsp = mainHSP + heatOffset.toInteger()
    def maxVo = settings.maxVo.toInteger()
    def minVo = settings.minVo.toInteger()
    def cvo = atomicState.voRequest
  
 
    
    def canRun = (tempChanged || mainChanged || initChanged ) 
    def runNow = (canRun && (!zoneDisabled || zoneDisablePending))

    log.debug "change states- run: ${runNow}, tempChanged: ${tempChanged}, mainChanged: ${mainChanged}, initChanged: ${initChanged}, zoneDisablePending: ${zoneDisablePending}, zoneDisabled: ${zoneDisabled}"
    
    //local only
    def setPoint
	def mainSetPoint
    
    //rune zone management
 	if (runNow){
    	def spMet = false
        running = true
    	if (mainMode == "heating"){
        	setPoint = hsp
            mainSetPoint = mainHSP
            
			spMet = zoneTemp >= hsp
            if (spMet){
            	cvo = setVents(minVo)
            	running = false
            } else {
            	cvo = setVents(maxVo)
            }
        } else if (mainMode == "cooling"){
        	setPoint = csp
            mainSetPoint = mainCSP
        	spMet = zoneTemp <= csp
        	if (spMet){
            	cvo = setVents(minVo)
            	running = false
        	} else {
            	cvo = setVents(maxVo)
            }            
        } else if (mainMode == "idle"){
        	running = false
            zoneDisablePending = false
            log.info "zoneEvaluate- main HVAC is idle"
    		def zsp = hsp-- ?: 0
    		def d = zoneTemp - zsp
        	d = d.round(1)
    		state.endReport = "\n\tset point: ${tempStr(zsp)}\n\tend temp: ${tempStr(zoneTemp)}\n\tvariance: ${tempStr(d)}\n\tvent levels: ${vents.currentValue("level")}%"        
       
            if (zoneDisabled){ 
               	log.warn "Vents closed via zone disable switch"
               	cvo = setVents(0)
            }

			def closeOption = ventCloseWait.toInteger()
        	if (closeOption == 0){
        		log.warn "Vents closed via Close vents option"
        		cvo = setVents(0)
        	} else if (closeOption > 0){
        		log.warn "Vent closing scheduled via Close vents option"
        		runIn(closeOption,delayClose)
        	}
            
        } else {
        	//udf mode
        }
        
    } else {
    	//nothing to do
    }
    
    //write vars
    state.mainMode = mainMode
    state.zoneTemp = zoneTemp
    state.zoneCSP = csp
    state.zoneHSP = hsp
    state.maxVo = maxVo
    state.minVo = minVo
    state.running = running
    state.zoneDisabled = zoneDisabled
    state.zoneDisablePending = zoneDisablePending
    
    
    log.info "zoneEvaluate--"
    log.trace "--hvacMode: ${mainMode}"
    log.trace "--zone running: ${running}"
    log.trace "--zone disabled: ${zoneDisabled}"
    log.trace "--zone active set point: ${tempStr(setPoint)}"
    //log.trace "--zone cooling set point: ${tempStr(csp)}"
    //log.trace "--zone heating set point: ${tempStr(hsp)}"
    log.trace "--current vo: ${cvo}"
    log.trace "--zone temp: ${tempStr(zoneTemp)}"
    log.trace "--main active set point: ${tempStr(mainSetPoint)}"
    //log.trace "--main cooling set point: ${tempStr(mainCSP)}"
    //log.trace "--main heating set point: ${tempStr(mainHSP)}"
}

//event handlers
def levelHandler(evt){
    def ventData = atomicState."${evt.deviceId}"
    def oldVO = (atomicState.voRequest ?: -1).toInteger()
	def resetVent = false
	//log.trace "levelHandler current: ${ventData}, last app vo requested: ${atomicState.voRequest}" 
   	//vent level reply
    if (ventData != null){
    	def v = evt.value.toFloat().round(0).toInteger()
   		if (evt.value.isInteger() == false){
   			//log.info "[${evt.displayName}] vent level response: ${v}%"
       		ventData.voActual = v
   		} else {
   			//log.info "[${evt.displayName}] vent level request: ${v}%"
       		ventData.voRequest = v
       		if (v == oldVO){
       			log.debug "VL request OK"
       		} else {
       			log.warn "External VL request!!!"
                resetVent = true
           		//if (atomicState.running == true) zoneEvaluate()
       		}
   		}
        atomicState."${evt.deviceId}" = ventData
        
        if (resetVent) zoneEvaluate("ventReset- vOld: ${oldVO}, vNew: ${v}")
        
		//log.trace "levelHandler result: ${atomicState."${evt.deviceId}"}"    
    }
    
}

def zoneDisableHandeler(evt){
    //log.debug "zoneDisableHandeler- evt.name:${evt.name}, evt.value:${evt.value}"
    if (evt.isStateChange()) zoneEvaluate("zoneDisable")
}

def tempHandler(evt){
	//log.debug "tempHandler- name: ${evt.name}, reported: ${evt.value}"
    zoneEvaluate("tempChange")	
}

def getAdjustedPressure(evt){
    def hvacMap = parent.getHVACstate()
    if (hvacMap == null) return
    //def mainCSP	= hvacMap.mainCSP
    //def mainHSP	= hvacMap.mainHSP
    //def mainMode = hvacMap.mainMode
    def mainOn = hvacMap.mainOn
    
	
	
	if (mainOn){
    	def vid = evt.deviceId
        def vent = vents.find{it.id == vid}
        
    	//find start up settings
    	//def s = state."${vid}"
        //log.debug "vent:${evt.displayName}, start up state:${s}"
        //if (s.vOffset){
        	//def vOffset = s.vOffset.toFloat()
     		def T2 = 273.15 //standard kelvin
    		def stdP = 101325.0 //standard pressure
            def vo = vent.currentValue("level")
   			def P1 = vent.currentValue("pressure").toFloat()
        	def T = vent.currentValue("temperature").toFloat()
			def T1 = tempToK(T)
       		def pAdjusted = ((P1 * T2)/T1).round(0)
        	def pOffset = (pAdjusted - stdP).round(0)
            def k = (P1/T1).round(0)
            
        	log.info "[${vent.displayName}] adjusted~ pressure: ${pAdjusted}Pa, offset: ${pOffset}Pa, k: ${k}, actuals~ temp: ${T1}K, pressure: ${P1}Pa, vo: ${vo}%" 
            
       // }
    }
    
}

//misc utility methods
def setVents(newVo){
	atomicState.voRequest = newVo
	vents.each{ vent ->
    	//log.info "setVents data [${vent.displayName}] ${atomicState."${vent.id}"}"
        def ventData = atomicState."${vent.id}"
        def previousRequest
        def previousActual
        def changeRequired = false
        if (ventData != null){
         	previousRequest = ventData.voRequest
        	previousActual = ventData.voActual
            //log.info "change required test: pr: ${previousRequest}, newVo: ${newVo}"
            changeRequired = previousRequest.toInteger() != newVo.toInteger()
            ventData.voRequest = "${newVo}"
            //log.debug "setVents- device state map updated here: , VD: ${ventData}, new vo: ${newVo}"
        } else {
           	ventData =  [voRequest:"${newVo}",voActual:"${newVo}"]
            changeRequired = true
            previousRequest = newVo
            //log.debug "setVents- device state map built here: ${ventData}"
            
        }
        atomicState."${vent.id}" = ventData
        //log.info "setVents- [${vent.displayName}], changeRequired: ${changeRequired}, new vo: ${newVo}, previous requested vo: ${previousRequest}, previous actual vo: ${previousActual}"
        if (changeRequired){
        	vent.setLevel(newVo)
        }
    }
 	return newVo
}

def getStandardPressure(){
	log.debug "geting initial pressure readings"
	//pressure is a string
    //temp is an int 
    def T2 = 273.15 //standard kelvin
    def stdP = 101325.0 //standard pressure
    vents.each{ vent ->
    	def vo = vent.currentValue("level")
    	def P1 = vent.currentValue("pressure").toFloat()
        def T = vent.currentValue("temperature").toFloat()
		def T1 = tempToK(T)
        //((P*K)/stdK) - stdP = vOffset
        def pAdjusted = ((P1 * T2)/T1).round(0)
        def pOffset = (pAdjusted - stdP).round(0)
        def k = (P1/T1).round(0)
        //state."${vent.id}".pOffset = vOffset 
        log.info "init[${vent.displayName}] adjusted~ pressure: ${pAdjusted}, offset: ${pOffset}, k: ${k}, actuals~ temp: ${T1}, pressure: ${P1}, vo: ${vo}"
	}
}

def pollVents(){
	vents.getPressure()
    vents.getTemperature()
}

def delayClose(){
    setVents(0)
    log.info "Vent delayed close executed"
}

def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp) return "${temp.toString()}°${tc}"
    else return "No data available yet."
}

def tempToK(ct){
   	def K
   	if (state.tempScale == "F"){
		//F to K: [K] = ([°F] + 459.67) × 5⁄9
        K = ((ct + 459.67) * 5) / 9
    } else {
    	//C to K: [K] = [°C] + 273.15
        K = ct + 273.15
    }
	return K.toInteger()        
}

//dynamic page input helpers
def minVoptions(){
	return [["0":"Fully closed"],["5":"5%"],["10":"10%"],["15":"15%"],["20":"20%"],["25":"25%"],["30":"30%"],["35":"35%"],["40":"40%"]]
}

def maxVoptions(){
	def opts = []
    def start = minVo.toInteger() + 5
    start.step 95, 5, {
   		opts.push(["${it}":"${it}%"])
	}
    opts.push(["100":"Fully open"])
    return opts
}

def zoneTempOptions(){
	def zo
    if (!state.tempScale) state.tempScale = location.temperatureScale
	if (state.tempScale == "F"){
    	zo = [["-5":"-5°F"],["-4":"-4°F"],["-3":"-3°F"],["-2":"-2°F"],["-1":"-1°F"],["0":"0°F"],["1":"1°F"],["2":"2°F"],["3":"3°F"],["4":"4°F"],["5":"5°F"]]
    } else {
    	zo = [["-5":"-5°C"],["-4":"-4°C"],["-3":"-3°C"],["-2":"-2°C"],["-1":"-1°C"],["0":"0°C"],["1":"1°C"],["2":"2°C"],["3":"3°C"],["4":"4°C"],["5":"5°C"]]
    }
	return zo
}


//legacy data logging and statistics
//spent too much time on this to delete it yet.
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

//report methods, called from parent
def getEndReport(){
	return state.endReport ?: "\n\tNo data available yet."
}

def getZoneConfig(){
	//zoneControlSwitch
    def zc = "Not Activated" 
    if (zoneControlSwitch) zc = "is ${zoneControlSwitch.currentValue("switch")} via [${zoneControlSwitch.displayName}]"
	return "\n\tKeen vents: ${vents}\n\tTemp Sensors: [${tempSensors}]\n\tMinimum vent opening: ${minVo}%\n\tMaximum vent opening: ${maxVo}%\n\tHeating offset: ${tempStr(heatOffset)}\n\tCooling offset: ${tempStr(coolOffset)}\n\tZone control: ${zc}\n\tVersion: ${state.vChild ?: "No data available yet."}"
}

def getZoneState(){
    def s 
    if (state.running == true) s = true
    else s = false
    return "\n\trunning: ${s}\n\tcurrent temp: ${tempStr(tempSensors.currentValue("temperature"))}\n\tvent levels: ${vents.currentValue("level")}%"
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
