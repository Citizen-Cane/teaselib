package teaselib.hosts;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

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
    double actorZoom;

    String text;
    boolean isIntertitle;

    float focusLevel;

    //
    // Composition stack

    boolean repaintSceneImage;
    AffineTransform t;
    BufferedImage sceneImage;

    boolean repaintTextImage;
    BufferedImage textImage;

    RenderState() {
        this.displayImageResource = "";
        this.displayImage = null;
        this.pose = HumanPose.Estimation.NONE;
        this.actorZoom = ProximitySensor.zoom.get(Proximity.FAR);

        this.text = "";
        this.isIntertitle = false;

        this.focusLevel = 1.0f;

        this.t = null;
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
        copy.actorZoom = this.actorZoom;

        copy.text = this.text;
        copy.isIntertitle = this.isIntertitle;

        copy.focusLevel = this.focusLevel;

        copy.t = this.t;
        copy.repaintSceneImage = false;
        copy.sceneImage = this.sceneImage;

        copy.repaintTextImage = false;
        copy.textImage = this.textImage;

        return copy;
    }

    public void updateFrom(RenderState oldState) {
        repaintSceneImage = displayImageResource != oldState.displayImageResource || actorZoom != oldState.actorZoom;
        repaintTextImage = text != oldState.text || isIntertitle != oldState.isIntertitle;
    }

    // TODO apply rules elsewhere
    boolean renderText() {
        return focusLevel == 1.0 && actorZoom < ProximitySensor.zoom.get(Proximity.CLOSE) && !text.isBlank();
    }

}
