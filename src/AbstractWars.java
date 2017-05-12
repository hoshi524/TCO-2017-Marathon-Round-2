import java.io.*;
import java.lang.reflect.Array;
import java.security.*;
import java.util.*;
import java.util.List;

public class AbstractWars {

    XorShift random = new XorShift();
    Base[] bases;
    int speed, turn = 0;

    public int init(int[] baseLocations, int speed) {
        bases = new Base[baseLocations.length / 2];
        for (int i = 0; i < bases.length; ++i) {
            Base b = new Base();
            b.x = baseLocations[2 * i];
            b.y = baseLocations[2 * i + 1];
            bases[i] = b;
        }
        this.speed = speed;
        return 0;
    }

    List<Integer> others;

    public int getRandomBase(int sourceInd) {
        double[] probs = new double[others.size()];
        double sp = 0;
        for (int i = 0; i < others.size(); ++i) {
            probs[i] = 1 / distance(bases[sourceInd], bases[others.get(i)]);
            sp += probs[i];
        }

        double r = random.nextDouble() * sp;
        double s = 0;
        for (int i = 0; i < others.size(); ++i) {
            s += probs[i];
            if (s >= r)
                return others.get(i);
        }
        return others.get(others.size() - 1);
    }

    int[] sendTroops(int[] bases, int[] troops) {
        ++turn;
        others = new ArrayList<>();
        for (int i = 0; i < this.bases.length; ++i)
            if (bases[2 * i] != 0) others.add(i);
        if (others.size() == 0) return new int[0];

        int ret[] = new int[this.bases.length * 2], rets = 0;
        for (int i = 0; i < this.bases.length; ++i) {
            if (bases[2 * i] == 0 && bases[2 * i + 1] > 1000 * 2 / 3) {
                ret[rets++] = i;
                ret[rets++] = getRandomBase(i);
            }
        }
        return Arrays.copyOf(ret, rets);
    }

    class Base {
        int x, y;
        int growth = 1;
    }

    double distance(Base a, Base b) {
        int x = a.x - b.x;
        int y = a.y - b.y;
        return x * x + y * y;
    }

    private final class XorShift {
        int x = 123456789;
        int y = 362436069;
        int z = 521288629;
        int w = 88675123;

        int nextInt(int n) {
            final int t = x ^ (x << 11);
            x = y;
            y = z;
            z = w;
            w = (w ^ (w >>> 19)) ^ (t ^ (t >>> 8));
            final int r = w % n;
            return r < 0 ? r + n : r;
        }

        int nextInt() {
            final int t = x ^ (x << 11);
            x = y;
            y = z;
            z = w;
            w = (w ^ (w >>> 19)) ^ (t ^ (t >>> 8));
            return w;
        }

        double nextDouble() {
            int x = nextInt();
            return (double) (x > 0 ? x : x + Integer.MAX_VALUE) / Integer.MAX_VALUE;
        }
    }

    private void debug(Object... o) {
        System.out.println(Arrays.deepToString(o));
    }
}
