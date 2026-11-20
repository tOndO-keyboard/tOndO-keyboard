import UIKit
import TondoCore

/// The keyboard view: forwards multi-touch input to the shared engine and paints its scene.
/// Mirror of tondo/android/.../TondoKeyboardView.kt.
final class TondoKeyboardView: UIView {
    let engine: KeyboardEngine
    private let painter = ScenePainter()
    private var displayLink: CADisplayLink?
    private var touchIds: [ObjectIdentifier: Int32] = [:]
    private var nextTouchId: Int32 = 0

    init(engine: KeyboardEngine) {
        self.engine = engine
        super.init(frame: .zero)
        isMultipleTouchEnabled = true
        isOpaque = true
        contentMode = .redraw
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    private static func nowMillis() -> Int64 { Int64(CACurrentMediaTime() * 1000) }

    override func layoutSubviews() {
        super.layoutSubviews()
        engine.setViewSize(widthPx: Float(bounds.width), heightPx: Float(bounds.height))
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        engine.setViewSize(widthPx: Float(bounds.width), heightPx: Float(bounds.height))
        let animating = engine.tick(timeMillis: Self.nowMillis())
        painter.paint(engine.render(), in: ctx)
        animating ? startAnimating() : stopAnimating()
    }

    /// Called by the host when the engine asks for a new frame.
    func requestRender() {
        setNeedsDisplay()
    }

    func trimMemory() { painter.trimMemory() }

    // MARK: - Frame loop (only while something animates or a finger is down)

    private func startAnimating() {
        guard displayLink == nil else { return }
        let link = CADisplayLink(target: self, selector: #selector(onFrame))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    private func stopAnimating() {
        displayLink?.invalidate()
        displayLink = nil
    }

    @objc private func onFrame() { setNeedsDisplay() }

    // MARK: - Touches

    private func send(_ touches: Set<UITouch>, _ action: TouchAction, release: Bool) {
        for touch in touches {
            let key = ObjectIdentifier(touch)
            let id: Int32
            if let existing = touchIds[key] {
                id = existing
            } else {
                id = nextTouchId
                nextTouchId &+= 1
                touchIds[key] = id
            }
            let p = touch.location(in: self)
            engine.onTouch(e: TouchEvent(pointerId: id, action: action, x: Float(p.x), y: Float(p.y), timeMillis: Self.nowMillis()))
            if release { touchIds.removeValue(forKey: key) }
        }
        setNeedsDisplay()
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, .down, release: false) }
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, .move, release: false) }
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, .up, release: true) }
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, .cancel, release: true) }
}
