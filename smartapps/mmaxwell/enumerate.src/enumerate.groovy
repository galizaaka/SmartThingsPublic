definition(
    name: "enumerate",
    singleInstance: true,
    namespace: "mmaxwell",
    author: "Mike Maxwell",
    description: "Device command and exploration tool",
    category: "My Apps",
  	iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/Cat-ModeMagic@3x.png"
)

preferences {
    page(name: "customCommandsPAGE")
    page(name: "generalApprovalPAGE")
    page(name: "addCustomCommandPAGE")		
	page(name: "deleteCustomCommandPAGE")	
	page(name: "customParamsPAGE")			
			
}

def generalApprovalPAGE(params){
	def title = params.title
	def method = params.method
	def result
	dynamicPage(name: "generalApprovalPAGE", title: title ){
		section() {
			if (method) {
				result = app."${method}"()
				paragraph "${result}"
			}
		}
	}
}

def customCommandsPAGE() {
	if (!state.lastCmdIDX) state.lastCmdIDX = 0
	def savedCommands = getCommands()
	dynamicPage(name: "customCommandsPAGE", title: "Custom Commands", uninstall: true, install: true) {
		section(){
			input(
				name			: "baseDevice"
				,title			: "Base Device Type"
				,multiple		: false
				,required		: false
				,type			: "enum"
                ,options		: [["capability.actuator":"Actuator"],["capability.switch":"Switch"],["capability.musicPlayer":"Music Player"],["capability.notification":"Notification"],["capability.timedSession":"Timed Session"],["capability.consumable":"Consumable"],["capability.imageCapture":"Image Capture"]]
				,submitOnChange	: true
			)
            if (baseDevice){
				input(
					name			: "devices"
					,title			: "Test device"
					,multiple		: false
					,required		: false
					,type			: baseDevice
					,submitOnChange	: true
				)
            }
			if (devices && baseDevice && savedCommands.size() != 0){
				input(
					name			: "testCmd"
					,title			: "Select saved command to test"
					,multiple		: false
					,required		: false
					,type			: "enum"					
                	,options		: savedCommands
					,submitOnChange	: true
				)
            }
        }
        def result = execCommand(settings.testCmd)
        if (result) {
        	section("${result}"){
    		}
        }
    	section(){
        	if (devices && baseDevice){
				href( "addCustomCommandPAGE"
					,title		: "New custom command..."
					,description: ""
					,state		: null
				)
        	}
			if (getCommands()){
				href( "deleteCustomCommandPAGE"
					,title		: "Delete custom commands..."
					,description: ""
					,state		: null
				)
			}
		}
	}
}
def addCustomCommandPAGE(){
	def cmdLabel = getCmdLabel()
	def complete = "" 
	def test = false
    def pageTitle = "Create new custom command for:\n${devices}" 
	if (cmdLabel){
		complete = "complete"
		test = true
	}
	dynamicPage(name: "addCustomCommandPAGE", title: pageTitle, uninstall: false, install: false) {
		section(){
			input(
		   		name			: "cCmd"
				,title			: "Available device commands"
				,multiple		: false
				,required		: true
				//,type			: "text"
                ,type			: "enum"
                ,options		: getDeviceCommands()
				,submitOnChange	: true
		 	)
			href( "customParamsPAGE"
				,title: "Parameters"
				,description: parameterLabel()
				,state: null
			)
        }
 		if (test){
        	def result = execTestCommand()
           	section("Configured command: ${cmdLabel}\n${result}"){
			//}
            //section(result){
            	if (result == "suceeded"){
                   	if (!commandExists(cmdLabel)){
						href( "generalApprovalPAGE"
							,title		: "Save command now"
							,description: ""
							,state		: null
							,params		: [method:"addCommand",title:"Add Command"]
						)
                   	}
				} 
			}
		}
	}
}
def deleteCustomCommandPAGE(){
	dynamicPage(name: "deleteCustomCommandPAGE", title: "Delete custom commands", uninstall: false, install: false) {
		section(){
			input(
				name			: "deleteCmds"
				,title			: "Select commands to delete"
				,multiple		: true
				,required		: false
				,type			: "enum"
				,options		: getCommands()
				,submitOnChange	: true
			)
			if (isValidCommand(deleteCmds)){
				href( "generalApprovalPAGE"
					,title			: "Delete command(s) now"
					,description	: ""
					,state			: null
					,params			: [method:"deleteCommands",title:"Delete Command"]
					,submitOnChange	: true
				)
			}
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

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	childApps.each {child ->
            log.info "Installed Rules: ${child.label}"
    }
}
/***** child specific methods *****/

/*
//stub for non expert Rule Machine page
// must remain commented out for expert version
def getCommands(){
	return []
}
*/
def getCommandMap(cmdID){
	return state.customCommands["${cmdID}"]
}

/***** local custom command specific methods *****/
def anyCustom(){
	def result = null
    if (getCommands()) result = "complete"
    return result
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
def getCommands(){
	def result = [] 
	def cmdMaps = state.customCommands ?: []
	cmdMaps.each{ cmd ->
		def option = [(cmd.key):(cmd.value.text)]
        result.push(option)
	}
	return result
}

def isValidCommand(cmdIDS){
	def result = false
	cmdIDS.each{ cmdID ->
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
def commandExists(cmd){
	def result = false
	if (state.customCommands){
    	result = state.customCommands.find{ it.value.text == "${cmd}" }
    }
    return result
}
def addCommand(){
	def result
    def newCmd = getCmdLabel()
    def found = commandExists(newCmd)
	def cmdMaps = state.customCommands
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
        if (cmdMaps) cmdMaps.put((nextIDX),cmd)
        else state.customCommands = [(nextIDX):cmd]
		result = "command: ${newCmd} was added"
	} else {
		result = "command: ${newCmd} was not added, it already exists."
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
            result = "suceeded"
		}
		catch (IllegalArgumentException e){
           	def em = e as String
            def ems = em.split(":")
            ems = ems[2].replace(" [","").replace("]","")
            ems = ems.replaceAll(", ","\n")
            result = "failed, valid commands:\n${ems}"
		}
	}
	return result
}

def execCommand(cmdID){
    def result = ""
	def pList = []
    if (cmdID){
		def cmdMap = state.customCommands["${cmdID}"] 
		if (testCmd && cmdMap) {
			cmdMap.params.each{ p ->
				if (p.value.type == "string"){
					pList << "${p.value.value}"
       			} else if (p.value.type == "decimal"){
		   			pList << p.value.value.toBigDecimal()
				} else {
					pList << p.value.value.toInteger()
				}
			}
			def p = pList as Object[]
			devices.each { device ->
				try {
					device."${cmdMap.cmd}"(p)
					result = "Command succeeded"
				}
				catch (IllegalArgumentException e){
           			def em = e as String
            		def ems = em.split(":")
            		ems = ems[2].replace(" [","").replace("]","")
            		ems = ems.replaceAll(", ","\n")
            		result = "Command failed, valid commands:\n${ems}"
				}
			}
			return result
		}
    }
}
def getDeviceCommands(){
    def result = ""
	devices.each { device ->
		try {
			device."xxx"()
			result = "Command succeeded"
		}
		catch (IllegalArgumentException e){
            def em = e as String
            def ems = em.split(":")
            ems = ems[2].replace(" [","").replace("]","")
            result = ems.split(", ").collect{it as String}
		}
	}
	return result
}