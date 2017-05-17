import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;

class Troop {
    int owner;
    int size;
    int x, y;
    int sourceId;
    int targetId;
    int depTime;
    int arrivalTime;
}

class Base {
    int x, y;
    int owner;
    int size;
    int growthRate;
}

class TestCase {
    public static final int SIMULATION_TIME = 2000;
    public static final int PERSON_CAP = 1000;
    public static final int S = 600;

    static final int MIN_BASE_COUNT = 20;
    static final int MAX_BASE_COUNT = 100;
    static final int MIN_GROWTH_RATE = 1;
    static final int MAX_GROWTH_RATE = 3;
    static final int MIN_PERSONNEL = 1;
    static final int MAX_PERSONNEL = 10;
    static final int MIN_SPEED = 1; // 1
    static final int MAX_SPEED = 10; // 10
    static final int MIN_OPPONENTS = 1; // 1
    static final int MAX_OPPONENTS = 4; // 4

    public int NOpp;        // player 0 is the player, players 1..NOpp are AI opponents
    public double[] powers; // all opponents except player have strong troops
    public int B;
    public Base[] bases;
    public int speed;

    SecureRandom rnd = null;

    public TestCase(long seed) {
        try {
            rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.setSeed(seed);
        } catch (Exception e) {
            System.err.println("ERROR: unable to generate test case.");
            System.exit(1);
        }

        B = rnd.nextInt(MAX_BASE_COUNT - MIN_BASE_COUNT + 1) + MIN_BASE_COUNT;
        speed = rnd.nextInt(MAX_SPEED - MIN_SPEED + 1) + MIN_SPEED;
        NOpp = rnd.nextInt(MAX_OPPONENTS - MIN_OPPONENTS + 1) + MIN_OPPONENTS;

        if (seed == 1) {
            B = MIN_BASE_COUNT;
            speed = MIN_SPEED;
            NOpp = MIN_OPPONENTS;
        }
        if (seed == 2) {
            B = MAX_BASE_COUNT;
            speed = MAX_SPEED;
            NOpp = MAX_OPPONENTS;
        }

        bases = new Base[B];
        HashSet<Integer> locations = new HashSet<>();
        for (int i = 0; i < B; i++) {
            int bx, by;
            while (true) {
                bx = rnd.nextInt(S - 2) + 1;
                by = rnd.nextInt(S - 2) + 1;
                Integer loc = new Integer(S * bx + by);
                if (locations.contains(loc))
                    continue;
                locations.add(loc);
                break;
            }
            bases[i] = new Base();
            bases[i].x = bx;
            bases[i].y = by;
            bases[i].owner = rnd.nextInt(NOpp + 1);
            bases[i].size = rnd.nextInt(MAX_PERSONNEL - MIN_PERSONNEL + 1) + MIN_PERSONNEL;
            bases[i].growthRate = rnd.nextInt(MAX_GROWTH_RATE - MIN_GROWTH_RATE + 1) + MIN_GROWTH_RATE;
        }

        powers = new double[NOpp + 1];
        powers[0] = 1.0;
        for (int i = 1; i <= NOpp; ++i) {
            powers[i] = 1.0 + rnd.nextDouble() * 0.2;
            // powers[i] = 1.0;
        }
    }
}

class Drawer extends JFrame {
    public static final int EXTRA_WIDTH = 250;
    public static final int EXTRA_HEIGHT = 100;

    public World world;
    public DrawerPanel panel;

    public int S;
    public int width, height;

    public boolean pauseMode = false;
    public boolean stepMode = false;

