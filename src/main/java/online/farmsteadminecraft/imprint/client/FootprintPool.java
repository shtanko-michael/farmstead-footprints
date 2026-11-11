package online.farmsteadminecraft.imprint.client;

import java.util.function.Consumer;

/**
 * Fixed-size ring buffer of footprints. When full, the oldest print is overwritten,
 * so memory and per-frame cost stay bounded no matter how many entities walk around.
 */
public final class FootprintPool {
    private Footprint[] prints;
    private int next;
    private int alive;

    public FootprintPool(int capacity) {
        resize(capacity);
    }

    public void resize(int capacity) {
        capacity = Math.max(16, capacity);
        if (prints != null && prints.length == capacity) return;
        prints = new Footprint[capacity];
        for (int i = 0; i < prints.length; i++) prints[i] = new Footprint();
        next = 0;
        alive = 0;
    }

    public Footprint add() {
        Footprint p = prints[next];
        if (!p.alive) alive++;
        p.alive = true;
        next = (next + 1) % prints.length;
        return p;
    }

    /** Retires expired prints. Call once per client tick. */
    public void tick(long nowTick) {
        int count = 0;
        for (Footprint p : prints) {
            if (!p.alive) continue;
            if (nowTick - p.bornTick >= p.lifeTicks) {
                p.alive = false;
            } else {
                count++;
            }
        }
        alive = count;
    }

    public void forEachAlive(Consumer<Footprint> action) {
        for (Footprint p : prints) {
            if (p.alive) action.accept(p);
        }
    }

    public void clear() {
        for (Footprint p : prints) p.alive = false;
        alive = 0;
    }

    public int alive() {
        return alive;
    }

    public int capacity() {
        return prints.length;
    }
}
