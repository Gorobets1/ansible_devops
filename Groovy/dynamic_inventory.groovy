#!/usr/bin/env groovy

import groovy.json.JsonSlurper

import groovy.json.JsonBuilder

// create json build object 

JsonBuilder builder = new JsonBuilder()

// create stringBuilder objects to catch the curl output and errors, execute cutl to pull containers info from the local unix socker docker api 
  
def sout = new StringBuilder(), serr = new StringBuilder()
def proc = 'sudo curl --unix-socket /var/run/docker.sock http:/v1.24/containers/json'.execute()
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)


// write the curl output to the docker_api.json file 
def docker_api = new File('/home/ansible/ansible_devops/Groovy/docker_api.json')

docker_api.write("$sout $serr")

//parse the docker api file for running containers
JsonSlurper slurper = new JsonSlurper()
def result = slurper.parse(new File('/home/ansible/ansible_devops/Groovy/docker_api.json'))

//  create lists for container names and IP addresses
def Names = []
def IPs = []
def Networks = result.NetworkSettings.Networks
Networks.each(){

    IPs << it.values().IPAddress[0]
}

for (i in result.Names){
    def cont = i[0].split("/")[1]
    Names.add(cont)
}
// create map for containers with their names and IPs matching
Map Containers = [:]
// transpose Names and IPs lists and then put them to the Containers map.
GroovyCollections.transpose([Names, IPs]).each(){

    Containers.put("${it[0]}", "${it[1]}")


}

Map inventory = ["_meta":[hostvars:[:]],all:[children:["database","loadbalancer","slaves","ungrouped","webserver"]],webserver:[hosts:[]],loadbalancer:[hosts:[]],database:[hosts:[]],slaves:[hosts:[]],ungrouped:[hosts:[]]]

// iterate over Containers and update inventory Map with the correct container names and IP addresses

Containers.each { entry ->
    if ("$entry.key".find("app")) {
        inventory._meta.hostvars.put("$entry.key",[ansible_host:"$entry.value"])
        inventory.webserver.hosts.add("$entry.key")
    } else if ("$entry.key".find("lb")) {
        inventory._meta.hostvars.put("$entry.key",[ansible_host:"$entry.value"])
        inventory.loadbalancer.hosts.add("$entry.key")
    } else if ("$entry.key".find("js")) {
        inventory._meta.hostvars.put("$entry.key",[ansible_host:"$entry.value"])
        inventory.slaves.hosts.add("$entry.key")
    } else if ("$entry.key".find("db")) {
        inventory._meta.hostvars.put("$entry.key",[ansible_host:"$entry.value"])
        inventory.database.hosts.add("$entry.key")
    }
    else {
        inventory._meta.hostvars.put("$entry.key",[ansible_host:"$entry.value"])
        inventory.ungrouped.hosts.add("$entry.key")
    }
}



//build json object out of the inventory Map 

builder inventory

//print out the json object 
println builder







