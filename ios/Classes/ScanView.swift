//
//  ScanView.swift
//  anyline_plugin
//
//  Created by Rohit Lokhande on 26/07/24.
//

import Flutter
import UIKit
import Anyline

class ScanView: NSObject, FlutterPlatformView, FlutterStreamHandler {

    private var _view: UIView    
    var scanView: ALScanView!
    var events: FlutterEventSink!

    init(
        frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?,
        binaryMessenger messenger: FlutterBinaryMessenger?
    ) {
        _view = UIView()    
        super.init()

        // Event Channel created that sends result backto flutter in stream
        let eventChannel = FlutterEventChannel(name: "anyline_embedded_plugin_\(viewId)", binaryMessenger: messenger!) // timeHandlerEvent is event name
        eventChannel.setStreamHandler(self)    

        do {
            let arguments = args as? [String: Any]
            let viewConfig = arguments?["config"] as? [String : Any]
            self.scanView = try ALScanViewFactory.withJSONDictionary(viewConfig!, delegate: self)
            _view.addSubview(self.scanView)
            self.scanView.translatesAutoresizingMaskIntoConstraints = false
            self.scanView.leftAnchor.constraint(equalTo: _view.leftAnchor).isActive = true
            self.scanView.rightAnchor.constraint(equalTo: _view.rightAnchor).isActive = true
            self.scanView.topAnchor.constraint(equalTo: _view.topAnchor).isActive = true
            self.scanView.bottomAnchor.constraint(equalTo: _view.bottomAnchor).isActive = true
            self.scanView.startCamera()
            try self.scanView.startScanning()
        } catch {
    
        }
    }

    func view() -> UIView {    
        return _view
    }

    func getDocumentDirectoryPath() -> NSString {
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)    
        let documentsDirectory = paths[0]
        return documentsDirectory as NSString
    }

    func saveImageToDocumentsDirectory(image: UIImage) -> String? {
        if let data = image.pngData() {
            let dirPath = getDocumentDirectoryPath()
            let timestamp = NSDate().timeIntervalSince1970
            let imageFileUrl = URL(fileURLWithPath: dirPath.appendingPathComponent(String(format: "%f",timestamp * 1000)) as String)
            do {
                try data.write(to: imageFileUrl)
                print("Successfully saved image at path: \(imageFileUrl)")
                return imageFileUrl.path
            } catch {
                print("Error saving image: \(error)")
            }
        }
        return nil
    }
    
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.events = events
        return nil
        
    }

    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        return nil
        
    }
}    

extension ScanView: ALScanPluginDelegate {
    
    func scanPlugin(_ scanPlugin: ALScanPlugin, resultReceived scanResult: ALScanResult) {
        // Send result back to flutter
        var tempDir : [String:Any] = [:]
        tempDir["result"] = scanResult.pluginResult.asJSONString()
        tempDir["cutoutImage"] = self.saveImageToDocumentsDirectory(image: scanResult.croppedImage)
        self.events(tempDir)
    }
}
