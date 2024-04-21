## Device 
1. Device RPC (**uri: ipc:///ipc/device_rpc**)
    * `GetAssetState` (request: [b"GetAssetState", anx.Empty], response: anx.AssetState)

    * `StartDeviceImu` (request: [b"StartDeviceImu", anx.StartDeviceImu], response: anx.StdResponse)

    * `StartDeviceGnss` (request: [b"StartDeviceGnss", anx.Empty], response: anx.StdResponse) 
     
    * `StartDeviceCamera` (request: [b"StartDeviceCamera", anx.StartDeviceCamera], response: anx.StdResponse)

    * `StopDeviceImu` (request: [b"StopDeviceImu", anx.Empty], response: anx.StdResponse)

    * `StopDeviceGnss` (request: [b"StopDeviceGnss", anx.Empty], response: anx.StdResponse)

    * `StopDeviceCamera` (request: [b"StopDeviceCamera", anx.Empty], response: anx.StdResponse)

    * `GetImeiNumbers` (request: [b"GetImeiNumbers", anx.Empty], response: anx.GetImeiNumbersResponse)
    
    * `Shutdown` (request: [b"Shutdown", anx.Empty], response: anx.StdResponse)

    * `Reboot` (request: [b"Reboot", anx.Empty], response: anx.StdResponse)

    * `RestartAnxService` (request: [b"RestartAnxService", anx.Empty], response: anx.StdResponse)

    * `SetWifi` (request: [b"SetWifi", anx.SetWifiRequest], response: anx.StdResponse)
    
    * `SetHotspot` (request: [b"SetHotspot", anx.SetWifiRequest], response: anx.StdResponse)

    * `GetFloOsVersion` (request: [b"GetFloOsVersion", anx.Empty], response: anx.VersionResponse)

    * `GetAnxVersion` (request: [b"GetAnxVersion", anx.Empty], response: anx.VersionResponse)

    * `StartAndroidLogs` (request: [b"StartAndroidLogs", anx.Empty], response: anx.StdResponse)

    * `StopAndroidLogs` (request: [b"StopAndroidLogs", anx.Empty], response: anx.StdResponse)

2. GPU (**uri: ipc:///ipc/gpu**), DSP (**uri: ipc:///ipc/dsp**), CPU (**uri: ipc:///ipc/cpu**)
    * `LoadModel` (request: [b"LoadModel", anx.Payload], response [anx.StdResponse, anx.ModelMeta])

    * `InvokeModel` (request: [b"InvokeModel", anx.PayloadArray], response: anx.PayloadArray)

    * `UnloadModel` (request: [b"UnloadModel"], response: anx.StdResponse)

## Assets (Device sensors)
> Asset data stream

* `DeviceImu` (anx.ImuData) (**uri: ipc:///ipc/device_imu**)

* `DeviceGnss` (anx.GnssData) (**uri: ipc:///ipc/device_gnss**)

* `DeviceCamera` (anx.CameraData) (**uri: ipc:///ipc/device_camera**)
