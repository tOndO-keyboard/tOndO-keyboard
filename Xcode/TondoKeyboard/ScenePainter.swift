import UIKit
import TondoCore

/// Paints a `Scene` produced by the shared Kotlin engine with CoreGraphics.
/// Mirror of tondo/android/.../CanvasPainter.kt: the keyboard looks identical on both platforms.
final class ScenePainter {
    /// Scene.FONT_SCALE in tondo/core (TextMeshPro sizes -> em size).
    private let fontScale: CGFloat = 0.75
    private let assetsURL: URL = Bundle.main.bundleURL.appendingPathComponent("assets")

    /// Small, bounded cache: the keyboard extension has a strict memory budget.
    private let images: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.totalCostLimit = 8 * 1024 * 1024
        return cache
    }()
    private var missing = Set<String>()

    func trimMemory() {
        images.removeAllObjects()
    }

    private func image(_ name: String) -> UIImage? {
        if let cached = images.object(forKey: name as NSString) { return cached }
        if missing.contains(name) { return nil }
        let path = name.hasPrefix("emoji/") ? "\(name).png" : "icons/\(name).png"
        guard let img = UIImage(contentsOfFile: assetsURL.appendingPathComponent(path).path) else {
            missing.insert(name)
            return nil
        }
        let cost = Int(img.size.width * img.size.height * img.scale * img.scale * 4)
        images.setObject(img, forKey: name as NSString, cost: cost)
        return img
    }

    private func ui(_ c: TondoCore.Color) -> UIColor {
        UIColor(red: CGFloat(c.r), green: CGFloat(c.g), blue: CGFloat(c.b), alpha: CGFloat(c.a))
    }

    private func cg(_ r: TondoCore.Rect) -> CGRect {
        CGRect(x: CGFloat(r.left), y: CGFloat(r.top), width: CGFloat(r.right - r.left), height: CGFloat(r.bottom - r.top))
    }

    private func pt(_ v: Vec) -> CGPoint { CGPoint(x: CGFloat(v.x), y: CGFloat(v.y)) }

    private func radians(_ deg: Float) -> CGFloat { CGFloat(deg) * .pi / 180 }

    func paint(_ scene: Scene, in ctx: CGContext) {
        ctx.saveGState()
        ctx.scaleBy(x: CGFloat(scene.scale), y: CGFloat(scene.scale))
        for case let op as DrawOp in scene.ops {
            draw(op, ctx)
        }
        ctx.restoreGState()
    }

    private func draw(_ op: DrawOp, _ ctx: CGContext) {
        switch op {
        case let o as DrawOp.FillRect:
            ctx.setFillColor(ui(o.color).cgColor)
            ctx.fill(cg(o.rect))

        case let o as DrawOp.FillRoundRect:
            let r = cg(o.rect)
            let radius = CGFloat(o.radius)
            var corners: UIRectCorner = []
            if o.roundTop { corners.formUnion([.topLeft, .topRight]) }
            if o.roundBottom { corners.formUnion([.bottomLeft, .bottomRight]) }
            let path = UIBezierPath(roundedRect: r, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
            ctx.setFillColor(ui(o.color).cgColor)
            ctx.addPath(path.cgPath)
            ctx.fillPath()

        case let o as DrawOp.FillCircle:
            let r = CGFloat(o.radius)
            ctx.setFillColor(ui(o.color).cgColor)
            ctx.fillEllipse(in: CGRect(x: CGFloat(o.center.x) - r, y: CGFloat(o.center.y) - r, width: r * 2, height: r * 2))

        case let o as DrawOp.StrokeCircle:
            let r = CGFloat(o.radius)
            ctx.setStrokeColor(ui(o.color).cgColor)
            ctx.setLineWidth(CGFloat(o.width))
            ctx.strokeEllipse(in: CGRect(x: CGFloat(o.center.x) - r, y: CGFloat(o.center.y) - r, width: r * 2, height: r * 2))

        case let o as DrawOp.FillSector:
            // Angles are clockwise on screen; UIKit's y-down space makes `clockwise: true` match.
            let c = pt(o.center)
            let start = radians(o.startAngle)
            let end = radians(o.startAngle + o.sweepAngle)
            let path = UIBezierPath(arcCenter: c, radius: CGFloat(o.outerRadius), startAngle: start, endAngle: end, clockwise: true)
            if o.innerRadius > 0 {
                path.addArc(withCenter: c, radius: CGFloat(o.innerRadius), startAngle: end, endAngle: start, clockwise: false)
            } else {
                path.addLine(to: c)
            }
            path.close()
            ctx.setFillColor(ui(o.color).cgColor)
            ctx.addPath(path.cgPath)
            ctx.fillPath()

        case let o as DrawOp.Capsule:
            ctx.setStrokeColor(ui(o.color).cgColor)
            ctx.setLineWidth(CGFloat(o.width))
            ctx.setLineCap(.round)
            ctx.move(to: pt(o.from))
            ctx.addLine(to: pt(o.to))
            ctx.strokePath()

        case let o as DrawOp.CircleShadow:
            let rr = CGFloat(o.radius + o.blur)
            let c0 = ui(o.color)
            let f = max(0.01, min(0.98, CGFloat((o.radius - o.blur) / (o.radius + o.blur))))
            let colors = [c0.cgColor, c0.cgColor, c0.withAlphaComponent(0).cgColor] as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, f, 1]) {
                ctx.drawRadialGradient(gradient, startCenter: pt(o.center), startRadius: 0, endCenter: pt(o.center), endRadius: rr, options: [])
            }

        case let o as DrawOp.RoundRectShadow:
            let steps = 8
            for s in stride(from: steps, through: 1, by: -1) {
                let d = CGFloat(o.blur) * CGFloat(s) / CGFloat(steps)
                let r = cg(o.rect).insetBy(dx: -d + CGFloat(o.blur) / 2, dy: -d + CGFloat(o.blur) / 2)
                ctx.setFillColor(ui(o.color.withAlpha(alpha: o.color.a / Float(steps))).cgColor)
                ctx.addPath(UIBezierPath(roundedRect: r, cornerRadius: CGFloat(o.radius) + d).cgPath)
                ctx.fillPath()
            }

        case let o as DrawOp.Text:
            drawText(o)

        case let o as DrawOp.Icon:
            guard let img = image(o.name) else { return }
            let tinted = img.withTintColor(ui(o.tint), renderingMode: .alwaysOriginal)
            let r = cg(o.rect)
            if o.flipY {
                ctx.saveGState()
                ctx.translateBy(x: 0, y: r.midY)
                ctx.scaleBy(x: 1, y: -1)
                ctx.translateBy(x: 0, y: -r.midY)
                tinted.draw(in: r)
                ctx.restoreGState()
            } else {
                tinted.draw(in: r)
            }

        case let o as DrawOp.Image:
            guard let img = image(o.name) ?? o.fallback.flatMap({ image($0) }) else { return }
            img.draw(in: cg(o.rect), blendMode: .normal, alpha: CGFloat(o.alpha))

        case let o as DrawOp.PushClip:
            ctx.saveGState()
            ctx.clip(to: cg(o.rect))

        case is DrawOp.PopClip:
            ctx.restoreGState()

        default:
            break
        }
    }

    private func drawText(_ o: DrawOp.Text) {
        let size = CGFloat(o.size) * fontScale
        let font = UIFont(name: o.bold ? "Arial-BoldMT" : "ArialMT", size: size)
            ?? (o.bold ? UIFont.boldSystemFont(ofSize: size) : UIFont.systemFont(ofSize: size))
        let paragraph = NSMutableParagraphStyle()
        paragraph.lineBreakMode = .byTruncatingTail
        paragraph.alignment = o.align == TextAlign.center ? .center : .left
        let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: ui(o.color), .paragraphStyle: paragraph]
        let text = o.text as NSString
        let anchor = pt(o.anchor)

        if o.maxWidth <= 0 {
            let s = text.size(withAttributes: attrs)
            let x = o.align == TextAlign.center ? anchor.x - s.width / 2 : anchor.x
            text.draw(at: CGPoint(x: x, y: anchor.y - s.height / 2), withAttributes: attrs)
            return
        }
        let width = CGFloat(o.maxWidth)
        let maxHeight = font.lineHeight * CGFloat(max(1, o.maxLines))
        let bounds = text.boundingRect(with: CGSize(width: width, height: maxHeight),
                                       options: [.usesLineFragmentOrigin, .truncatesLastVisibleLine],
                                       attributes: attrs, context: nil)
        let h = min(maxHeight, ceil(bounds.height))
        let x = o.align == TextAlign.center ? anchor.x - width / 2 : anchor.x
        text.draw(with: CGRect(x: x, y: anchor.y - h / 2, width: width, height: h),
                  options: [.usesLineFragmentOrigin, .truncatesLastVisibleLine],
                  attributes: attrs, context: nil)
    }
}
