/**
 *  kvChild 0.1.1
 	
    0.1.1	fixed bug in zone temp
    0.1.0	added adjustable logging
    		added pressure polling switch option
    0.0.8a	bug fixes, poller and notify
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
    page(name: "advanced")
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
	state.vChild = "0.1.1"
    parent.updateVer(state.vChild)
    subscribe(tempSensors, "temperature", tempHandler)
    subscribe(vents, "pressure", getAdjustedPressure)
    subscribe(vents, "level", levelHandler)
    subscribe(zoneControlSwitch,"switch",zoneDisableHandeler)
	if (!state.mainState){ 
    	//def data = parent.notifyZone()
        //log.debug "init: ${data}"
    	//zoneEvaluate(data)
        zoneEvaluate(parent.notifyZone())
    } else {
    	//normal tap through
        if (zoneControlSwitch) state.zoneDisabled = zoneControlSwitch.currentValue("switch") == "off"
        else {
        	state.zoneDisabled = false
            state.zoneDisablePending = false
        }
        if ((state.maxVo !=  maxVo.toInteger()) || (state.minVo !=  minVo.toInteger()) || (state.coolOffset != coolOffset.toInteger()) || (state.heatOffset != heatOffset.toInteger())){
        	if (state.mainOn){
            	def data = [msg:"self", data:[settingsChanged:true]]
                zoneEvaluate(data)
            }
        } else {
			//nothing changes        
        }
    } 
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
                   	,defaultValue	: ["-1"]
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
				
                //advanced hrefs...
				href( "advanced"
					,title			: "Advanced features..."
					,description	: ""
					,state			: null
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

def advanced(){
    return dynamicPage(
    	name		: "advanced"
        ,title		: "Advanced Options"
        ,install	: false
        ,uninstall	: false
        ){
         section(){
         		def vpTitle = vPolling ?  "Vent polling is [on]" : "Vent polling is [off]" 
          		input(
            		name			: "vPolling"
                	,title			: vpTitle 
                	,multiple		: false
                	,required		: false
                	,type			: "bool"
                    ,submitOnChange	: true
                    ,defaultValue	: false
            	)                  
         		input(
            		name			: "logLevel"
                	,title			: "IDE logging level" 
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
               		,options		: [["0":"None"],["10":"Lite"],["20":"Moderate"],["30":"Detailed"],["40":"Super nerdy"],["15":"Pressure only"]]
                	,submitOnChange	: false
                   	,defaultValue	: ["10"]
            	)                      
        }
    }
}

//zone control methods
def zoneEvaluate(params){
	logger(30,"debug","zoneEvaluate:enter-- parameters: ${params}")
	//param data structures
    /*
    [msg:stat, data:[mainState:heat, mainStateChange:false, mainMode:heat, mainModeChange:false, mainCSP:80.0, mainCSPChange:false, mainHSP:74.0, mainHSPChange:true, mainOn:true]]
    [stat:[mainMode:heat|cool|auto,mainState:heat|cool|idle,mainCSP:,mainHSP:,mainOn:true|false]]
    [msg:temp, data:[value:]]
    [msg:vent, data:[value:]]
    [msg:"zoneSwitch", data:[zoneIsEnabled:true|false]]
    [msg:pressure, data:[value:,isOK:true|false]]
    [msg:self, data:[evt:"updated"]]
    */
	
    // variables
    def evaluateVents = false
    def msg = params.msg
    def data = params.data
    
    def mainState = state.mainState
    def mainMode = state.mainMode 
	def mainHSP = state.mainHSP 
	def mainCSP = state.mainCSP 
	def mainOn = state.mainOn 
    def zoneCSP = state.zoneCSP
    def zoneHSP = state.zoneHSP
    
    def zoneDisablePending = state.zoneDisablePending ?: false
	def zoneDisabled = state.zoneDisabled ?: false
    def running
    
    //always fetch these
    def zoneTemp = tempSensors.currentValue("temperature").toFloat()
    def coolOffset = settings.coolOffset.toInteger()
    def heatOffset = settings.heatOffset.toInteger()
    def maxVo = settings.maxVo.toInteger()
    def minVo = settings.minVo.toInteger()
    
    switch (msg){
    	case "stat" :
        		//[initRequest:true, mainState:heat, mainStateChange:false, mainMode:heat, mainModeChange:false, mainCSP:80.0, mainCSPChange:false, mainHSP:74.0, mainHSPChange:true, mainOn:true]
        		//logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
                //initial request for info during app install
                if (data.initRequest){
                    evaluateVents = data.mainOn
                    //log.info "zoneEvaluate- init zone request, evaluateVents: ${evaluateVents}"
                //set point changes, ignore setbacks
                } else if (data.mainOn && (mainHSP < data.mainHSP || mainCSP > data.mainCSP)) {
                    evaluateVents = true
                    logger(30,"info","zoneEvaluate- set point changes, evaluate: ${true}")
                //system state changed
                } else if (data.mainStateChange){
                	//system start up
                	if (data.mainOn && !zoneDisabled){
                        evaluateVents = true
                        if (vPolling) pollVents() 
                        logger(30,"info","zoneEvaluate- system start up, evaluate: ${evaluateVents}, vent polling started: ${vPolling}")
                        logger(10,"info","Main HVAC is on and ${data.mainState}ing")
                    //system shut down
                    } else if (!data.mainOn){
                    	zoneDisablePending = false
                    	running = false
						//check zone vent close options
                        def closeOption = ventCloseWait.toInteger()
        				if (closeOption == 0){
                			logger(10,"warn", "Vents closed via Close vents option")
        					setVents(0)
        				} else if (closeOption > 0){
                			logger(10,"warn", "Vent closing is scheduled in ${closeOption} seconds")
        					runIn(closeOption,delayClose)
        				}
                        def asp = state.activeSetPoint
                        def d = (zoneTemp - asp).toFloat()
                        d = d.round(1)
                        state.endReport = "\n\tsetpoint: ${tempStr(asp)}\n\tend temp: ${tempStr(zoneTemp)}\n\tvariance: ${tempStr(d)}\n\tvent levels: ${vents.currentValue("level")}%"        
                    	logger(10,"info","Main HVAC has shut down.")
     				}
                } else {
                	logger(30,"warn","zoneEvaluate- ${msg}, no matching events, data: ${data}")
                }
                //always update data
                mainState = data.mainState
                mainMode = data.mainMode
                mainHSP = data.mainHSP
                mainCSP = data.mainCSP
                mainOn = data.mainOn
                zoneCSP = mainCSP + coolOffset
                zoneHSP = mainHSP + heatOffset
        	break
        case "temp" :
        		//data:["tempChange"]
        		//logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
                //process changes if zone is not disabled
                if (!zoneDisabled || zoneDisablePending){
                	logger(30,"debug","zoneEvaluate- zone temperature changed, zoneTemp: ${zoneTemp}")
                	evaluateVents = true
                } else {
                	logger(30,"warn","zoneEvaluate- ${msg}, no matching events")
                }
        	break
        case "vent" :
        		logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
                
        	break
        case "zoneSwitch" :
        		//[msg:"zoneSwitch", data:[zoneIsEnabled:true|false]]
                //fire up zone since it was activated
                if (mainOn && data.zoneIsEnabled){
                	logger(30,"debug","zoneEvaluate- zone was enabled, data: ${data}")
                	evaluateVents = true
                //zone is active, zone switch went inactive
                } else if (mainOn && !data.zoneIsEnabled) {
                	zoneDisabled = true
                    zoneDisablePending = true
                } else {
                	logger(30,"warn","zoneEvaluate- ${msg}, no matching events, data: ${data}")
                }
        	break
        case "pressure" :
        		logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
                
        	break
        case "self" :
        		//[msg:self, data:[settingsChanged:true|false]
        		//logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
                if (data.settingsChanged){
                	//[msg:"self", data:[maxVo:maxVo.toInteger(),minVo:minVo.toInteger(),coolOffset:coolOffset.toInteger(),heatOffset:heatOffset.toInteger()]]
                	logger(30,"debug","zoneEvaluate- zone settingsChanged, data: ${data}")
                    zoneCSP = mainCSP + coolOffset
                	zoneHSP = mainHSP + heatOffset
                	evaluateVents = true
                }
        	break
            
    }    
    
    if (evaluateVents){
    	def slResult = ""
       	if (mainState == "heat"){
        	state.activeSetPoint = zoneHSP
       		if (zoneTemp >= zoneHSP){
           		slResult = setVents(minVo)
             	logger(10,"info", "Zone temp is ${tempStr(zoneTemp)}, heating setpoint of ${tempStr(zoneHSP)} is met${slResult}")
				running = false
          	} else {
				slResult = setVents(maxVo)
				logger(10,"info", "Zone temp is ${tempStr(zoneTemp)}, heating setpoint of ${tempStr(zoneHSP)} is not met${slResult}")
				running = true
           	}            	
        } else if (mainState == "cool"){
        	state.activeSetPoint = zoneCSP
       		if (zoneTemp <= zoneCSP){
				slResult = setVents(minVo)
                logger(10,"info", "Zone temp is ${tempStr(zoneTemp)}, cooling setpoint of ${tempStr(zoneCSP)} is met${slResult}")
                running = false
       		} else {
				slResult = setVents(maxVo)
                logger(10,"info", "Zone temp is ${tempStr(zoneTemp)}, cooling setpoint of ${tempStr(zoneCSP)} is not met${slResult}")
                running = true
           	}                        
      	} else {
            logger(10,"error","zoneEvaluate- evaluateVents, mainState: ${mainState}, zoneTemp: ${zoneTemp}, zoneHSP: ${zoneHSP}, zoneCSP: ${zoneCSP}")
       	}
    }
    //write state
    state.mainState = mainState
    state.mainMode = mainMode
	state.mainHSP = mainHSP
	state.mainCSP = mainCSP
	state.mainOn = mainOn
    state.zoneCSP = zoneCSP
    state.zoneHSP = zoneHSP
    state.zoneTemp = zoneTemp
    state.zoneDisablePending = zoneDisablePending
	state.zoneDisabled = zoneDisabled
    state.running = running
   	state.maxVo = maxVo
    state.minVo = minVo
    state.coolOffset = coolOffset
    state.heatOffset = heatOffset

    logger(40,"debug","zoneEvaluate:exit- ")
    

}