    class DrawerKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            synchronized (keyMutex) {
                if (e.getKeyChar() == ' ') {
                    pauseMode = !pauseMode;
                }
                if (e.getKeyChar() == 's') {
                    stepMode = !stepMode;
                }
                keyPressed = true;
                keyMutex.notifyAll();
            }
        }
    }

    static int getOwnerColor(int owner) {
        int[] colors = {0x000000, 0x0080FE, 0xFE0080, 0xFE8000, 0x00FE80};
        return colors[owner];
    }

    class DrawerPanel extends JPanel {
        public void paint(Graphics g) {
            synchronized (world.worldLock) {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(new Color(0xA0A0A0));
                g.fillRect(15, 15, S + 1, S + 1);

                // draw bases
                g.setFont(new Font("Arial", Font.PLAIN, 11));
                for (int b = 0; b < world.tc.B; b++) {
                    int c = getOwnerColor(world.tc.bases[b].owner);
                    g.setColor(new Color(c));
                    int x = world.tc.bases[b].x;
                    int y = world.tc.bases[b].y;
                    g.fillRect(15 + x - 3, 15 + y - 3, 7, 7);
                    g.setColor(new Color(c / 2));
                    g.drawRect(15 + x - 3, 15 + y - 3, 7, 7);
                    g2.drawString("" + world.tc.bases[b].size, 15 + x + 1, 15 + y);
                }

                // draw troops
                g.setFont(new Font("Arial", Font.PLAIN, 10));
                for (Troop t : world.troops) {
                    int c = getOwnerColor(t.owner);
                    g.setColor(new Color(c));
                    g.fillOval(15 + t.x - 3, 15 + t.y - 3, 7, 7);
                    g.setColor(new Color(c / 2));
                    g2.drawString("" + t.size, 15 + t.x + 1, 15 + t.y);
                }

                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.setColor(Color.BLACK);

                int horPos = 40 + S;

                g2.drawString("Speed = " + world.tc.speed, horPos, 30);
                g2.drawString("Simulation step = " + world.curStep, horPos, 50);
                g2.drawString(String.format("Score = %.4f", world.playerScore), horPos, 70);

                // output shares of active players using their respective colors
                for (int i = 0; i <= world.tc.NOpp; ++i) {
                    g2.setColor(new Color(getOwnerColor(i)));
                    try {
                        g2.drawString(String.format("#" + i + " (power %.2f) share %.4f", world.tc.powers[i], world.playersUnits[i] * 1.0 / world.totalUnits), horPos, 90 + 20 * i);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    class DrawerWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            System.exit(0);
        }
    }

    final Object keyMutex = new Object();
    boolean keyPressed;

    public void processPause() {
        synchronized (keyMutex) {
            if (!stepMode && !pauseMode) {
                return;
            }
            keyPressed = false;
            while (!keyPressed) {
                try {
                    keyMutex.wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    public Drawer(World world) {
        super();

        panel = new DrawerPanel();
        getContentPane().add(panel);

        addWindowListener(new DrawerWindowListener());

        this.world = world;

        S = world.tc.S;
        width = S + EXTRA_WIDTH;
        height = S + EXTRA_HEIGHT;

        addKeyListener(new DrawerKeyListener());

        setSize(width, height);
        setTitle("Visualizer tool for problem AbstractWars");

        setResizable(false);
        setVisible(true);
    }
}

class World {
    final Object worldLock = new Object();
    TestCase tc;
    int curStep = -1;
    List<Troop> troops = new ArrayList<>();
    double playerScore = 0;
    long totalUnits;
    long[] playersUnits;
    boolean simComplete = false;

    World(TestCase tc) {
        this.tc = tc;
    }

    void updateScore() {
        // score is % of bases size and troops which belongs to the player
        totalUnits = 0;
        playersUnits = new long[tc.NOpp + 1];
        for (int i = 0; i < tc.B; ++i) {
            totalUnits += tc.bases[i].size;
            playersUnits[tc.bases[i].owner] += tc.bases[i].size;
        }
        for (Troop t : troops) {
            totalUnits += t.size;
            playersUnits[t.owner] += t.size;
        }
        // update total player's score
        if (totalUnits > 0)
            playerScore += playersUnits[0] * 1.0 / totalUnits;
        if (playersUnits[0] == totalUnits || playersUnits[0] == 0) {
            // in either case, nothing else to be done
            simComplete = true;
        }
    }

    void updateTroopDepartures(int owner, int[] attacks) {
        synchronized (worldLock) {
            String warnPrefix = "WARNING: time step = " + curStep + ". ";
            // run through attacks, ignoring invalid ones (but print warning if user's one is invalid)
            for (int i = 0; i < attacks.length / 2; ++i) {
                int from = attacks[2 * i];
                int to = attacks[2 * i + 1];
                if (from < 0 || from >= tc.B || to < 0 || to >= tc.B) {
                    if (owner == 0) {
                        System.err.println(warnPrefix + "Invalid base index in troop sending attempt " + i + ", ignoring.");
                    }
                    continue;
                }
                if (tc.bases[from].owner != owner) {
                    if (owner == 0) {
                        System.err.println(warnPrefix + "Base not owned by you in troop sending attempt " + i + ", ignoring.");
                    }
                    continue;
                }
                if (from == to) {
                    if (owner == 0) {
                        System.err.println(warnPrefix + "Sending troop from the base to itself in troop sending attempt " + i + ", ignoring.");
                    }
                    continue;
                }
                // don't check ownership of the target base, it can be both attack and reinforcement
                if (tc.bases[from].size < 2) {
                    if (owner == 0) {
                        System.err.println(warnPrefix + "Source base has less than 2 units in troop sending attempt " + i + ", ignoring.");
                    }
                    continue;
                }

                // spawn a new troop from source base
                Troop t = new Troop();
                t.owner = owner;
                t.size = tc.bases[from].size / 2;
                t.x = tc.bases[from].x;
                t.y = tc.bases[from].y;
                t.sourceId = from;
                t.targetId = to;
                t.depTime = curStep;
                int moveT = (int) Math.ceil(Math.sqrt(Math.pow(t.x - tc.bases[to].x, 2) +
                        Math.pow(t.y - tc.bases[to].y, 2)) / tc.speed);
                t.arrivalTime = t.depTime + moveT;
                troops.add(t);

                tc.bases[from].size -= t.size;
            }
        }
    }

    void updateTroopArrivals() {
        synchronized (worldLock) {
            // check the troops which arrive at this time step
            for (int t = 0; t < troops.size(); ) {
                if (troops.get(t).arrivalTime != curStep) {
                    t++;
                    continue;
                }
                // let troop interact with the base
                int town = troops.get(t).owner;
                int tsize = troops.get(t).size;
                int ttarget = troops.get(t).targetId;
                if (town == tc.bases[ttarget].owner) {
                    // reinforcement scenario
                    tc.bases[ttarget].size += tsize;
                } else {
                    // attack scenario
                    // compare sizes of troop and base with respect to their powers
                    // attack/defense power = size * powers[owner]
                    double pTroop = tsize * tc.powers[town];
                    double pBase = tc.bases[ttarget].size * tc.powers[tc.bases[ttarget].owner];
                    if (pBase >= pTroop) {
                        // base wins but loses as many units as is necessary to overpower all units of the troop (rounding up)
                        // it's possible that the base becomes empty
                        tc.bases[ttarget].size = Math.max(0, tc.bases[ttarget].size - (int) Math.ceil(pTroop / tc.powers[tc.bases[ttarget].owner]));
                        // empty bases preserve their ownership, but that doesn't affect anything except visualization
                    } else {
                        // troop wins, occupies the bases but loses units
                        tc.bases[ttarget].size = Math.max(0, tsize - (int) Math.ceil(pBase / tc.powers[town]));
                        tc.bases[ttarget].owner = town;
                    }
                }
                // make sure that after troop arrivals the base doesn't hold more than cap units
                if (tc.bases[ttarget].size > tc.PERSON_CAP)
                    tc.bases[ttarget].size = tc.PERSON_CAP;
                // regardless of the outcome, the troop stops existing
                troops.remove(t);
            }
        }
    }

    void startNewStep() {
        curStep++;
        // update bases personnel
        for (int i = 0; i < tc.B; ++i) {
            if (tc.bases[i].size > 0)
                tc.bases[i].size += tc.bases[i].growthRate + tc.bases[i].size / 100;
            // limit growth
            if (tc.bases[i].size > tc.PERSON_CAP)
                tc.bases[i].size = tc.PERSON_CAP;
        }
        // update troops positions (for visualization/passing to solution purposes)
        for (Troop t : troops) {
            if (t.arrivalTime == curStep) {
                t.x = tc.bases[t.targetId].x;
                t.y = tc.bases[t.targetId].y;
                continue;
            }
            // if the troop has not arrived yet, approximate its position based on time it moved
            double partMoved = (curStep - t.depTime) * 1.0 / (t.arrivalTime - t.depTime);
            double x = tc.bases[t.sourceId].x + (tc.bases[t.targetId].x - tc.bases[t.sourceId].x) * partMoved;
            double y = tc.bases[t.sourceId].y + (tc.bases[t.targetId].y - tc.bases[t.sourceId].y) * partMoved;
            t.x = (int) x;
            t.y = (int) y;
        }
    }

}

// real AI opponent
class RealAI {
    SecureRandom rnd;
    int attackT;
    double locality;
    int B;
    int owner;
    int[] baseX, baseY;

    RealAI(long seed, int own) {
        try {
            rnd = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
        }
        rnd.setSeed(seed);
        owner = own;
        // once the base personnel reaches this threshold, the base sends out troops
        attackT = TestCase.PERSON_CAP / 2 + rnd.nextInt(TestCase.PERSON_CAP / 2);
        // the higher the locality, the more value there is in attacking nearby bases
        locality = rnd.nextDouble() * 2 + 1;
    }

    int init(int[] baseLocations, int speed) {
        B = baseLocations.length / 2;
        baseX = new int[B];
        baseY = new int[B];
        for (int i = 0; i < B; ++i) {
            baseX[i] = baseLocations[2 * i];
            baseY[i] = baseLocations[2 * i + 1];
        }
        return 0;
    }

    ArrayList<Integer> others;

    // picks a random base to attack based on distance to the opponent bases: the closer the base, the higher the chances are
    int getRandomBase(int sourceInd) {
        double[] probs = new double[others.size()];
        double sp = 0;
        for (int i = 0; i < others.size(); ++i) {
            int ind = others.get(i).intValue();
            probs[i] = Math.pow(1.0 / (Math.pow(baseX[sourceInd] - baseX[ind], 2) + Math.pow(baseY[sourceInd] - baseY[ind], 2)), locality);
            sp += probs[i];
        }

        double r = rnd.nextDouble() * sp;
        double s = 0;
        for (int i = 0; i < others.size(); ++i) {
            s += probs[i];
            if (s >= r)
                return others.get(i).intValue();
        }
        return others.get(others.size() - 1).intValue();
    }

    int[] sendTroops(int[] bases, int[] troops) {
        // compile the list of bases owned by other players
        others = new ArrayList<Integer>();
        for (int i = 0; i < B; ++i)
            if (bases[2 * i] != owner)
                others.add(i);
        if (others.size() == 0) {
            // noone to fight!
            return new int[0];
        }

        ArrayList<Integer> att = new ArrayList<Integer>();
        for (int i = 0; i < B; ++i) {
            if (bases[2 * i] == owner && bases[2 * i + 1] > attackT) {
                // send troops to a random base of different ownership
                att.add(i);
                att.add(getRandomBase(i));
            }
        }
        int[] ret = new int[att.size()];
        for (int i = 0; i < att.size(); ++i)
            ret[i] = att.get(i).intValue();
        return ret;
    }
}

public class AbstractWarsVis {
    static boolean vis = true;
    static int delay = 100;
    static boolean startPaused = false;
    AbstractWars player = new AbstractWars();

    RealAI[] realAIs;

    int callInit(int owner, int[] baseLocations, int speed) throws IOException {
        if (owner > 0)
            return realAIs[owner - 1].init(baseLocations, speed);
        return player.init(baseLocations, speed);
    }

    int[] callSendTroops(int owner, int[] bases, int[] troops) throws IOException {
        if (owner > 0)
            return realAIs[owner - 1].sendTroops(bases, troops);
        return player.sendTroops(bases, troops);
    }

    public double runTest(long seed) {
        TestCase tc = new TestCase(seed);

        // initialize opponents
        realAIs = new RealAI[tc.NOpp];
        for (int i = 1; i <= tc.NOpp; ++i)
            realAIs[i - 1] = new RealAI(Long.parseLong(seed + "" + i), i);

        // call init for all players
        int[] baseLoc = new int[tc.B * 2];
        for (int i = 0; i < tc.B; ++i) {
            baseLoc[2 * i] = tc.bases[i].x;
            baseLoc[2 * i + 1] = tc.bases[i].y;
        }
        try {
            callInit(0, baseLoc, tc.speed);
        } catch (Exception e) {
            System.err.println("ERROR: calling init failed for your solution: " + e);
            return -1;
        }
        for (int i = 1; i <= tc.NOpp; ++i)
            try {
                callInit(i, baseLoc, tc.speed);
            } catch (Exception e) {
                System.err.println("Opponent " + i + " threw exception in init: " + e);
            }

        World world = new World(tc);
        Drawer drawer = null;
        if (vis) {
            drawer = new Drawer(world);
            if (startPaused) {
                drawer.pauseMode = true;
            }
        }

        for (int step = 0; step < tc.SIMULATION_TIME; step++) {
            world.startNewStep();
            world.updateTroopArrivals();

            // let players do their turns ("simultaneously", so that neither has more information than the other)
            int[] basesArg = new int[tc.B * 2];
            for (int i = 0; i < tc.B; i++) {
                basesArg[2 * i] = tc.bases[i].owner;
                basesArg[2 * i + 1] = tc.bases[i].size;
            }

            int[] troopsArg = new int[world.troops.size() * 4];
            int it = 0;
            for (Troop t : world.troops) {
                troopsArg[it++] = t.owner;
                troopsArg[it++] = t.size;
                troopsArg[it++] = t.x;
                troopsArg[it++] = t.y;
            }

            String errPrefix = "ERROR: time step = " + step + ". ";
            for (int owner = 0; owner <= tc.NOpp; owner++) {
                int[] attacks;
                try {
                    attacks = callSendTroops(owner, basesArg, troopsArg);
                } catch (Exception e) {
                    if (owner > 0) {
                        // AI opponent threw exception - weird, but let's ignore and do nothing
                        System.err.println(errPrefix + "Opponent " + owner + " threw exception in sendTroops: " + e);
                        attacks = new int[0];
                    } else {
                        System.err.println(errPrefix + "Unable to get return from sendTroops. ");
                        e.printStackTrace();
                        return -1;
                    }
                }
                if (owner == 0) {
                    // validate size of user's return
                    int na = attacks.length;
                    if (na % 2 > 0) {
                        System.err.println(errPrefix + "Return from sendTroops must have even length.");
                        return -1;
                    }
                    na /= 2;
                    if (na > tc.B) {
                        System.err.println(errPrefix + "You can send at most B troops on each step.");
                        return -1;
                    }
                }
                world.updateTroopDepartures(owner, attacks);
            }

            world.updateScore();

            if (vis) {
                drawer.processPause();
                drawer.repaint();
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (world.simComplete) {
                // System.err.println("Simulation completed at step " + step + ".");
                // if we finish simulation early, the rest of steps will get the same score each step
                world.playerScore += (tc.SIMULATION_TIME - step - 1) * world.playersUnits[0] * 1.0 / world.totalUnits;
                break;
            }
        }
        debug("score", world.playerScore, "players", tc.NOpp + 1, "bases", tc.bases.length, "speed", tc.speed, "powers", tc.powers);
        return world.playerScore;
    }

    public static void main(String[] args) {
        try {
            final int testcase = 1000;
            vis = false;
            double sum = 0;
            long seed = 1;
            for (long end = seed + testcase; seed < end; ++seed) {
                double score = new AbstractWarsVis().runTest(seed);
                if (score < 0) throw new RuntimeException("seed : " + seed);
                sum += score;
                debug("seed", seed, "score", score, "avg", sum / (seed - end + testcase + 1));
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static void debug(Object... o) {
        System.out.println(Arrays.deepToString(o));
    }
}
