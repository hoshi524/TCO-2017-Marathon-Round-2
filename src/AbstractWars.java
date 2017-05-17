import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractWars {

    private static final int MAX_TURN = 2000;
    private int[][] arrivalTroops;
    private Base[] bases;
    private int speed, turn;
    private int[][] sendTurn;

    public int init(int[] baseLocations, int speed_) {
        bases = new Base[baseLocations.length / 2];
        for (int i = 0; i < bases.length; ++i) {
            Base b = new Base();
            b.id = i;
            b.x = baseLocations[2 * i];
            b.y = baseLocations[2 * i + 1];
            bases[i] = b;
        }
        turn = 0;
        speed = speed_;
        sendTurn = new int[bases.length][bases.length];
        for (int i = 0; i < bases.length; ++i) {
            for (int j = i + 1; j < bases.length; ++j) {
                sendTurn[i][j] = sendTurn[j][i] = (int) Math.ceil(distance(bases[i], bases[j]) / speed);
            }
        }
        arrivalTroops = new int[bases.length][MAX_TURN];
        return 0;
    }

    int[] sendTroops(int[] bases_, int[] troops_) {
        int players = 0;
        {
            Set<Integer> set = new HashSet<>();
            for (int i = 0; i < this.bases.length; ++i) {
                if (turn == 1) {
                    bases[i].growth = bases_[2 * i + 1] - bases[i].troops;
                    if (bases[i].growth < 1 || 3 < bases[i].growth) throw new RuntimeException();
                }
                bases[i].owner = bases_[2 * i];
                bases[i].troops = bases_[2 * i + 1];
                set.add(bases[i].owner);
            }
            players = set.size();
        }
        ++turn;
        List<Base> aly = Stream.of(bases).filter(x -> x.owner == 0 && x.troops > 0).collect(Collectors.toList());
        List<Base> opp = Stream.of(bases).filter(x -> x.owner != 0 || x.troops == 0).collect(Collectors.toList());
        if (turn < 2 || opp.size() == 0) return new int[0];

        for (Base b : opp) {
            b.reverse = false;
            int troops = b.troops;
            for (int t = turn + 1, ts = Math.min(t + 100, MAX_TURN); t < ts; ++t) {
                troops += b.growth + troops / 100;
                troops -= arrivalTroops[b.id][t];
                if (troops < 0) {
                    b.reverse = true;
                    break;
                }
            }
        }
        opp = opp.stream().filter(x -> x.reverse == false).collect(Collectors.toList());

        List<Integer> ret = new ArrayList<>();
        if (opp.size() > 0) {
            final int limit = speed < 2 ? 990 : 2;
            for (Base b : aly) {
                if (b.troops >= limit) {
                    Base t = opp.stream().min((x, y) -> sendTurn[b.id][x.id] - sendTurn[b.id][y.id]).get();
                    int arrival = turn + sendTurn[b.id][t.id];
                    ret.add(b.id);
                    ret.add(t.id);
                    if (arrival < MAX_TURN) arrivalTroops[t.id][arrival] += Math.ceil((b.troops / 2) / 1.20);
                }
            }
        }
        return to(ret);
    }

    class Base {
        int id, x, y, owner, troops, growth;
        boolean reverse;
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
        list = list.subList(0, Math.min(list.size(), bases.length * 2));
        int[] x = new int[list.size()];
        for (int i = 0; i < x.length; ++i) {
            x[i] = list.get(i);
        }
        return x;
    }

    private void debug(Object... o) {
        System.out.println(Arrays.deepToString(o));
    }
}