//event handlers
def levelHandler(evt){
	logger(40,"debug","levelHandler:enter- ")
	logger(30,"debug","levelHandler- evt name: ${evt.name}, value: ${evt.value}, rdLen: ${evt.description == ""}")
    //evt.description == "" is true for app initiated, and false for device results...
    def ventData = state."${evt.deviceId}"
    def oldVO = (state.voRequest ?: -1).toInteger()
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
       			//log.debug "VL request OK"
                logger(30,"debug","levelHandler- VL request OK")
       		} else {
       			//log.warn "External VL request!!!"
                logger(30,"warn","levelHandler- External VL request!!!")
                resetVent = true
           		//if (atomicState.running == true) zoneEvaluate()
       		}
   		}
        state."${evt.deviceId}" = ventData
        
        //if (resetVent) zoneEvaluate("ventReset- vOld: ${oldVO}, vNew: ${v}")
        //[msg:self, data:[evt:"updated"]]
        
		//log.trace "levelHandler result: ${atomicState."${evt.deviceId}"}"    
    }
    logger(40,"debug","levelHandler:exit- ")
}

def zoneDisableHandeler(evt){
    logger(30,"debug","zoneDisableHandeler- evt name: ${evt.name}, value: ${evt.value}")
    if (evt.isStateChange()) zoneEvaluate([msg:"zoneSwitch", data:[zoneIsEnabled:(evt.value == "on" )]])
}

