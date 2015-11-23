/**
 *  enumerate Beta 1  device command exploration tool 
 *
 *  Copyright 2015 Mike Maxwell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
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
	description: "Device command explorer",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "customCommandsPAGE")
	page(name: "addCustomCommandPAGE")
	page(name: "deleteCustomCommandPAGE")
	page(name: "customCommandPAGE")
	page(name: "customParamsPAGE")
	page(name: "generalApprovalPAGE")
}
/***** page methods	*****/
def customCommandsPAGE() {
	if (!state.lastCmdIDX) state.lastCmdIDX = 0
	if (!state.customCommands) state.customCommands = [0:[text:"showMethods()",cmd:"showMethods",params:[:]]]

	dynamicPage(name: "customCommandsPAGE", title: "Custom Commands", uninstall: true, install: true) {
	section() {
			input(
				name			: "devices"
				,title			: "Test Devices"
				,multiple		: true
				,required		: false
				,type			: "capability.actuator"
				,submitOnChange	: true
			)
			if (settings.devices){
				input(
					name			: "testCmd"
					,title			: "Select command to test"
					,multiple		: false
					,required		: false
					,type			: "enum"
					,options		: getCommands(true)
					,submitOnChange	: true
				)
				if (isValidCommand([settings.testCmd])){
					def result = execCommand(settings.testCmd) 
					paragraph "${result}"
				}
			} //end devices
			href( "addCustomCommandPAGE"
				,title		: ""
				,description: "Add custom command..."
				,state		: null
			)
			if (getCommands(false)){
				href( "deleteCustomCommandPAGE"
					,title		: ""
					,description: "Delete custom commands..."
					,state		: null
				)
			}
		}
	}
}
def deleteCustomCommandPAGE(){
	dynamicPage(name: "deleteCustomCommandPAGE", title: "Delete Custom Commands", uninstall: false, install: false) {
		section(){
			input(
				name			: "deleteCmds"
				,title			: "Select commands to delete"
				,multiple		: true
				,required		: false
				,type			: "enum"
				,options		: getCommands(false)
				,submitOnChange	: true
			)
			log.debug "deleteCmds:${deleteCmds}"
			if (isValidCommand(deleteCmds)){
				href( "generalApprovalPAGE"
					,title			: ""
					,description	: "Delete Command(s) Now"
					,state			: null
					,params			: [method:"deleteCommands",title:"Delete Command",nextPage:"customCommandsPAGE"]
					,submitOnChange	: true
				)
			}
		}
	}
}

