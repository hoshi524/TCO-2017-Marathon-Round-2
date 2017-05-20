import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {

    private static final int MAX_TURN = 2005;
    private int[][] arrivalTroops;
    private Base[][] bases;
    private List<Troops> troops;
    private int speed, turn;
    private int[][] sendTurn;

    public int init(int[] baseLocations, int speed_) {
        int bs = baseLocations.length / 2;
        bases = new Base[MAX_TURN][bs];
        BaseBase[] bb = new BaseBase[bs];
        for (int i = 0; i < bs; ++i) {
            bb[i] = new BaseBase();
            bb[i].id = i;
            bb[i].x = baseLocations[2 * i];
            bb[i].y = baseLocations[2 * i + 1];
        }
        for (int i = 0; i < bases.length; ++i) {
            for (int j = 0; j < bases[i].length; ++j) {
                bases[i][j] = new Base();
                bases[i][j].base = bb[j];
            }
        }
        turn = 0;
        speed = speed_;
        sendTurn = new int[bs][bs];
        for (int i = 0; i < bs; ++i) {
            for (int j = i + 1; j < bs; ++j) {
                sendTurn[i][j] = sendTurn[j][i] = (int) Math.ceil(distance(bb[i].x, bb[i].y, bb[j].x, bb[j].y) / speed);
            }
        }
        arrivalTroops = new int[bs][MAX_TURN];
        troops = new ArrayList<>();
        return 0;
    }

    int[] sendTroops(int[] bases_, int[] troops_) {
        Base[] bases = this.bases[turn];
        for (int i = 0; i < bases.length; ++i) {
            bases[i].owner = bases_[2 * i];
            bases[i].troops = bases_[2 * i + 1];
            if (turn == 1) bases[i].base.growth = bases[i].troops - this.bases[turn - 1][i].troops;
        }
        List<Base> aly = Stream.of(bases).filter(x -> x.owner == 0 && x.troops > 1).collect(Collectors.toList());
        List<Base> opp = Stream.of(bases).filter(x -> x.owner != 0 || x.troops == 0).collect(Collectors.toList());
        int players = (int) Stream.of(bases).mapToInt(x -> x.owner).distinct().count();
        ++turn;
        if (turn < 2 || opp.size() == 0) return new int[0];

        for (Base b : aly) {
            b.nextTroops = b.troops + b.base.growth + b.troops / 100 + arrivalTroops[b.base.id][turn + 1];
        }
        for (Base b : opp) {
            b.reverse = Integer.MAX_VALUE;
            int troops = b.troops;
            for (int t = turn + 1, ts = Math.min(t + 100, MAX_TURN); t < ts; ++t) {
                troops += b.base.growth + troops / 100;
                troops -= arrivalTroops[b.base.id][t];
                if (troops < 0) {
                    b.reverse = t - turn;
                    break;
                }
            }
        }

        List<Integer> ret = new ArrayList<>();

        boolean used[] = new boolean[bases.length];
        while (true) {
            Base t = null;
            int value = Integer.MAX_VALUE;
            List<Base> va = aly.stream().filter(a -> a.troops < 100 && used[a.base.id] == false).collect(Collectors.toList());
            for (Base x : opp) {
                int s = 0;
                for (Base a : va.stream().filter(a -> sendTurn[a.base.id][x.base.id] < x.reverse).sorted(compare(x)).collect(Collectors.toList())) {
                    if (sendTurn[x.base.id][a.base.id] > 20 + 10 * (5 - players)) break;
                    s += a.base.growth;
                    if (x.base.growth + x.troops / 100 < s) {
                        if (value > sendTurn[x.base.id][a.base.id]) {
                            value = sendTurn[x.base.id][a.base.id];
                            t = x;
                        }
                        break;
                    }
                }
            }
            if (t == null) break;
            Base x = t;
            int s = 0;
            for (Base a : va.stream().filter(a -> sendTurn[a.base.id][x.base.id] < x.reverse).sorted(compare(x)).collect(Collectors.toList())) {
                ret.add(a.base.id);
                ret.add(x.base.id);
                int arrival = turn + sendTurn[a.base.id][t.base.id];
                if (arrival < MAX_TURN) arrivalTroops[t.base.id][arrival] += Math.ceil((a.troops / 2) / 1.20);
                used[a.base.id] = true;
                s += a.base.growth;
                if (x.base.growth + x.troops / 100 < s) {
                    break;
                }
            }
        }

        aly.stream().filter(x -> used[x.base.id] == false && x.nextTroops > 1000).forEach(a -> {
            Stream.of(bases).filter(x -> sendTurn[a.base.id][x.base.id] < x.reverse || (a != x && x.owner == 0 && x.troops < 300)).min(compare(a)).ifPresent(t -> {
                ret.add(a.base.id);
                ret.add(t.base.id);
                int arrival = turn + sendTurn[a.base.id][t.base.id];
                if (arrival < MAX_TURN) arrivalTroops[t.base.id][arrival] += Math.ceil((a.troops / 2) / 1.20);
            });
        });
        return to(ret);
    }

    Comparator<Base> compare(Base x) {
        return (n, m) -> sendTurn[x.base.id][n.base.id] - sendTurn[x.base.id][m.base.id];
    }

    class BaseBase {
        int id, x, y, growth;
    }

    class Base {
        BaseBase base;
        int owner, troops, nextTroops, reverse;
    }

    class Troops {
        int owner, size, x, y, from, in, to, time = Integer.MAX_VALUE;
    }

    double distance(int a, int b, int c, int d) {
        int x = a - c;
        int y = b - d;
        return Math.sqrt(x * x + y * y);
    }

    int[] to(List<Integer> list) {
        int[] x = new int[list.size()];
        for (int i = 0; i < x.length; ++i) {
            x[i] = list.get(i);
        }
        return x;
    }

    private void debug(Object... o) {
        System.err.println(Arrays.deepToString(o));
    }
}