def tempHandler(evt){
    logger(40,"debug","tempHandler- evt name: ${evt.name}, value: ${evt.value}")
    state.zoneTemp = evt.value.toFloat()
    if (state.mainOn){
    	logger(30,"debug","tempHandler- tempChange, value: ${evt.value}")
    	zoneEvaluate([msg:"temp", data:["tempChange"]])	
    }
    
}

def getAdjustedPressure(evt){
	logger(40,"debug","getAdjustedPressure:enter- ")
	if (state.mainOn || (vPolling == true)){
    	logger(30,"info","getAdjustedPressure- evt name: ${evt.name}, value: ${evt.value}")
    	def vid = evt.deviceId
        def vent = vents.find{it.id == vid}
 
     	def stdT = 273.15 //standard temperature, kelvin
    	def stdP = 101325.0 //standard pressure, pascal
        def stdD = 1.2041 //standard air density, kg/m3
        def vo = vent.currentValue("level").toFloat().round(0).toInteger()
   		def P1 = vent.currentValue("pressure").toFloat()
        def T = vent.currentValue("temperature").toFloat()
		def T1 = tempToK(T)
       	def pAdjusted = ((P1 * stdT)/T1) //pascal
        def pVelocity = Math.sqrt((2 * P1)/stdD).round(0).toInteger()
        def pVelocityAdjusted = Math.sqrt((2 * pAdjusted)/stdD).round(0).toInteger()
        //Math.sqrt(delegate)
        logger(15,"info","getAdjustedPressure- [${vent.displayName}] adjusted~ pressure: ${pAdjusted.round(0).toInteger()} Pa, velocity: ${pVelocityAdjusted} m/s, actuals~ temp: ${T1} K, pressure: ${P1.round(0).toInteger()} Pa, velocity: ${pVelocity} m/s, vo: ${vo}%, mainOn: ${state.mainOn}")
            
        	//def pOffset = (pAdjusted - stdP).round(0)
            //def k = (P1/T1).round(0)
            //logger(-1,"info","getAdjustedPressure- [${vent.displayName}] adjusted~ pressure: ${pAdjusted}Pa, offset: ${pOffset}Pa, k: ${k}, actuals~ temp: ${T1}K, pressure: ${P1}Pa, vo: ${vo}%, mainOn: ${mainOn}")
            
    }
    logger(40,"debug","getAdjustedPressure:exit- ")
}

