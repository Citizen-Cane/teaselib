package teaselib.core;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation.Gaze;

class PersistedPoseEstimation {
    private final Properties persisted = new Properties();

    PersistedPoseEstimation(HumanPose.Estimation estimation) {
        estimation.distance.ifPresent(distance -> {
            persisted.put("distance", "" + distance);
        });
        estimation.head.ifPresent(head -> {
            persisted.put("head", head.getX() + " " + head.getY());
        });
        estimation.gaze.ifPresent(gaze -> {
            persisted.put("gaze", gaze.nod + " " + gaze.shake + " " + gaze.tilt);
        });
    }

    PersistedPoseEstimation(InputStream stream) throws IOException {
        persisted.load(stream);
    }

    HumanPose.Estimation toPose() {
        Optional<Float> distance = restoreFloat(persisted.getProperty("distance"));
        Optional<Point2D.Float> head = restorePoint2D(persisted.getProperty("head"));
        Optional<Gaze> gaze = restoreGaze(persisted.getProperty("gaze"));

        if (distance.isPresent()) {
            if (head.isPresent()) {
                if (gaze.isPresent()) {
                    return new HumanPose.Estimation(distance.get(), head.get(), gaze.get());
                } else {
                    return new HumanPose.Estimation(distance.get(), head.get());
                }
            } else {
                return new HumanPose.Estimation(distance.get());
            }
        } else {
            return HumanPose.Estimation.NONE;
        }
    }

    private static Optional<Float> restoreFloat(String value) {
        return value == null ? Optional.empty() : Optional.of(Float.parseFloat(value));
    }

    private static Optional<Point2D.Float> restorePoint2D(String value) {
        if (value == null) {
            return Optional.empty();
        } else {
            String[] c = value.split(" ");
            return Optional.of(new Point2D.Float(Float.parseFloat(c[0]), Float.parseFloat(c[1])));
        }
    }

    private static Optional<Gaze> restoreGaze(String value) {
        if (value == null) {
            return Optional.empty();
        } else {
            String[] c = value.split(" ");
            return Optional.of(new Gaze(Float.parseFloat(c[0]), Float.parseFloat(c[1]), Float.parseFloat(c[2])));
        }
    }

    public void store(OutputStream out) throws IOException {
        persisted.store(out, "Pose estimation");
    }
}