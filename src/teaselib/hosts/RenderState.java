package teaselib.hosts;

import java.awt.image.BufferedImage;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.ProximitySensor;

/**
 * @author Citizen-Cane
 *
 */
class RenderState {

    String displayImageResource;
    BufferedImage displayImage;
    HumanPose.Estimation pose;
    double actorZoom;

    String text;
    boolean isIntertitle;

    float focusLevel;

    boolean repaintSceneImage;
    BufferedImage sceneImage;

    BufferedImage renderedImage;
    String renderedText;

    RenderState() {
        this.displayImageResource = "";
        this.displayImage = null;
        this.pose = HumanPose.Estimation.NONE;
        this.actorZoom = ProximitySensor.zoom.get(Proximity.FAR);

        this.text = "";
        this.isIntertitle = false;

        this.focusLevel = 1.0f;

        this.repaintSceneImage = false;
        this.sceneImage = null;

        this.renderedImage = null;
        this.renderedText = null;

    }

    RenderState copy() {
        RenderState copy = new RenderState();

        copy.displayImage = this.displayImage;
        copy.displayImageResource = this.displayImageResource;
        copy.pose = this.pose;
        copy.actorZoom = this.actorZoom;

        copy.text = this.text;
        copy.isIntertitle = this.isIntertitle;

        copy.focusLevel = this.focusLevel;

        copy.repaintSceneImage = false;
        copy.sceneImage = this.sceneImage;

        copy.renderedImage = this.renderedImage;
        copy.renderedText = this.renderedText;

        return copy;
    }

    public void updateFrom(RenderState oldState) {
        repaintSceneImage = displayImageResource != oldState.displayImageResource //
                || actorZoom != oldState.actorZoom//
                || isIntertitle != oldState.isIntertitle;

        if (renderText()) {
            renderedText = text;
        } else {
            renderedText = "";
        }
    }

    boolean renderText() {
        return focusLevel == 1.0 && actorZoom < ProximitySensor.zoom.get(Proximity.CLOSE) && !text.isBlank();
    }

}