/**
 *  enumerate
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
    name: "enumerate",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "Yea...",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "main")
	page(name: "customCommand")
    page(name: "customParams")
}
def main() {
	def cmdLabel = getCmdLabel()
    def complete = "" 
    def title = "Create custom command"
    if (cmdLabel){
    	complete = "complete"
        title = "Custom command"
    }
	dynamicPage(name: "main", title: "main", uninstall: true, install: true) {
	section() {
        	input(
            	name			: "devices"
                ,title			: "devices"
                ,multiple		: true
                ,type			: "capability.actuator"
            )
            href( "customCommand"
            	,title		: title
            	,description: cmdLabel ?: "Tap to set..."
            	,state		: complete
            )
        }
	}
}
def customCommand(){
	dynamicPage(name: "customCommand", title: "Create custom command"){
    	section() {
    		input(
           		name			: "cCmdOn"
            	,title			: "Command"
            	,multiple		: false
            	,required		: true
            	,type			: "text"
                ,submitOnChange	: true
         	)
            href( "customParams"
            	,title: "Parameters"
            	,description: parameterLabel()
            	//,params		: [ct:pCount.toString()]
            	,state: null
            )
        }
    }
}
def customParams(p) {
	log.debug "p:${p}"
    def ct = settings.findAll{it.key.startsWith("cpType_")}
    state.howManyP = ct.size() + 1
    def howMany = state.howManyP
    log.debug "customParams - ct:${ct} howMany:${howMany}"
    
	dynamicPage(name: "customParams", title: "Select parameters", uninstall: false) {
		if(howMany) {
			for (int i = 1; i <= howMany; i++) {
				def thisParam = "cpType_" + i
                def myParam = ct.find {it.key == thisParam}
				section("Parameter #${i}") {
					getParamType(thisParam, i != howMany)
					if(myParam) {
						def pType = myParam.value
                        getPvalue(pType, i)
					}
				}
			}
		}
	}
}

def getParamType(myParam,isLast) {  
	log.info "getParamType - myParam:${myParam} isLast:${isLast}"
	def myOptions = ["string", "number"]
    /* possible iOS deselect workaround...
    if (isLast) myOptions = ["string", "number",[null:"Delete"]]
    else myOptions = ["string", "number"]
    */
	def result = input (
					name			: myParam
					,type			: "enum"
					,title			: "parameter type"
					,required		: isLast
					,options		: myOptions
					,submitOnChange	: true
				)
	return result
}

def getPvalue(myPtype, n) {
	//log.info "getPvalue - myPtype:${myPtype} n:${n}"
    def myVal = "cpVal_" + n
	def result = null
	if (myPtype == "string"){
    	result = input(
					name		: myVal
					,title		: "parameter value"
					,type		: "text"
					,required	: false
				)
	} else if (myPtype == "number"){
    	result = input(
					name		: myVal
					,title		: "parameter value"
					,type		: "number"
					,required	: false
				)
    }
	return result
}
def getCmdLabel(){
	def cmd
	if (settings.cCmdOn) cmd = settings.cCmdOn.value
    def cpTypes = settings.findAll{it.key.startsWith("cpType_")}.sort()
	def result = null
    if (cmd) {
    	//log.debug "command:${cmd}" 
    	result = "${cmd}("
        if (cpTypes.size() == 0){
        	result = result + ")"
        } else {
			result = "${result}${getParams(cpTypes)})"
		}
    }
    return result
}
def getParams(cpTypes){
	def result = ""
    cpTypes.each{ cpType ->
    	//log.info "param:${param.key}"
        def i = cpType.key.replaceAll("cpType_","")
        def cpVal = settings.find{it.key == "cpVal_${i}"}
        if (cpType.value == "string"){
           	result = result + "'${cpVal.value}'," 
        } else {
           	result = result + "${cpVal.value}," 
        }
	}
	result = result[0..-2]   
    return result
}
def parameterLabel() {
	def howMany = (state.howManyP ?: 1) - 1
	def result = ""
	if (howMany) {
		for (int i = 1; i <= howMany; i++) {
			result = result + parameterLabelN(i) + "\n"
		}
        result = result[0..-2]
    }
	return result
}

def parameterLabelN(i) {
	def result = ""
	def cpType = settings.find{it.key == "cpType_${i}"}
	def cpVal = settings.find{it.key == "cpVal_${i}"}
    if (cpType && cpVal){
		result = "p${i} - type:${cpType.value}, value:${cpVal.value}"
	} 
	return result
}
def getParamsAsList(cpTypes){
	def result = []
    cpTypes.each{ cpType ->
        def i = cpType.key.replaceAll("cpType_","")
        def cpVal = settings.find{it.key == "cpVal_${i}"}
        if (cpType.value == "string"){
           	result << "${cpVal.value}" 
        } else {
           	result << cpVal.value.toInteger() 
        }
	}
	//result = result[0..-2]   
    return result
}


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	state.pCount = 0
	unsubscribe()
	initialize()
}
def initialize() {
	//fire the custom command at them...
    //whatevs.["${commands}()"]
    
    //incomming command
    //fanOn(xxs)
    //def cmd = "fanOn(80)"
     /*
    def s1 = commands.split(/\(|\)/)
    def method = s1[0]
    log.info "method:${method}"
    if (s1[1]) {
    	def params = s1[1].split(",")
        log.info "params:${params}"
    }
    //log.info "${cmd.split(/\(|\)/)}"
	*/
    def cTypes = settings.findAll{it.key.startsWith("cpType_")}
    log.debug "cTypes:${cTypes}"
    log.debug "getParams:${getParamsAsList(cTypes)}"
    def p = getParamsAsList(cTypes) as Object[]
    log.debug "p:${p}"
    
    devices.each { device ->
    	log.info "${device.displayName}: ${cCmdOn}(${p})"
        try {
        	device."${cCmdOn}"(p)
            log.info "-worked"
        }
        catch (e){
        	log.info "-failed"
        }
    }
   
    
}
