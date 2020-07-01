/**
 *  EasyRollSingle
 *
 *  Copyright 2020 Inoshade
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

 /*EasyRollSingle DTH는 단일 블라인드를 DTH로 제어하기 위한 템플릿코드 입니다.*/
 /*
  * 20200619 - Window Shade Capability 추가
  * 20200701 - Shade Level 일정 주기마다 polling 될 수 있도록 기능 추가
  */
 
import groovy.json.*
import groovy.json.JsonSlurper

metadata {
	definition (name: "EasyRollSingle", namespace: "Inoshade", author: "Nuovothoth", runLocally: true, ocfDeviceType: "oic.d.blind", vid: "generic-shade") { //, runLocally: true
		capability "Window Shade"
        capability "Switch"
		capability "Switch Level"
        capability "Momentary"
        
        capability "Health Check"
     	capability "Refresh"
        capability "Polling"

        attribute "locDev1", "number"

		command "up"
    	command "stop"
        command "down"
        command "jogUp"
        command "jogDown"
        
        command "m1"
        command "m2"
        command "m3"
        
        command "topSave"
        command "bottomSave"
        
        command "poll"
	}
	
    preferences {
        input "easyrollAddress1", "text", type:"text", title:"IP Address 1", description: "enter easyroll address must be [ip]:[port] ", required: true
        input "ezInterval", "number", title: "Check Interval (unit:sec, 61: not checking)", description: "How often do you want to check the level of the blind?", range: "10..61", defaultValue: 10,  required: true 
        
        input name: "setMode", type: "bool", title: "SetMode", description: "On/Off"
        
    }

	tiles(scale: 2)  {
    	multiAttributeTile(name: "windowShade", type: "generic", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState("closed", label: 'closed', action: "windowShade.open", icon: "st.doors.garage.garage-closed", backgroundColor: "#A8A8C6", nextState: "closed")
                attributeState("open", label: 'open', action: "windowShade.close", icon: "st.doors.garage.garage-open", backgroundColor: "#F7D73E", nextState: "open")
                attributeState("partially open", label: 'partially\nopen', action: "windowShade.close", icon: "st.doors.garage.garage-closing", backgroundColor: "#D4ACEE", nextState: "closed")
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState("level", action: "setLevel")
            }
        }
        
    	valueTile("valueDev1", "device.locDev1", width: 4, height: 1, decoration: "flat") {
            state "val", label:'${currentValue}%', defaultState: true
        }
        
        standardTile("refresh", "command.refresh", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state("default", label: 'refresh', action: "refresh.refresh")
        }

		standardTile("up", "device.momentary", width: 4, height: 2, inactiveLabel: false, decoration: "flat") {
            state("up", label: 'up', action: "up", icon: "st.samsung.da.oven_ic_up")
        }
        standardTile("jogUp", "device.momentary", width: 2, height: 3, inactiveLabel: false, decoration: "flat") {
            state("jogUp", label: 'jogUp', action: "jogUp", icon: "st.samsung.da.oven_ic_plus")
        }
        standardTile("stop", "device.momentary", width: 4, height: 2, inactiveLabel: false, decoration: "flat") {
            state("stop", label: 'stop', action: "stop", icon: "st.samsung.da.washer_ic_cancel")
        }
        standardTile("jogDown", "device.momentary", width: 2, height: 3, inactiveLabel: false, decoration: "flat") {
            state("jogDown", label: 'jogDown', action: "jogDown", icon: "st.samsung.da.oven_ic_minus")
        }
        standardTile("down", "device.momentary", width: 4, height: 2, inactiveLabel: false, decoration: "flat") {
            state("close", label: 'down', action: "down", icon: "st.samsung.da.oven_ic_down")
        }
        
        standardTile("m1", "device.momentary", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state("push", label: 'M1', action: "m1", icon: "st.illuminance.illuminance.dark")
        }
        standardTile("m2", "device.momentary", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state("push", label: 'M2', action: "m2", icon: "st.illuminance.illuminance.dark")
        }
        standardTile("m3", "device.momentary", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state("push", label: 'M3', action: "m3", icon: "st.illuminance.illuminance.dark")
        }

        standardTile("topSave", "device.momentary", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state("push", label: "top save", action: "topSave")
        }
        standardTile("bottomSave", "device.momentary", width: 3, height: 1, inactiveLabel: false, decoration: "flat") {
            state("push", label: "bottom save", action: "bottomSave")
        }
        
        main(["windowShade"])
        details(["windowShade", "valueDev1", "refresh", "setMode", "up", "jogUp", "stop", "jogDown", "down", "m1", "m2", "m3", "topSave", "bottomSave"])
	}
}

def installed() {
    //log.debug "installed()"
    updated()
}

def uninstalled() {
    //log.debug "uninstalled()"
    unschedule()
}

def updated() {
	//log.debug("updated")
    sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "LAN", scheme:"untracked"]), displayed: false)
    resetting()
}

def ping() {
	//log.debug("ping")
	getCurData()
}

def resetting(){
	//log.debug("resetting")
	unschedule()
    if(ezIntarval!=61){
    	poll()
        runEvery10Minutes(resetting) //runIn이 도중에 멈추는 현상이 랜덤하게 발생하여 주기적 resetting이 필요.
    }
}

