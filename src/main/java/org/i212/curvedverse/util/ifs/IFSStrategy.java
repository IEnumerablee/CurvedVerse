package org.i212.curvedverse.util.ifs;

import org.i212.curvedverse.util.ComplexNumber;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public abstract class IFSStrategy {
    private static final Map<String, IFSStrategy> registry = new ConcurrentHashMap<>();
    public static final int ATTRACTOR_MAX_DEPTH = 6;
    public static final double ATTRACTOR_MIN_DISTANCE = 0.3;

    private final List<Transformation> transformations;
    private List<ComplexNumber> attractors = Collections.emptyList();

    public IFSStrategy() {
        this.transformations = defineTransformations();
    }

    public void bake() {
        this.attractors = calculateAttractors();
    }

    protected abstract List<Transformation> defineTransformations();
    public abstract String getName();

    public List<Transformation> getTransformations() {
        return Collections.unmodifiableList(transformations);
    }

    public List<ComplexNumber> getAttractors() {
        return Collections.unmodifiableList(attractors);
    }

    public List<TransitionPoint> getNeighbors(ComplexNumber currentPoint, int currentDepth) {
        Set<TransitionPoint> neighbors = new HashSet<>();

        for (Transformation t : transformations) {
            ComplexNumber next = t.apply(currentPoint);
            neighbors.add(createTransitionPoint(next, currentDepth + 1));
        }

        for (Transformation t : transformations) {
            try {
                ComplexNumber prev = t.applyInverse(currentPoint);
                neighbors.add(createTransitionPoint(prev, currentDepth - 1));
            } catch (IllegalStateException e) {
            }
        }
        return new ArrayList<>(neighbors);
    }

    private TransitionPoint createTransitionPoint(ComplexNumber point, int depth) {
        double phase = Math.atan2(point.getImaginary(), point.getReal());
        double minDistance = attractors.stream()
                .mapToDouble(a -> a.subtract(point).abs())
                .min()
                .orElse(Double.POSITIVE_INFINITY);
        return new TransitionPoint(point.getReal(), point.getImaginary(), depth, phase, minDistance);
    }

    protected List<ComplexNumber> calculateAttractors() {
        List<ComplexNumber> calculatedAttractors = new ArrayList<>();
        Queue<CompositeTransformation> queue = new LinkedList<>();

        for (Transformation t : transformations) {
            queue.add(new CompositeTransformation(t, 1));
        }

        while (!queue.isEmpty()) {
            CompositeTransformation current = queue.poll();
            if (current.depth > ATTRACTOR_MAX_DEPTH) {
                continue;
            }

            Optional<ComplexNumber> fixedPointOpt = current.transformation.getFixedPoint();
            if (fixedPointOpt.isPresent()) {
                ComplexNumber fixedPoint = fixedPointOpt.get();
                if (isFarFromOthers(fixedPoint, calculatedAttractors)) {
                    calculatedAttractors.add(fixedPoint);
                }
            }

            if (current.depth < ATTRACTOR_MAX_DEPTH) {
                for (Transformation t : transformations) {
                    queue.add(new CompositeTransformation(
                            Transformation.compose(current.transformation, t),
                            current.depth + 1
                    ));
                }
            }
        }
        return calculatedAttractors;
    }

    private boolean isFarFromOthers(ComplexNumber point, List<ComplexNumber> others) {
        return others.stream().allMatch(p -> p.subtract(point).abs() >= ATTRACTOR_MIN_DISTANCE);
    }

    private static class CompositeTransformation {
        final Transformation transformation;
        final int depth;

        CompositeTransformation(Transformation transformation, int depth) {
            this.transformation = transformation;
            this.depth = depth;
        }
    }

    public static void register(String name, IFSStrategy strategy) {
        registry.put(name, strategy);
    }

    public static Optional<IFSStrategy> get(String name) {
        return Optional.ofNullable(registry.get(name));
    }

    public static Set<String> getAvailableStrategies() {
        return Collections.unmodifiableSet(registry.keySet());
    }
}




