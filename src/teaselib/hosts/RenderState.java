package teaselib.hosts;

import java.awt.image.BufferedImage;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;

/**
 * @author Citizen-Cane
 *
 */
class RenderState {

    String displayImageResource;
    BufferedImage displayImage;
    HumanPose.Estimation pose;
    HumanPose.Proximity actorProximity;

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
        this.actorProximity = Proximity.FAR;

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
        copy.actorProximity = this.actorProximity;

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
                || actorProximity != oldState.actorProximity//
                || isIntertitle != oldState.isIntertitle;

        if (renderText()) {
            renderedText = text;
        } else {
            renderedText = "";
        }
    }

    boolean renderText() {
        return focusLevel == 1.0 && actorProximity != Proximity.CLOSE && !text.isBlank();
    }

}