//misc utility methods
def logger(displayLevel,errorLevel,text){
	//input logLevel 1,2,3,4,-1
    /*
    [1:"Lite"],[2:"Moderate"],[3:"Detailed"],[4:"Super nerdy"]
    input 	logLevel
    
    1		Lite		
    2		Moderate	
    3		Detailed
    4		Super nerdy
    
    errorLevel 	color		number
    error		red			5
    warn		yellow		4
    info		lt blue		3
    debug		dk blue		2
    trace		gray		1
    */
    def logL = 10
    if (logLevel) logL = logLevel.toInteger()
    
    if (logL == 0) {return}//bail
    else if (logL == 15 && displayLevel == 15) log."${errorLevel}"(text)
    else if (logL >= displayLevel) log."${errorLevel}"(text)
 }

def setVents(newVo){
	logger(40,"debug","setVents:enter- ")
	logger(30,"warn","setVents- newVo: ${newVo}")
	//state.voRequest = newVo
    def result = ""
    def changeRequired = false
	vents.each{ vent ->
    	//log.info "setVents data [${vent.displayName}] ${atomicState."${vent.id}"}"
        def ventData = state."${vent.id}"
        def previousRequest
        def previousActual
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
        state."${vent.id}" = ventData
        //log.info "setVents- [${vent.displayName}], changeRequired: ${changeRequired}, new vo: ${newVo}, previous requested vo: ${previousRequest}, previous actual vo: ${previousActual}"
        logger(30,"info","setVents- [${vent.displayName}], changeRequired: ${changeRequired}, new vo: ${newVo}, previous requested vo: ${previousRequest}, previous actual vo: ${previousActual}")
        if (changeRequired){
        	vent.setLevel(newVo)
        }
    }
    if (changeRequired) result = ", setting vents to ${newVo}%"
    else result = ", vents already at ${newVo}%"
 	return result
    logger(40,"debug","setVents:exit- ")
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
	if (vPolling){
		logger(15,"warn","pollVents- polling vents")
   		vents.getTemperature()
		vents.getPressure()
   		if (state.mainOn == null) return
   		if (state.mainOn) runIn(60,pollVents)
    }
}

def pollVentsII(){
	log.warn "polling ventsII"
    vents.getTemperature()
	vents.getPressure()
    if (vPolling == true) runIn(60,pollVentsII)
}

def delayClose(){
    setVents(0)
    logger(10,"warn","Vent close executed")
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
	return "\n\tVents: ${vents}\n\tTemp Sensors: [${tempSensors}]\n\tMinimum vent opening: ${minVo}%\n\tMaximum vent opening: ${maxVo}%\n\tHeating offset: ${tempStr(heatOffset)}\n\tCooling offset: ${tempStr(coolOffset)}\n\tZone control: ${zc}\n\tVersion: ${state.vChild ?: "No data available yet."}"
}

def getZoneState(){
    def s 
    if (state.running == true) s = true
    else s = false
    return "\n\trunning: ${s}\n\tcurrent temp: ${tempStr(state.zoneTemp)}\n\tsetpoint: ${tempStr(state.activeSetPoint)}\n\tvent levels: ${vents.currentValue("level")}%\n\tbattery: ${vents.currentValue("battery")}%"
}

def getVentReport(){
	def report = []
    vents.each{ vent ->
    	def P = vent.currentState("pressure")
        def L = vent.currentState("level")
        def T = vent.currentState("temperature")
        def B = vent.currentState("battery")
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
