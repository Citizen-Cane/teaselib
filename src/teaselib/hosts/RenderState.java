package teaselib.hosts;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.ProximitySensor;

/**
 * @author Citizen-Cane
 *
 */
class RenderState {

    // Scene properties
    String displayImageResource;
    BufferedImage displayImage;
    HumanPose.Estimation pose;
    Point2D actorOffset;
    double actorZoom;

    String text;
    boolean isIntertitle;

    float focusLevel;

    //
    // Composition stack

    boolean repaintSceneImage;
    AffineTransform transform;
    float alpha;
    BufferedImage sceneImage;

    boolean repaintTextImage;
    BufferedImage textImage;

    RenderState() {
        this.displayImageResource = "";
        this.displayImage = null;
        this.pose = HumanPose.Estimation.NONE;
        this.actorOffset = new Point2D.Double();
        this.actorZoom = ProximitySensor.zoom.get(Proximity.FAR);

        this.text = "";
        this.isIntertitle = false;

        this.focusLevel = 1.0f;

        this.transform = null;
        this.alpha = 1.0f;
        this.repaintSceneImage = false;
        this.sceneImage = null;

        this.repaintTextImage = false;
        this.textImage = null;
    }

    RenderState copy() {
        RenderState copy = new RenderState();

        copy.displayImageResource = this.displayImageResource;
        copy.displayImage = this.displayImage;
        copy.pose = this.pose;
        copy.actorOffset = new Point2D.Double(this.actorOffset.getX(), this.actorOffset.getY());
        copy.actorZoom = this.actorZoom;

        copy.text = this.text;
        copy.isIntertitle = this.isIntertitle;

        copy.focusLevel = this.focusLevel;

        copy.transform = this.transform;
        copy.alpha = 1.0f;
        copy.repaintSceneImage = false;
        copy.sceneImage = this.sceneImage;

        copy.repaintTextImage = false;
        copy.textImage = this.textImage;

        return copy;
    }

    public void updateFrom(RenderState oldState) {
        repaintSceneImage = actorZoom != oldState.actorZoom ||
                alpha != oldState.alpha ||
                !Objects.equals(displayImageResource, oldState.displayImageResource) ||
                !Objects.equals(actorOffset, oldState.actorOffset);
        repaintTextImage = isIntertitle != oldState.isIntertitle ||
                !Objects.equals(text, oldState.text);
    }

    // TODO apply rules elsewhere
    boolean renderText() {
        return focusLevel == 1.0 && actorZoom < ProximitySensor.zoom.get(Proximity.CLOSE) && !text.isBlank();
    }

}
