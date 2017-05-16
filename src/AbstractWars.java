import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractWars {

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
        return 0;
    }

    int[] sendTroops(int[] bases_, int[] troops_) {
        {
            for (int i = 0; i < this.bases.length; ++i) {
                if (turn == 1) {
                    bases[i].growth = bases_[2 * i + 1] - bases[i].troops;
                    if (bases[i].growth < 1 || 3 < bases[i].growth) throw new RuntimeException();
                }
                bases[i].owner = bases_[2 * i];
                bases[i].troops = bases_[2 * i + 1];
            }
        }
        ++turn;
        List<Base> aly = Stream.of(bases).filter(x -> x.owner == 0).collect(Collectors.toList());
        List<Base> opp = Stream.of(bases).filter(x -> x.owner != 0).collect(Collectors.toList());
        if (turn < 2 || opp.size() == 0) return new int[0];

        List<Integer> ret = new ArrayList<>();
        final int limit = speed < 2 ? 900 : 8;
        for (Base b : aly) {
            if (b.troops >= limit) {
                Base t = opp.stream().sorted((x, y) -> sendTurn[b.id][x.id] - sendTurn[b.id][y.id]).findFirst().get();
                ret.add(b.id);
                ret.add(t.id);
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