def poll() {
	//log.debug("poll")
	getCurData()
    runIn(ezInterval, poll)
}

/*refresh: 블라인드 위치값을 알기 위해 사용(추후 polling으로도 사용 가능)*/
def refresh() {
	//log.debug "refresh()"
    getCurData()
}
// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

/*HTTP REST API Request&response Handler*/
def runAction(String uri, String mode, def command){
	def options = [
        "method": "POST",
        "path": "${uri}",
        "headers": [
            "HOST": "${easyrollAddress1}"
        ],
        "body": '{'+
        	"mode:"+ "${mode}"+','+
            "command:"+ "${command}"+
        '}'
    ]
    
    def myhubAction = new physicalgraph.device.HubAction(options, null)
    sendHubCommand(myhubAction)
}

def getCurData() {
	//log.debug "getCurData()"
	def options = [
            "method": "GET",
            "path": "/lstinfo",
            "headers": [
                    "HOST": "${easyrollAddress1}"
            ]
    ]
    //log.debug "options: ${options}"
    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: fromHub])
    sendHubCommand(myhubAction)
}

def fromHub(physicalgraph.device.HubResponse hubResponse){
	//log.debug "callbackHub()"
    def msg
    try {
        msg = parseLanMessage(hubResponse.description)

        def resp = new JsonSlurper().parseText(msg.body)
		//log.debug "Parsing '${resp}'"
        //log.debug "Parsing '${resp.local_ip}'"
        //parse(resp)
        
        if(easyrollAddress1 == resp.local_ip) {
            //log.debug "sendEvent2"
            def realVal = 100-resp.position.intValue()
            //log.debug "realVal '${realVal}'"
            statusUpdate(realVal)
        }
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

/*명령어 실행 함수들*/
//퍼센트 이동 명령
def setLevel(value, rate = null) {
	//log.debug "setLevel($value)"
	if (setMode != true) {
    	runAction("/action", "level", 100-value)
        statusUpdate(value)
    }
}

def statusUpdate(value){
	//log.debug "statusUpdate()"
	sendEvent(name:"level", value: value)
    sendEvent(name: "locDev1", value: value)
    if(value==0){
        sendEvent(name:"windowShade", value: "closed")
    }else if(value==100){
        sendEvent(name:"windowShade", value: "open")
    }else{
        sendEvent(name:"windowShade", value: "partially open")
    }
}
//올리기(세팅모드일때는 강제올림)
def up() {
	//log.debug "Executing 'up'"
    if (setMode == true) {
    	runAction("/action", "force", "FTU")
    } else {
    	runAction("/action", "general", "TU")
    }  
}
//멈춤
def stop() {
	//log.debug "Executing 'stop'"
    if (setMode == true) {
    	runAction("/action", "force", "FSS")
    } else {
    	runAction("/action", "general", "SS")
    } 
}
//내리기(세팅모드일때는 강제내림)
def down() {
	//log.debug "Executing 'down'"
    if (setMode == true) {
        runAction("/action", "force", "FBD")
    } else {
    	runAction("/action", "general", "BD")
    } 
}
//한 칸 올리기(세팅모드일때는 강제 한 칸 올리기)
def jogUp() {
	//log.debug "Executing 'jogUp'"
    if (setMode == true) {
        runAction("/action", "force", "FSU")
    } else {
    	runAction("/action", "general", "SU")
    } 
}
//한 칸 내리기(세팅모드일때는 강제 한 칸 내리기)
def jogDown() {
	//log.debug "Executing 'jogDown'"
    if (setMode == true) {
        runAction("/action", "force", "FSD")
    } else {
    	runAction("/action", "general", "SD")
    } 
}
//메모리1 이동 (세팅 모드 시 현재 위치 메모리1에 저장)
def m1() {
	//log.debug "Executing 'm1'"
    if (setMode == true) {
        runAction("/action", "save", "SM1")
    } else {
    	runAction("/action", "general", "M1")
    } 
}
//메모리2 이동 (세팅 모드 시 현재 위치 메모리2에 저장)
def m2() {
	//log.debug "Executing 'm2'"
    if (setMode == true) {
        runAction("/action", "save", "SM2")
    } else {
    	runAction("/action", "general", "M2")
    } 
}
//메모리3 이동 (세팅 모드 시 현재 위치 메모리3에 저장)
def m3() {
	//log.debug "Executing 'm3'"
    if (setMode == true) {
        runAction("/action", "save", "SM3")
    } else {
    	runAction("/action", "general", "M3")
    } 
}
//최상단, 최하단은 세팅모드에서만 동작하도록 함
def bottomSave() {
	//log.debug "Executing 'bottomSave'"
    if (setMode == true) {
        runAction("/action", "save", "SB")
    }
}
def topSave() {
	//log.debug "Executing 'topSave'"
    if (setMode == true) {
        runAction("/action", "save", "ST")
    }
}

def close() {
       setLevel(0)
}

def open() {
       setLevel(100)
}

def off() {
    //log.debug "off()"
   close()
}

def on() {
    //log.debug "on()"
   open()
}