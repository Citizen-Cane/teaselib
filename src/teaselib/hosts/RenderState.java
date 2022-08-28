package teaselib.hosts;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.ProximitySensor;
import teaselib.util.AnnotatedImage;
import teaselib.util.AnnotatedImage.Annotation;

/**
 * @author Citizen-Cane
 *
 */
class RenderState {

    // Scene properties
    String displayImageResource;
    AbstractValidatedImage<?> displayImage;
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
    AbstractValidatedImage<?> textImage;
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
        repaintSceneImage = actorZoom != oldState.actorZoom ||
                sceneBlend != oldState.sceneBlend ||
                !Objects.equals(displayImageResource, oldState.displayImageResource) ||
                !Objects.equals(displayImageOffset, oldState.displayImageOffset);
        // TODO Explicit update necessary if texts are the same - for instance with random.item(text)
        repaintTextImage = // oldState.repaintTextImage ||
                isIntertitle != oldState.isIntertitle ||
                        !Objects.equals(text, oldState.text);
    }

    boolean isBackgroundVisisble() {
        return displayImage == null
                || pose.distance.isEmpty()
                || displayImageOffset.getX() != 0.0
                || displayImageOffset.getY() != 0.0
                || sceneBlend < 1.0f
                || actorZoom < 1.0;
    }

    Optional<Rectangle2D> focusRegion() {
        Optional<Rectangle2D> focusRegion;
        if (annotations.contains(Annotation.Person.Actor)) {
            focusRegion = focusRegion(pose, actorZoom);
            if (focusRegion.isEmpty()) {
                focusRegion = Optional.of(new Rectangle2D.Double(0.4, 0.4, 0.2, 0.2));
            }
        } else if (annotations.contains(Annotation.Person.Actor)) {
            // TODO images with non-actor models may be focused on as well,
            // probably only the whole body -> pose.bounds
            focusRegion = Optional.empty();
        } else {
            focusRegion = Optional.empty();
        }
        return focusRegion;
    }

    private static Optional<Rectangle2D> focusRegion(HumanPose.Estimation pose, double actorZoom) {
        if (actorZoom > ProximitySensor.zoom.get(Proximity.FACE2FACE)) {
            // TODO set focus area from proximity sensor to choose region depending on player state & position
            return pose.face(); // should be head -> boobs, or shoes -> shoes, etc.
        } else if (actorZoom > ProximitySensor.zoom.get(Proximity.AWAY)) {
            return pose.face();
        } else {
            return pose.face();
        }
    }

}
