package teaselib.host;

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
public class RenderState {

    // Scene properties
    public String displayImageResource;
    public AbstractValidatedImage<?> displayImage;
    public HumanPose.Estimation pose;
    public Set<AnnotatedImage.Annotation> annotations;
    public double actorZoom;
    public Point2D displayImageOffset;

    public String text;
    public boolean isIntertitle;

    public float focusLevel;

    //
    // Composition stack

    public boolean repaintSceneImage;
    public AffineTransform transform;
    public float sceneBlend;

    public boolean repaintTextImage;
    public AbstractValidatedImage<?> textImage;
    public Rectangle textImageRegion;
    public float textBlend;

    public RenderState() {
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

    public RenderState copy() {
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
        repaintTextImage = // oldState.repaintTextImage ||
                isIntertitle != oldState.isIntertitle ||
                // Demand text string to be the same,
                // in order to display existing text when just changing the image
                // -> single paragraph when showing prompt with new image
                        text != oldState.text;
        // TODO Re-activate oldState.repaintTextImage for such purposes
        // !Objects.equals(text, oldState.text);
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
