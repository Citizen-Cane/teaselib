package teaselib.hosts;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.VolatileImage;
import java.util.Objects;
import java.util.Set;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.ProximitySensor;
import teaselib.util.AnnotatedImage;

/**
 * @author Citizen-Cane
 *
 */
class RenderState {

    // Scene properties
    String displayImageResource;
    VolatileImage displayImage;
    HumanPose.Estimation pose;
    Set<AnnotatedImage.Annotation> annotations;
    double actorZoom;
    Point2D displayImageOffset;

    String text;
    boolean isIntertitle;

    float focusLevel;

    //
    // Composition stack

    boolean repaintSceneImage;
    AffineTransform transform;
    float sceneBlend;

    boolean repaintTextImage;
    VolatileImage textImage;
    Rectangle textImageRegion;
    float textBlend;

    RenderState() {
        this.displayImageResource = "";
        this.displayImage = null;
        this.pose = HumanPose.Estimation.NONE;
        this.annotations = null;
        this.actorZoom = ProximitySensor.zoom.get(Proximity.FAR);

        this.text = "";
        this.isIntertitle = false;

        this.focusLevel = 1.0f;

        this.transform = null;
        this.sceneBlend = 1.0f;
        this.repaintSceneImage = false;
        this.displayImageOffset = new Point2D.Double();

        this.repaintTextImage = false;
        this.textImage = null;
        this.textImageRegion = null;
        this.textBlend = 1.0f;
    }

    RenderState copy() {
        RenderState copy = new RenderState();

        copy.displayImageResource = this.displayImageResource;
        copy.displayImage = this.displayImage;
        copy.pose = this.pose;
        copy.annotations = this.annotations;
        copy.actorZoom = this.actorZoom;

        copy.text = this.text;
        copy.isIntertitle = this.isIntertitle;

        copy.focusLevel = this.focusLevel;

        copy.transform = this.transform;
        copy.sceneBlend = this.sceneBlend;
        copy.repaintSceneImage = false;
        copy.displayImageOffset = this.displayImageOffset;

        copy.repaintTextImage = false;
        copy.textImage = this.textImage;
        copy.textImageRegion = this.textImageRegion;
        copy.textBlend = this.textBlend;

        return copy;
    }

    public void updateFrom(RenderState oldState) {
        repaintSceneImage = oldState.repaintSceneImage ||
                actorZoom != oldState.actorZoom ||
                sceneBlend != oldState.sceneBlend ||
                !Objects.equals(displayImageResource, oldState.displayImageResource) ||
                !Objects.equals(displayImageOffset, oldState.displayImageOffset);
        repaintTextImage = oldState.repaintTextImage ||
                isIntertitle != oldState.isIntertitle ||
                !Objects.equals(text, oldState.text);
    }

    // TODO apply rules elsewhere
    boolean renderText() {
        return focusLevel == 1.0 && actorZoom < ProximitySensor.zoom.get(Proximity.CLOSE) && !text.isBlank();
    }

    boolean isBackgroundVisisble() {
        return displayImage == null || pose.distance.isEmpty() ||
                displayImageOffset.getX() != 0.0 || displayImageOffset.getY() != 0.0 || sceneBlend < 1.0f
                || actorZoom < 1.0;
    }

}
