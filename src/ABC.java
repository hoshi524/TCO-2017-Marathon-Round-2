import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ABC {

    private static final int MAX_TURN = 2005;
    private int B;
    private int[][] arrival;
    private int[] arrivalAll;
    private Base[][] bases;
    private List<Troops> troops;
    private int speed, turn;
    private int[][] sendTurn;
    private XorShift random = new XorShift();
    private Plan.State plan;

    public int init(int[] baseLocations, int speed_) {
        plan = null;
        B = baseLocations.length / 2;
        bases = new Base[MAX_TURN][B];
        BaseBase[] bb = new BaseBase[B];
        for (int i = 0; i < B; ++i) {
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
        sendTurn = new int[B][B];
        for (int i = 0; i < B; ++i) {
            for (int j = i + 1; j < B; ++j) {
                sendTurn[i][j] = sendTurn[j][i] = (int) Math.ceil(distance(bb[i].x, bb[i].y, bb[j].x, bb[j].y) / speed);
            }
        }
        arrival = new int[B][MAX_TURN];
        arrivalAll = new int[B];
        troops = new ArrayList<>();
        return 0;
    }

    int[] sendTroops(int[] bases_, int[] troops_) {
        Base[] bases = this.bases[++turn];
        for (int i = 0; i < bases.length; ++i) {
            bases[i].owner = bases_[2 * i];
            bases[i].troops = bases_[2 * i + 1];
            if (turn == 2) bases[i].base.growth = bases[i].troops - this.bases[turn - 1][i].troops;
            arrivalAll[i] -= arrival[i][turn];
        }
        if (turn < 2) return new int[0];
        {
            troops.stream().forEach(x -> x.to = -2);
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

        List<Base> aly = Stream.of(bases).filter(x -> x.owner == 0 && x.troops >= 2).collect(Collectors.toList());
        List<Base> opp = Stream.of(bases).filter(x -> x.owner != 0 || x.troops == 0).collect(Collectors.toList());
        int players = (int) Stream.of(bases).mapToInt(x -> x.owner).distinct().count();

        aly.stream().forEach(x -> x.nextTroops = x.troops + x.base.growth + x.troops / 100 + arrival[x.base.id][turn + 1]);
        for (Base b : opp) {
            b.reverse = Integer.MAX_VALUE;
            int troops = b.troops;
            for (int t = turn + 1, ts = Math.min(t + 100, MAX_TURN); t < ts; ++t) {
                troops += b.base.growth + troops / 100;
                troops -= arrival[b.base.id][t];
                if (troops < 0) {
                    b.reverse = t - turn;
                    break;
                }
            }
        }

        boolean used[] = new boolean[bases.length];
        class Result {
            List<Integer> res = new ArrayList<>();

            void sendTroops(Base a, Base b) {
                res.add(a.base.id);
                res.add(b.base.id);
                int v = turn + sendTurn[a.base.id][b.base.id];
                int h = a.troops / 2;
                int troops = (int) Math.ceil(h / 1.20);
                if (v < MAX_TURN) arrival[b.base.id][v] += troops;
                arrivalAll[b.base.id] += troops;
                used[a.base.id] = true;
            }

            int[] to() {
                int[] x = new int[res.size()];
                for (int i = 0; i < x.length; ++i) {
                    x[i] = res.get(i);
                }
                return x;
            }
        }
        Result result = new Result();
        if (plan == null) {
            plan = new Plan(bases).HC();
            debug(plan.turn, plan.score, IntStream.of(plan.ok).filter(x -> x == 0).count(), plan.ok);
        }
        for (int i = 0; i < B; ++i) {
            if (plan.ok[i] > 0 && plan.ok[i] != Integer.MAX_VALUE && plan.ok[i] <= turn && (bases[i].owner != 0 || bases[i].troops == 0)) {
                debug(turn, i, bases[i].owner, bases[i].troops, plan.ok[i]);
                throw new RuntimeException();
            }
        }
        if (plan.turn > turn) {
            if (false) {
                int x = 10;
                debug(turn, x, bases[x].owner, bases[x].troops, bases[x].base.growth, arrival[x][turn], arrival[x][turn + 1], arrival[x][turn + 2], arrival[x][turn + 3]);
            }
            for (int i = 0; i < B; ++i) {
                if (bases[i].owner == 0 && bases[i].troops > 1) {
                    for (int j : plan.target[i]) {
                        if (plan.ok[j] == Integer.MAX_VALUE) break;
                        if (plan.ok[j] >= turn + sendTurn[i][j] && i != j) {
                            if (j == 88)
                                debug(turn, "from", i, "to", j, plan.ok[j], "attack", bases[i].troops / 2, "troops", bases[j].troops, "sendTurn", sendTurn[i][j], "growth", bases[j].base.growth);
                            result.sendTroops(bases[i], bases[j]);
                            break;
                        }
                    }
                }
            }
            return result.to();
        }
        if (true) return result.to();

        while (true) {
            Base t = null;
            int value = Integer.MAX_VALUE;
            List<Base> v = aly.stream().filter(a -> a.troops < 100 && used[a.base.id] == false).collect(Collectors.toList());
            for (Base x : opp.stream().filter(x -> x.troops < 200).collect(Collectors.toList())) {
                int s = 0;
                for (Base a : v.stream().filter(a -> sendTurn[a.base.id][x.base.id] < x.reverse).sorted(compare(x)).collect(Collectors.toList())) {
                    if (sendTurn[x.base.id][a.base.id] > 20 + 10 * (5 - players)) break;
                    s += a.base.growth;
                    if (x.troops == 0 || x.base.growth + x.troops / 100 < s) {
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
            for (Base a : v.stream().filter(a -> sendTurn[a.base.id][x.base.id] < x.reverse).sorted(compare(x)).collect(Collectors.toList())) {
                result.sendTroops(a, x);
                s += a.base.growth;
                if (x.troops == 0 || x.base.growth + x.troops / 100 < s) {
                    break;
                }
            }
        }

        int[] attackTime = new int[bases.length];
        Arrays.fill(attackTime, Integer.MAX_VALUE);
        for (Troops t : troops) {
            if (t.to > -1 && attackTime[bases[t.to].base.id] > t.time) attackTime[bases[t.to].base.id] = t.time;
        }
        aly.stream().filter(x -> used[x.base.id] == false && x.nextTroops > 1000 && (attackTime[x.base.id] > turn + 15 || x.nextTroops > 1100)).forEach(a -> {
            Stream.of(bases).filter(x -> sendTurn[a.base.id][x.base.id] < x.reverse || (x.owner == 0 && x.troops + arrivalAll[x.base.id] < 300)).min(compare(a)).ifPresent(x -> {
                result.sendTroops(a, x);
            });
        });
        return result.to();
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

    class Plan {
        private final static int T = 200;
        Base[] bases;

        class State {
            int score, turn, target[][] = new int[B][B], ok[] = new int[B];

            State init() {
                turn = 0;
                score = Integer.MIN_VALUE;
                for (int i = 0; i < B; ++i) {
                    for (int j = 0; j < B; ++j) {
                        target[i][j] = j;
                    }
                    int t = i;
                    target[t] = sort(target[t], (a, b) -> sendTurn[t][a] - sendTurn[t][b]);
                }
                return this;
            }

            State shuffle() {
                for (int i = 0; i < 10; ++i) {
                    int a = random.nextInt(B);
                    int b = random.nextInt(B);
                    int c = random.nextInt(B);
                    int t = target[a][b];
                    target[a][b] = target[a][c];
                    target[a][c] = t;
                }
                return this;
            }

            State copy() {
                State x = new State();
                x.score = score;
                x.turn = turn;
                for (int i = 0; i < B; ++i) {
                    System.arraycopy(target[i], 0, x.target[i], 0, B);
                }
                System.arraycopy(ok, 0, x.ok, 0, B);
                return x;
            }

            State simulate() {
                Arrays.fill(ok, -1);
                int troop[] = new int[B];
                int growth[] = new int[B];
                int arrival[][] = new int[B][T + 1000];
                int b = 0;
                for (int i = 0; i < B; ++i) {
                    if (bases[i].owner == 0) {
                        troop[i] = +bases[i].troops;
                        ++b;
                    } else {
                        troop[i] = -bases[i].troops;
                        ok[i] = Integer.MAX_VALUE;
                    }
                    growth[i] = bases[i].base.growth;
                }
                turn = 2;
                score = 0;
                while (b < B && turn < T) {
                    for (int i = 0; i < B; ++i) {
                        if (troop[i] >= 2) {
                            int j = 0;
                            while (j + 1 < B && turn + sendTurn[i][target[i][j]] >= ok[target[i][j]]) ++j;
                            int x = target[i][j];
                            arrival[x][turn + sendTurn[i][x]] += Math.ceil((troop[i] / 2) / 1.2);
                            troop[i] -= troop[i] / 2;
                            ++score;
                        }
                    }
                    for (int i = 0; i < B; ++i) {
                        int prev = troop[i];
                        if (troop[i] > 0) {
                            troop[i] += growth[i];
                        } else {
                            troop[i] -= growth[i];
                        }
                        troop[i] += troop[i] / 100 + arrival[i][turn];
                        if (prev < 0 && troop[i] >= 0) ++b;
                        if (ok[i] != -1 && troop[i] < 1) {
                            int x = troop[i];
                            for (int j = turn + 1; j < T; ++j) {
                                x -= growth[i];
                                x += x / 100 + arrival[i][j];
                                if (x > 1) {
                                    if (ok[i] > j) ok[i] = j;
                                    break;
                                }
                            }
                        }
                    }
                    ++turn;
                }
                score += B * (T - turn);
                return this;
            }
        }

        Plan(Base[] bases) {
            this.bases = bases;
            {
                int[] buf = new int[0xff];
                for (int i = 0; i < buf.length; ++i) {
                    buf[i] = 0xff - i;
                }
                buf = sort(buf, (a, b) -> a - b);
                for (int i = 0; i + 1 < buf.length; ++i) {
                    if (buf[i] > buf[i + 1]) {
                        debug(buf);
                        throw new RuntimeException();
                    }
                }
            }
        }

        State HC() {
            State best = new State().init();
            for (int i = 0; i < 0xff; ++i) {
                State tmp = best.copy().shuffle().simulate();
                if (best.score <= tmp.score) best = tmp;
            }
            return best;
        }
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
            return w = (w ^ (w >>> 19)) ^ (t ^ (t >>> 8));
        }

        long nextLong() {
            return ((long) nextInt() << 32) | (long) nextInt();
        }
    }

    int[] sort(int x[], Comparator<Integer> comparator) {
        int l[] = new int[x.length], li = 0;
        int r[] = new int[x.length], ri = 0;
        int a[] = new int[x.length];
        int p = x[0];
        for (int i = 1; i < x.length; ++i) {
            if (comparator.compare(p, x[i]) > 0) {
                l[li++] = x[i];
            } else {
                r[ri++] = x[i];
            }
        }
        a[li] = p;
        if (li > 0) System.arraycopy(sort(Arrays.copyOf(l, li), comparator), 0, a, 0, li);
        if (ri > 0) System.arraycopy(sort(Arrays.copyOf(r, ri), comparator), 0, a, li + 1, ri);
        return a;
    }

    private void debug(Object... o) {
        System.err.println(Arrays.deepToString(o));
    }
}
