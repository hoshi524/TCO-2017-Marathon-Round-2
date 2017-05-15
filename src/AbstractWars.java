import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AbstractWars {

    XorShift random = new XorShift();
    Base[] bases;
    List<Troops> troops;
    int speed, turn;
    int[][] sendTurn;

    public int init(int[] baseLocations, int speed_) {
        bases = new Base[baseLocations.length / 2];
        for (int i = 0; i < bases.length; ++i) {
            Base b = new Base();
            b.id = i;
            b.x = baseLocations[2 * i];
            b.y = baseLocations[2 * i + 1];
            bases[i] = b;
        }
        troops = new ArrayList<>();
        turn = 0;
        speed = speed_;
        sendTurn = new int[bases.length][bases.length];
        for (int i = 0; i < bases.length; ++i) {
            for (int j = i + 1; j < bases.length; ++j) {
                sendTurn[i][j] = sendTurn[j][i] = (int) Math.ceil(distance(bases[i], bases[j]) / speed);
            }
        }
        return 0;
    }

    int[] sendTroops(int[] bases_, int[] troops_) {
        for (int i = 0; i < this.bases.length; ++i) {
            if (turn == 1) {
                bases[i].growth = bases_[2 * i + 1] - bases[i].troops;
                if (bases[i].growth < 1 || 3 < bases[i].growth) throw new RuntimeException();
            }
            bases[i].owner = bases_[2 * i];
            bases[i].troops = bases_[2 * i + 1];
        }
        ++turn;
        List<Base> aly = Stream.of(bases).filter(x -> x.owner == 0).collect(Collectors.toList());
        List<Base> opp = Stream.of(bases).filter(x -> x.owner != 0).collect(Collectors.toList());
        if (turn < 2 || opp.size() == 0) return new int[0];

        List<Integer> ret = new ArrayList<>();
        for (Base b : aly) {
            if (b.troops >= 900) {
                Base t = opp.stream().sorted((x, y) -> sendTurn[b.id][x.id] - sendTurn[b.id][y.id]).findFirst().get();
                Base c = Stream.of(bases).filter(x -> b.id != x.id && sendTurn[b.id][t.id] * 6 > (sendTurn[b.id][x.id] + sendTurn[x.id][t.id]) * 5).sorted((x, y) -> sendTurn[b.id][x.id] - sendTurn[b.id][y.id]).findFirst().orElse(t);
                ret.add(b.id);
                ret.add(c.id);
            }
        }
        return to(ret);
    }

    class Base {
        int id, x, y, owner, troops, growth;
    }

    class Troops {
        int owner, size, x, y, to, time;
    }

    double distance(Base a, Base b) {
        int x = a.x - b.x;
        int y = a.y - b.y;
        return Math.sqrt(x * x + y * y);
    }

    int[] to(List<Integer> list) {
        int[] x = new int[list.size()];
        for (int i = 0; i < x.length; ++i) {
            x[i] = list.get(i);
        }
        return x;
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
