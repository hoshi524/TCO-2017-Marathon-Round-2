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
                int troops = (int) Math.ceil((a.troops / 2) / 1.20);
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
            // debug(plan.score, plan.get);
        }
        if (plan.score > 0) {
            for (int i = 0; i < B; ++i) {
                if (plan.get[i] < turn && (bases[i].owner != 0 || bases[i].troops == 0)) {
                    debug(plan.get[i], i, "owner", bases[i].owner, "troops", bases[i].troops, "growth", bases[i].base.growth, "arrival", arrival[i][turn], arrival[i][turn + 1]);
                    throw new RuntimeException();
                }
                if (bases[i].owner == 0 && bases[i].troops > 1) {
                    for (int j : plan.target) {
                        if (bases[j].owner != 0 && plan.get[j] >= turn + sendTurn[i][j]) {
                            result.sendTroops(bases[i], bases[j]);
                            break;
                        }
                    }
                }
            }
            return result.to();
        }

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
            int score, target[] = new int[B], get[] = new int[B];

            State init() {
                score = Integer.MIN_VALUE;
                for (int i = 0; i < B; ++i) {
                    target[i] = i;
                }
                return this;
            }

            State shuffle() {
                for (int i = 0; i < 10; ++i) {
                    int a = random.nextInt(B);
                    int b = random.nextInt(B);
                    int t = target[a];
                    target[a] = target[b];
                    target[b] = t;
                }
                return this;
            }

            State copy() {
                State x = new State();
                x.score = score;
                System.arraycopy(target, 0, x.target, 0, B);
                System.arraycopy(get, 0, x.get, 0, B);
                return x;
            }

            State simulate() {
                Arrays.fill(get, 0);
                int owner[] = new int[B];
                int growth[] = new int[B];
                for (int i = 0; i < B; ++i) {
                    owner[i] = bases[i].owner;
                    growth[i] = bases[i].base.growth;
                }
                int used[] = new int[B];
                score = 0;
                for (int i = 0; i < B; ++i) {
                    int c = target[i], t = 0;
                    if (owner[c] == 0) continue;
                    int arrival[] = new int[2000];
                    int troop = bases[c].troops;
                    for (; t < T && troop > -1 && troop < 500; ++t) {
                        for (int j = 0; j < B; ++j) {
                            if (owner[j] == 0) {
                                arrival[t + used[j] + sendTurn[c][j]] += growth[j];
                            }
                        }
                        troop += growth[c];
                        troop += troop / 100;
                        troop -= arrival[t];
                    }
                    if (t == T || troop > -1) {
                        score = Integer.MIN_VALUE + score;
                        return this;
                    }
                    owner[c] = 0;
                    get[c] = t;
                    for (int j = 0; j < B; ++j) {
                        if (used[j] < t - sendTurn[c][j]) used[j] = t - sendTurn[c][j];
                    }
                    score += 0x1000 - t * growth[c];
                }
                return this;
            }
        }

        Plan(Base[] bases) {
            this.bases = bases;
        }

        State HC() {
            State best = new State().init();
            for (int i = 0; i < 1; ++i) {
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

    private void debug(Object... o) {
        System.err.println(Arrays.deepToString(o));
    }
}
