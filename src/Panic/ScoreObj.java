package Panic;

import vgpackage.*;
//import java.awt.*;

public class ScoreObj extends GameObj {

    // ==== GameObj methods

    /**
     * Prepares for a new game or level.  Override this method; the
     * default method does nothing.
     * @param type GAME, LEVEL, or user-defined value
     */
    public static void prepare(int type) {
        switch (type) {
        case GAME:
            removeAll();
            break;
        case LEVEL:
            removeAll();
            break;
        }
    }

    /**
     * Determines if a game level has been completed.
     * Override this method as required.  The default method always
     * returns true.
     */
    public static boolean levelCompleted() {
        return true;
    }

    /**
     * Move all the objects.  Also, process any other logic.
     */
    public static void move() {
        iter.toFirst();
        while (iter.isNext()) {
            ScoreObj s = (ScoreObj)iter.getNext();
            s.moveOne();
        }

    }

    /**
     * Plot all the objects.
     */
    public static void draw() {
        iter.toFirst();
        while (iter.isNext()) {
            ScoreObj s = (ScoreObj)iter.getNext();
            s.drawOne();
        }
    }

	protected void drawOne() {
		final Pt sLoc = new Pt();
        if (getStatus() == S_VACANT) return;
        BEngine.ptWorldToView(pos.x, pos.y, sLoc);
        String s = Integer.toString(score);
        parent.getCharSet(0).centerString(s, sLoc.x, sLoc.y);
	}

    /**
     * Adjust the number of active objects by a value.
     * Override this method to modify the subclass-specific
     * total.
     */
    public void adjustActiveCount(int val) {
        activeCount += val;
    }
    //===================================

    private static GameObjIterator iter;

	public static void init(Panic p) {
		parent = p;
        iter = new GameObjIterator(TOTAL);
        while (iter.isRoom())
            iter.store(new ScoreObj());
	}

	public static void unusedstart() {
		removeAll();
	}

	private static void removeAll() {
        iter.toFirst();
        while (iter.isNext()) {
            ScoreObj s = (ScoreObj)iter.getNext();
            s.setStatus(S_VACANT);
        }
	}

	protected void moveOne() {
        if (getStatus() == S_VACANT) return;

        pos.y += yvel;

        lifeSpan -= VidGame.CYCLE;
        if (lifeSpan <= 0)
            setStatus(S_VACANT);
	}

	public static void add(int score, Pt loc, int lifeSpan) {
        ScoreObj n = (ScoreObj)findVacant(iter);
        if (n == null) return;

		n.pos.x = loc.x + Board.OBJ_H_CENTER;
		n.pos.y = loc.y;

        n.setStatus(S_ACTIVE);
		n.lifeSpan = lifeSpan;
		n.score = score;
		n.yvel = -(YVEL + YVELADD * activeCount);
		n.pos.y -= n.yvel * (VidGame.FPS >> 4);
	}

	private static Panic parent;
	private static ScoreObj list[];
	private static int activeCount;

	private static final int TOTAL = 5;
	private static final int YVEL = BEngine.TICK * 300;
	private static final int YVELADD = BEngine.TICK * 200;

	private Pt pos = new Pt();
	private int lifeSpan;
	private int score;
	private int yvel;

    private static final int S_ACTIVE = 1;

}
