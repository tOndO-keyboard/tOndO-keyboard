//
//  ViewController.swift
//  native_app
//
//  Created by NSWell on 2019/12/19.
//  Copyright © 2019 WEACW. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        UnityEmbeddedSwift.showUnity()
        
        let uView = UnityEmbeddedSwift.getUnityView()
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: {
            self.view.addSubview(uView!)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1, execute: {
                self.view.sendSubviewToBack(uView!)
            })
        })
    }
}