def addCustomCommandPAGE(){
	def cmdLabel = getCmdLabel()
	def complete = "" 
	def test = false
	def title = "Create custom command"
	if (cmdLabel){
		complete = "complete"
		test = true
		title = "Custom command"
	}
	dynamicPage(name: "addCustomCommandPAGE", title: "Add Custom Commands", uninstall: false, install: false) {
		section(){
   			href( "customCommandPAGE"
				,title		: title
				,description: cmdLabel ?: "Tap to set"
				,state		: complete
			)
			if (test){
			//test devices
		   		input(
					name			: "devices"
					,title			: "Test Devices"
					,multiple		: true
					,type			: "capability.actuator"
					,required		: false
					,submitOnChange	: true
				)
				if (devices){
					def result
					result = execTestCommand() 
					if (!result){
						paragraph "Command suceeded"
						href( "generalApprovalPAGE"
							,title		: ""
							,description: "Save Command Now"
							,state		: null
							,params		: [method:"addCommand",title:"Add Command",nextPage:"customCommandsPAGE"]
						)
					} else {
						paragraph "${result}"
					}
				}
			}
		}
	}
}
def generalApprovalPAGE(params){
	def title = params.title
	def method = params.method
	def nextPage = params.nextPage
	def result
	dynamicPage(name: "generalApprovalPAGE", title: title, nextPage: nextPage){
		section() {
			if (method) {
				result = app."${method}"()
				paragraph "${result}"
			}
		}
	}
}
def customCommandPAGE(){
	dynamicPage(name: "customCommandPAGE", title: "Create custom command"){
		section() {
			input(
		   		name			: "cCmd"
				,title			: "Command"
				,multiple		: false
				,required		: true
				,type			: "text"
				,submitOnChange	: true
		 	)
			href( "customParamsPAGE"
				,title: "Parameters"
				,description: parameterLabel()
				,state: null
			)
		}
	}
}
def customParamsPAGE(p){
	def ct = settings.findAll{it.key.startsWith("cpType_")}
	state.howManyP = ct.size() + 1
	def howMany = state.howManyP
	dynamicPage(name: "customParamsPAGE", title: "Select parameters", uninstall: false) {
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

def getParamType(myParam,isLast){  
	def myOptions = ["string", "number", "decimal"]
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

def getPvalue(myPtype, n){
	def myVal = "cpVal_" + n
	def result = null
	if (myPtype == "string"){
		result = input(
					name		: myVal
					,title		: "string value"
					,type		: "text"
					,required	: false
				)
	} else if (myPtype == "number"){
		result = input(
					name		: myVal
					,title		: "integer value"
					,type		: "number"
					,required	: false
				)
	} else if (myPtype == "decimal"){
		result = input(
					name		: myVal
					,title		: "decimal value"
					,type		: "decimal"
					,required	: false
				)
	}
	return result
}
def getCmdLabel(){
	def cmd
	if (settings.cCmd) cmd = settings.cCmd.value
	def cpTypes = settings.findAll{it.key.startsWith("cpType_")}.sort()
	def result = null
	if (cmd) {
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
		def i = cpType.key.replaceAll("cpType_","")
		def cpVal = settings.find{it.key == "cpVal_${i}"}
		if (cpType.value == "string"){
		   	result = result + "'${cpVal.value}'," 
		} else {
			if (cpVal.value.isNumber()){
				result = result + "${cpVal.value}," 
			} else {
				result = result + "[${cpVal.value}]: is not a number,"
			}
		}
	}
	result = result[0..-2]   
	return result
}
def parameterLabel(){
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

def parameterLabelN(i){
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
		} else if (cpType.value == "decimal"){
		   	result << cpVal.value.toBigDecimal()
		} else {
			result << cpVal.value.toInteger() 
		}
	}
	return result
}


def installed(){
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated(){
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}
def getCommands(showAll){
	def result = [] 
	def cmdMaps = state.customCommands
	cmdMaps.each{ cmd ->
		def option = [(cmd.key):(cmd.value.text)]
		if (showAll){
			result.push(option)
		} else if (cmd.key != "0") {
			result.push(option)	
		}
	}
	return result
}
def isValidCommand(cmdIDS){
	def result = false
	cmdIDS.each{ cmdID ->
		log.debug "checking:${cmdID}"
		def cmd = state.customCommands["${cmdID}"]
		if (cmd) result = true
	}
	return result
}

def deleteCommands(){
	def result
	def cmdMaps = state.customCommands
	if (deleteCmds.size == 1) result = "Command removed"
	else result = "Commands removed"
	deleteCmds.each{ it -> 
		cmdMaps.remove(it)
	}
	return result
}

def addCommand(){
	def result
	def cmdMaps = state.customCommands
	def newCmd = getCmdLabel()
	def found = cmdMaps.find{ it.value.text == "${newCmd}" }
	//only update if not found...
	if (!found) {
		state.lastCmdIDX = state.lastCmdIDX + 1
		def nextIDX = state.lastCmdIDX
		def cmd = [text:"${newCmd}",cmd:"${cCmd}"]
		def params = [:]
		def cpTypes = settings.findAll{it.key.startsWith("cpType_")}.sort()
		cpTypes.each{ cpType ->
			def i = cpType.key.replaceAll("cpType_","")
			def cpVal = settings.find{it.key == "cpVal_${i}"}
			def param = ["type":"${cpType.value}","value":"${cpVal.value}"]
			params.put(i, param)
		}	
		cmd.put("params",params)
		cmdMaps.put((nextIDX),cmd)
		result = "command:${newCmd} was added"
	} else {
		result = "command:${newCmd} was not added, it already exists."
	}
	return result
}

def execTestCommand(){
	def result
	def cTypes = settings.findAll{it.key.startsWith("cpType_")}
	def p = getParamsAsList(cTypes) as Object[]
	devices.each { device ->
		try {
			device."${cCmd}"(p)
			//log.info "${device.displayName}: command succeeded"
		}
		catch (IllegalArgumentException e){
			//log.info "${device.displayName}: command failed${e}"
			result = "${device.displayName}: command failed\n${e}\n\n"
		}
	}
	return result
}

def execCommand(cmdID){
    def result = ""
	def pList = []
	def cmdMap = state.customCommands["${cmdID}"] 
	if (testCmd && cmdMap) {
		cmdMap.params.each{ p ->
			if (p.value.type == "string"){
				pList << "${p.value.value}"
			} else {
				pList << p.value.value.toInteger()
			}
		}
		def p = pList as Object[]
		devices.each { device ->
			try {
				device."${cmdMap.cmd}"(p)
				result = result + "${device.displayName}: command succeeded\n\n"
			}
			catch (IllegalArgumentException e){
				result = result + "${device.displayName}: command failed\n${e}\n\n"
			}
		}
		return result
	}
}
def initialize() {

}
