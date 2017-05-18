import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractWars {

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
            if (turn == 1) {
                bases[i].base.growth = bases[i].troops - this.bases[turn - 1][i].troops;
                if (bases[i].base.growth < 1 || 3 < bases[i].base.growth) throw new RuntimeException();
            }
        }
        {
            for (Troops z : troops) {
                z.to = -2;
            }
            boolean used[] = new boolean[bases.length];
            for (int i = 0; i < troops_.length / 4; ++i) {
                int owner = troops_[i * 4 + 0];
                int size = troops_[i * 4 + 1];
                int x = troops_[i * 4 + 2];
                int y = troops_[i * 4 + 3];
                if (owner == 0) continue;
                int count = 0;
                int id = 0;
                for (int j = 0; j < bases.length; ++j) {
                    Base b = this.bases[turn - 1][j];
                    if (used[j]) continue;
                    if (b.owner == owner && b.troops / 2 == size && distance(x, y, b.base.x, b.base.y) <= speed) {
                        ++count;
                        id = j;
                    }
                }
                Troops t = null;
                if (count == 1) {
                    t = new Troops();
                    t.owner = owner;
                    t.size = size;
                    t.x = x;
                    t.y = y;
                    t.in = turn - 1;
                    t.from = id;
                    used[id] = true;
                    troops.add(t);
                } else {
                    count = 0;
                    for (Troops z : troops) {
                        if (z.owner == owner && z.size == size && distance(x, y, z.x, z.y) <= speed + 1) {
                            ++count;
                            t = z;
                        }
                    }
                    if (count != 1) continue;
                    t.x = x;
                    t.y = y;
                }
                t.to = -1;
                count = 0;
                id = 0;
                for (int j = 0; j < bases.length; ++j) {
                    Base b = this.bases[t.in][j];
                    double part = (double) (turn - t.in) / sendTurn[t.from][j];
                    if (b.owner != t.owner
                            && t.x == (int) (bases[t.from].base.x + (b.base.x - bases[t.from].base.x) * part)
                            && t.y == (int) (bases[t.from].base.y + (b.base.y - bases[t.from].base.y) * part)
                            ) {
                        ++count;
                        id = j;
                    }
                }
                if (count != 1) continue;
                t.to = id;
                t.time = t.in + sendTurn[t.from][t.to];
            }
            troops = troops.stream().filter(x -> x.to != -2).collect(Collectors.toList());
        }
        List<Base> aly = Stream.of(bases).filter(x -> x.owner == 0 && x.troops > 0).collect(Collectors.toList());
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
                    b.reverse = t;
                    break;
                }
            }
        }

        List<Integer> ret = new ArrayList<>();
        aly.stream().filter(x -> x.troops > 1).forEach(b -> {
            opp.stream().filter(x -> turn + sendTurn[b.base.id][x.base.id] < x.reverse).min((x, y) -> sendTurn[b.base.id][x.base.id] - sendTurn[b.base.id][y.base.id]).ifPresent(t -> {
                if (sendTurn[b.base.id][t.base.id] > 10 * (6 - players) && b.nextTroops < 1000) return;
                int arrival = turn + sendTurn[b.base.id][t.base.id];
                ret.add(b.base.id);
                ret.add(t.base.id);
                if (arrival < MAX_TURN) arrivalTroops[t.base.id][arrival] += Math.ceil((b.troops / 2) / 1.20);
            });
        });
        return to(ret);
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
