//
//  KeyboardViewController.swift
//  tOndO_keyboard
//
//  Created by user on 08/01/22.
//  Copyright 2022 WEACW. All rights reserved.
//

import UIKit
import Flutter

class KeyboardViewController: UIInputViewController {

    @IBOutlet var nextKeyboardButton: UIButton!
    private var flutterEngine: FlutterEngine?
    private var flutterViewController: FlutterViewController?
    
    override func updateViewConstraints() {
        super.updateViewConstraints()
        
        // Add custom view sizing constraints here
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Perform custom UI setup here
        
        self.nextKeyboardButton = UIButton(type: .system)
        
        self.nextKeyboardButton.setTitle(NSLocalizedString("Next Keyboard", comment: "Title for 'Next Keyboard' button"), for: [])
        self.nextKeyboardButton.sizeToFit()
        self.nextKeyboardButton.translatesAutoresizingMaskIntoConstraints = false
        
        self.nextKeyboardButton.addTarget(self, action: #selector(handleInputModeList(from:with:)), for: .allTouchEvents)
        
        self.view.addSubview(self.nextKeyboardButton)
        
        self.nextKeyboardButton.leftAnchor.constraint(equalTo: self.view.leftAnchor).isActive = true
        self.nextKeyboardButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor).isActive = true
        
        // Setup Flutter
        flutterEngine = FlutterEngine(name: "tOndO Keyboard Flutter Engine")
        flutterEngine?.run()
        
        flutterViewController = FlutterViewController(engine: flutterEngine!, nibName: nil, bundle: nil)
        if let flutterView = flutterViewController?.view {
            flutterView.frame = self.view.bounds
            flutterView.translatesAutoresizingMaskIntoConstraints = false
            self.view.addSubview(flutterView)
            
            // Setup constraints
            NSLayoutConstraint.activate([
                flutterView.topAnchor.constraint(equalTo: self.view.topAnchor),
                flutterView.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
                flutterView.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
                flutterView.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
            ])
            
            self.view.sendSubviewToBack(flutterView)
        }
    }
    
    override func viewWillLayoutSubviews() {
        self.nextKeyboardButton.isHidden = !self.needsInputModeSwitchKey
        super.viewWillLayoutSubviews()
    }
    
    override func didReceiveMemoryWarning() {
        print("[TONDO KEYBOARD LOG] didReceiveMemoryWarning!");
    }
    
    override func textWillChange(_ textInput: UITextInput?) {
        // The app is about to change the document's contents. Perform any preparation here.
    }
    
    override func textDidChange(_ textInput: UITextInput?) {
        // The app has just changed the document's contents, the document context has been updated.
        
        var textColor: UIColor
        let proxy = self.textDocumentProxy
        if proxy.keyboardAppearance == UIKeyboardAppearance.dark {
            textColor = UIColor.white
        } else {
            textColor = UIColor.black
        }
        self.nextKeyboardButton.setTitleColor(textColor, for: [])
    }

}
