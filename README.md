<!---
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

# org.awokenwell.proximity

This plugin provides access to the device's (IR) proximity sensor. This sensor is typically used in applications to prevent touch events on the screen when the device is held close to one's face.

## Installation

    cordova plugin add https://github.com/awoken-well/cordova-plugin-proximity.git

## Supported Platforms

- iOS
- Android

## Methods

- navigator.proximity.getProximityState
- navigator.proximity.enableSensor
- navigator.proximity.disableSensor

## navigator.proximity.getProximityState

Get the current proximity sensor state: true = near, false = far.

This proximity state is returned to the 'successCallback' callback function.

    navigator.proximity.getProximityState(successCallback);

## navigator.proximity.enableSensor

Enable the proximity sensor. In iOS the proximity sensor is disabled by default and must
be enabled manually.

    navigator.proximity.enableSensor();

## navigator.proximity.disableSensor

Disable the proximity sensor.

    navigator.proximity.disableSensor();

### Example

    function onSuccess(state) {
        alert('Proximity state: ' + state? 'near' : 'far');
    };

    navigator.proximity.enableSensor();
    
    setInterval(function(){
      navigator.getProximityState(onSuccess);
    }, 1000);


### Example2

This example shows a watcher. If other things approaches the phone, 'on_approch_callback' would be called. 


    var proximitysensor = {
    };

    var proximitysensorWatchStart = function(_scope, on_approch_callback) {
        if(navigator.proximity == null)
            console.log("cannot find navigator.proximity");

        navigator.proximity.enableSensor();

        // Start watch timer to get proximity sensor value
        var frequency = 100;
        _scope.id = window.setInterval(function() {
            navigator.proximity.getProximityState(function(val) { // on success
                var timestamp = new Date().getTime();
                if(timestamp - _scope.lastemittedtimestamp > 1000) { // test on each 1 secs
                    if( proximitysensor.lastval == 1 && val == 0 ) { // far => near changed
                        _scope.lastemittedtimestamp = timestamp;
                        _scope.lastval = val;
                        on_approch_callback(timestamp);
                    }
                }
                _scope.lastval = val;
            });
        }, frequency);
    }

    var proximitysensorWatchStop = function(_scope) {
        if(navigator.proximity == null)
            console.log("cannot find navigator.proximity");

        window.clearInterval( _scope.id );

        navigator.proximity.disableSensor();
    };

    proximitysensorWatchStart(proximitysensor, function(timestamp) {
       console.log('approched on ' + timestamp);
    });

    // .... after testing
    //proximitysensorWatchStop(proximitysensor);


### iOS Quirks

- iOS will automatically dim the screen and disable touch events when the proximity sensor is in the 'near' state. This can be circumvented by using undocumented API calls, but will result in App Store rejection.
