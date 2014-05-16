/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

/**
 * This class provides access to device proximity data.
 * @constructor
 */
var argscheck = require('cordova/argscheck'),
    exec = require("cordova/exec");

var proximity = {
    /**
     *  Get the current proximity sensor state.
     *  @param successCallback  callback function which delivers the boolean sensor state
     */
    getProximityState: function(successCallback) {
        argscheck.checkArgs('F', 'proximity.getProximityState', arguments);
        exec(successCallback, null, "Proximity", "getProximityState", []);
    },

    /**
     *  Enable the proximity sensor. Needs to be called before getting the proximity state.
     */
    enableSensor: function() {
        exec(null, null, "Proximity", "start", []);
    },

    /**
     *  Disable the proximity sensor.
     */
    disableSensor: function() {
        exec(null, null, "Proximity", "stop", []);
    }
};
module.exports = proximity;
