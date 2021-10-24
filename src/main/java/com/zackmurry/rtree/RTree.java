package com.zackmurry.rtree;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class RTreeEntry<T> {

    Rectangle bounds;
    T value;

    public RTreeEntry(Rectangle bounds, T value) {
        this.bounds = bounds;
        this.value = value;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public String toString() {
        return "[ (" + bounds.x + ", " + bounds.y + ") to (" + (bounds.x + bounds.width) + ", " + (bounds.y + bounds.height) + ")" + "; value: " + value + " ]";
    }
}

/**
 * R-Tree implementation
 * http://www-db.deis.unibo.it/courses/SI-LS/papers/Gut84.pdf
 * @param <T> Type of value to store
 */
// todo: convert ints to doubles
public class RTree<T> {

    public static final int MIN_ENTRIES = 2;
    public static final int MAX_ENTRIES = 4;

    Rectangle bounds = new Rectangle(0, 0, 0, 0);
    List<RTree<T>> children = new ArrayList<>();
    List<RTreeEntry<T>> entries = new ArrayList<>();
    RTree<T> parent;

    public RTree() {

    }

    public RTree(Rectangle bounds, List<RTree<T>> children, List<RTreeEntry<T>> entries, RTree<T> parent) {
        this.bounds = bounds;
        this.children = children;
        this.entries = entries;
        this.parent = parent;
    }

    public List<T> search(Rectangle area) {
        if (area == null) {
            return new ArrayList<>();
        }
        final List<T> results = new ArrayList<>();
        if (!children.isEmpty()) {
            for (RTree<T> child : children) {
                if (area.intersects(child.bounds)) {
                    results.addAll(child.search(area));
                }
            }
        } else { // Is leaf
            for (RTreeEntry<T> entry : entries) {
                if (area.intersects(entry.bounds)) {
                    results.add(entry.value);
                }
            }
        }
        return results;
    }

    public void insert(Rectangle bounds, T value) {
        RTree<T> l = chooseLeaf(bounds);
        RTree<T> ll = null;
        if (children.isEmpty() && entries.isEmpty()) {
            this.bounds = bounds;
        }
        if (l.entries.size() < MAX_ENTRIES) {
            l.entries.add(new RTreeEntry<>(bounds, value));
            l.bounds = findSmallestBoundingRect(l.bounds, l.entries.get(l.entries.size() - 1).bounds);
        } else {
            l.entries.add(new RTreeEntry<>(bounds, value));
            var result = quadraticSplit(l.entries);
            result.get(0).parent = l;
//            result.get(1).parent = l;
            l = result.get(0);
            ll = result.get(1);
        }
        adjustTree(l, ll);
    }

    private void adjustTree(RTree<T> l, RTree<T> ll) {
        RTree<T> n = l;
        RTree<T> nn = ll;
        if (n.parent != null && !n.parent.children.contains(n)) {
            n.parent.children.add(n);
        }
        while (n.parent != null) {
            RTree<T> p = n.parent;
            p.bounds = findSmallestBoundingRect(p.bounds, n.bounds);

            if (nn != null) {
                if (p.children.size() < MAX_ENTRIES) {
                    p.children.add(nn);
                    p.bounds = findSmallestBoundingRect(p.bounds, nn.bounds);
                    nn.parent = p;
                    n = p;
                    nn = null;
                } else {
                    p.children.add(nn);
                    var result = quadraticSplitChildren(p.children);
                    n = result.get(0);
                    nn = result.get(1);
                }
            } else {
                n = p;
            }
        }
        if (nn != null) {
            if (children.size() >= MAX_ENTRIES) {
                children.clear();
                entries.clear();
                children.add(n);
                children.add(nn);
                bounds = findSmallestBoundingRect(children.get(0).bounds, children.get(1).bounds);
            } else {
                children.add(nn);
                bounds = findSmallestBoundingRect(bounds, children.get(children.size() - 1).bounds);
            }
        }
    }

    // todo: figure out how to combine this and quadraticSplit into one method
    private List<RTree<T>> quadraticSplitChildren(List<RTree<T>> e) {
        int[] seeds = pickSeedsTree(e);
        if (seeds.length < 2) {
            throw new RuntimeException("seeds.length < 2");
        }
        List<RTree<T>> result = new ArrayList<>();
        result.add(new RTree<>());
        result.add(new RTree<>());
        result.get(0).children.add(e.remove(seeds[0]));
        result.get(1).children.add(e.remove(seeds[1] - 1)); // Subtract one to adjust for index decrement from previous removal
        result.get(0).bounds = new Rectangle(result.get(0).children.get(0).bounds.x, result.get(0).children.get(0).bounds.y, result.get(0).children.get(0).bounds.width, result.get(0).children.get(0).bounds.height);
        result.get(1).bounds = new Rectangle(result.get(1).children.get(0).bounds.x, result.get(1).children.get(0).bounds.y, result.get(1).children.get(0).bounds.width, result.get(1).children.get(0).bounds.height);
        while (!e.isEmpty()) {
            if (MIN_ENTRIES == result.get(0).children.size() + e.size()) {
                for (RTree<T> child : e) {
                    result.get(0).children.add(child);
                    result.get(0).bounds = findSmallestBoundingRect(result.get(0).bounds, child.bounds);
                }
                e.clear();
                break;
            }
            if (MIN_ENTRIES == result.get(1).children.size() + e.size()) {
                for (RTree<T> child : e) {
                    result.get(1).children.add(child);
                    result.get(1).bounds = findSmallestBoundingRect(result.get(1).bounds, child.bounds);
                }
                e.clear();
                break;
            }
            int nextIndex = pickNextTree(e, result.get(0).bounds, result.get(1).bounds);
            int d1 = requiredAreaEnlargementToIncludeRect(result.get(0).bounds, e.get(nextIndex).bounds);
            int d2 = requiredAreaEnlargementToIncludeRect(result.get(1).bounds, e.get(nextIndex).bounds);
            if (d1 > d2) {
                result.get(0).children.add(e.remove(nextIndex));
            } else if (d1 < d2) {
                result.get(1).children.add(e.remove(nextIndex));
            } else if (result.get(0).bounds.width * result.get(0).bounds.height < result.get(1).bounds.width * result.get(1).bounds.height) {
                result.get(0).children.add(e.remove(nextIndex));
            } else {
                result.get(1).children.add(e.remove(nextIndex));
            }
            result.get(0).bounds = findSmallestBoundingRect(result.get(0).bounds, result.get(0).children.get(result.get(0).children.size() - 1).bounds);
            result.get(1).bounds = findSmallestBoundingRect(result.get(1).bounds, result.get(1).children.get(result.get(1).children.size() - 1).bounds);
        }
        return result;
    }

    private List<RTree<T>> quadraticSplit(List<RTreeEntry<T>> e) {
        int[] seeds = pickSeeds(e);
        if (seeds.length < 2) {
            throw new RuntimeException("seeds.length < 2");
        }
        List<RTree<T>> result = new ArrayList<>();
        result.add(new RTree<>());
        result.add(new RTree<>());
        result.get(0).entries.add(e.remove(seeds[0]));
        result.get(1).entries.add(e.remove(seeds[1] - 1)); // Subtract one to adjust for the index decrement from the previous removal
        result.get(0).bounds = new Rectangle(result.get(0).entries.get(0).bounds.x, result.get(0).entries.get(0).bounds.y, result.get(0).entries.get(0).bounds.width, result.get(0).entries.get(0).bounds.height);
        result.get(1).bounds = new Rectangle(result.get(1).entries.get(0).bounds.x, result.get(1).entries.get(0).bounds.y, result.get(1).entries.get(0).bounds.width, result.get(1).entries.get(0).bounds.height);
        while (!e.isEmpty()) {
            if (MIN_ENTRIES == result.get(0).entries.size() + e.size()) {
                for (RTreeEntry<T> entry : e) {
                    result.get(0).entries.add(entry);
                    result.get(0).bounds = findSmallestBoundingRect(result.get(0).bounds, entry.bounds);
                }
                e.clear();
                break;
            }
            if (MIN_ENTRIES == result.get(1).entries.size() + e.size()) {
                for (RTreeEntry<T> entry : e) {
                    result.get(1).entries.add(entry);
                    result.get(1).bounds = findSmallestBoundingRect(result.get(1).bounds, entry.bounds);
                }
                e.clear();
                break;
            }
            int nextIndex = pickNext(e, result.get(0).bounds, result.get(1).bounds);
            int d1 = requiredAreaEnlargementToIncludeRect(result.get(0).bounds, e.get(nextIndex).bounds);
            int d2 = requiredAreaEnlargementToIncludeRect(result.get(1).bounds, e.get(nextIndex).bounds);
            if (d1 < d2) {
                result.get(0).entries.add(e.remove(nextIndex));
            } else if (d1 > d2) {
                result.get(1).entries.add(e.remove(nextIndex));
            } else if (result.get(0).bounds.width * result.get(0).bounds.height < result.get(1).bounds.width * result.get(1).bounds.height) {
                result.get(0).entries.add(e.remove(nextIndex));
            } else {
                result.get(1).entries.add(e.remove(nextIndex));
            }
            result.get(0).bounds = findSmallestBoundingRect(result.get(0).bounds, result.get(0).entries.get(result.get(0).entries.size() - 1).bounds);
            result.get(1).bounds = findSmallestBoundingRect(result.get(1).bounds, result.get(1).entries.get(result.get(1).entries.size() - 1).bounds);
        }
        return result;
    }

    public static Rectangle findSmallestBoundingRect(Rectangle a, Rectangle b) {
        int x1 = Math.min(a.x, b.x);
        int y1 = Math.min(a.y, b.y);
        int x2 = Math.max(a.x + a.width, b.x + b.width);
        int y2 = Math.max(a.y + a.height, b.y + b.height);
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    private int pickNextTree(List<RTree<T>> e, Rectangle g1Rect, Rectangle g2Rect) {
        int maxDiff = Integer.MIN_VALUE;
        int maxDiffIndex = -1;
        for (int i = 0; i < e.size(); i++) {
            int d1 = requiredAreaEnlargementToIncludeRect(g1Rect, e.get(i).bounds);
            int d2 = requiredAreaEnlargementToIncludeRect(g2Rect, e.get(i).bounds);
            int dd = Math.abs(d1 - d2);
            if (dd > maxDiff) {
                maxDiff = dd;
                maxDiffIndex = i;
            }
        }
        return maxDiffIndex;
    }
    private int pickNext(List<RTreeEntry<T>> e, Rectangle g1Rect, Rectangle g2Rect) {
        int maxDiff = Integer.MIN_VALUE;
        int maxDiffIndex = -1;
        for (int i = 0; i < e.size(); i++) {
            int d1 = requiredAreaEnlargementToIncludeRect(g1Rect, e.get(i).bounds);
            int d2 = requiredAreaEnlargementToIncludeRect(g2Rect, e.get(i).bounds);
            int dd = Math.abs(d1 - d2);
            if (dd > maxDiff) {
                maxDiff = dd;
                maxDiffIndex = i;
            }
        }
        return maxDiffIndex;
    }

    private int[] pickSeedsTree(List<RTree<T>> e) {
        final int[] result = new int[2];
        int largestD = Integer.MIN_VALUE;
        for (int i = 0; i < e.size() - 1; i++) {
            for (int j = i + 1; j < e.size(); j++) {
                RTree<T> ei = e.get(i);
                RTree<T> ej = e.get(j);
                int rx1 = Math.min(ei.bounds.x, ej.bounds.x);
                int ry1 = Math.min(ei.bounds.y, ej.bounds.y);
                int rx2 = Math.max(ei.bounds.x + ei.bounds.width, ej.bounds.x + ej.bounds.width);
                int ry2 = Math.max(ei.bounds.y + ei.bounds.height, ej.bounds.y + ej.bounds.height);
                int d = ((rx2 - rx1) * (ry2 - ry1)) - (ei.bounds.width * ei.bounds.width);
                if (d > largestD) {
                    largestD = d;
                    result[0] = i;
                    result[1] = j;
                }
            }
        }
        return result;
    }
    private int[] pickSeeds(List<RTreeEntry<T>> e) {
        final int[] result = new int[2];
        int largestD = Integer.MIN_VALUE;
        for (int i = 0; i < e.size() - 1; i++) {
            for (int j = i + 1; j < e.size(); j++) {
                RTreeEntry<T> ei = e.get(i);
                RTreeEntry<T> ej = e.get(j);
                int rx1 = Math.min(ei.bounds.x, ej.bounds.x);
                int ry1 = Math.min(ei.bounds.y, ej.bounds.y);
                int rx2 = Math.max(ei.bounds.x + ei.bounds.width, ej.bounds.x + ej.bounds.width);
                int ry2 = Math.max(ei.bounds.y + ei.bounds.height, ej.bounds.y + ej.bounds.height);
                int d = ((rx2 - rx1) * (ry2 - ry1)) - (ei.bounds.width * ei.bounds.height);
                if (d > largestD) {
                    largestD = d;
                    result[0] = i;
                    result[1] = j;
                }
            }
        }
        return result;
    }

    // todo: needs unit testing
    private RTree<T> chooseLeaf(Rectangle bounds) {
        RTree<T> node = this;
        while (!node.children.isEmpty()) { // While not a leaf
            double leastAreaEnlargement = Double.POSITIVE_INFINITY;
            RTree<T> leastEnlargementChild = null;
            for (RTree<T> child : node.children) {
                double requiredArea = requiredAreaEnlargementToIncludeRect(child.bounds, bounds);
                if (requiredArea < leastAreaEnlargement || (requiredArea == leastAreaEnlargement && child.bounds.getWidth() * child.bounds.getHeight() < leastEnlargementChild.bounds.getWidth() * leastEnlargementChild.bounds.getHeight())) {
                    leastAreaEnlargement = requiredArea;
                    leastEnlargementChild = child;
                }
            }
            node = leastEnlargementChild;
        }
        return node;
    }

    // todo: needs unit testing
    public static int requiredAreaEnlargementToIncludeRect(Rectangle a, Rectangle b) {
        Rectangle boundingRect = findSmallestBoundingRect(a, b);
        return (boundingRect.width * boundingRect.height) - (a.width * a.height);
    }

    @Override
    public String toString() {
        if (bounds == null || children == null || entries == null) {
            return null;
        }
        return "[ (" + bounds.getX() + ", " + bounds.getY() + ") to (" + (bounds.getX() + bounds.getWidth()) + ", " + (bounds.getY() + bounds.getHeight()) + ") " +
                children.stream().map(RTree::toString).collect(Collectors.toList()) + " " + entries.stream().map(RTreeEntry::toString).collect(Collectors.toList()) + "]";
    }

    public int size() {
        int size = 0;
        for (RTree<T> child : children) {
            size += child.size();
        }
        return size + entries.size();
    }

}
