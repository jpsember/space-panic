package Panic;

import vgpackage.*;
import mytools.*;
import java.awt.*;

public class Bonus extends Obj {
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
            nextBonusLifeLevel = BONUS_LIFE_INTERVAL - 1;
            break;
        case LEVEL:
            removeAll();
			remaining = BONUS_PER_LEVEL;
			setAppearDelay();
            break;
        }
    }

    /**
     * Determines if a game level has been completed.
     * Override this method as required.  The default method always
     * returns true.
     */
    public static boolean levelCompleted() {
        return (activeCount == 0);
    }

    /**
     * Move all the objects.  Also, process any other logic.
     */
    public static void move() {

		if (VidGame.getStage() != Panic.GS_NORMAL)
            return;

        appearDelay -= VidGame.CYCLE;
        if (appearDelay < 0) {
            if (
                remaining > 0
             && (VidGame.getMode() == VidGame.MODE_PLAYING)
            ) {
                remaining--;
                Bonus b = (Bonus)findVacant(iter);
                if (b != null)
                    b.bringOn();
            }
            setAppearDelay();
        }

        for (int i = 0; i < TY_TYPES; i++) {
            bonusTimes[i] = Math.max(bonusTimes[i] - VidGame.CYCLE, 0);
        }

        move(iter);
    }

    /**
     * Plot all the objects.
     */
    public static void draw() {
        draw(iter);
    }

    protected void drawOne() {
        final Pt workPt = new Pt();
		Board.worldToView(position.x, position.y, workPt);

	    final byte arc_y[] = {
	        0,2,4,5,6,7,8,9,
	        9,8,7,6,5,4,2,0
	    };

		workPt.y -= arc_y[(frame >> 1) & 15];

		BEngine.drawSprite(ims[S_LARGE + type], workPt);
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

	public static final int TY_SPEED = 0;
	public static final int TY_TIME = 1;
	public static final int TY_AIR = 2;
	public static final int TY_SHOVEL = 3;
	public static final int TY_TYPES = 4;

	public static void oldstart() {
//		workPt = new Pt();
		removeAll();
	}

	private static void prepareSprites() {
		Sprite s = new Sprite(parent.getSprite(),90,114,101,53);
		ims = new Sprite[S_TOTAL];

		final short data[] = {
			1,1,	19,30,	10,28,
			21,1,	19,30,	10,28,
			41,1,	19,30,	10,28,
			61,1,	19,30,	10,28,
			81,1,	19,30,	10,28,

			1,32,	12,20,	0,0,
			14,32,	12,20,	0,0,
			27,32,	12,20,	0,0,
			40,32,	12,20,	0,0,
			53,32,	12,20,	0,0,
		};

		int i, j;
		for (i = 0, j = 0; i < S_TOTAL; i++, j += 6) {
			ims[i] = new Sprite(s, data[j],data[j+1],data[j+2],data[j+3],data[j+4],data[j+5]);

			// Add collision rectangle only for the first large one; assume others are same.

			if (i == S_LARGE)
				ims[i].addColRect(4,4,16,22);
		}

	}

    private static GameObjIterator iter;

	private static void removeAll() {
        iter.toFirst();
        while (true) {
            Bonus b = (Bonus)iter.getNextActive();
            if (b == null) break;
            b.setStatus(S_VACANT);
        }
//		active = 0;
	}

	public static void init(Panic parent) {
		Bonus.parent = parent;

        iter = new GameObjIterator(MAX);
        while (iter.isRoom())
            iter.store(new Bonus());


//		workPt = new Pt();
		prepareSprites();

		bonusTimes = new int[TY_TYPES];
	}
/*
	public Bonus() {
	}
*/

	private void bringOn() {
		int side = MyMath.rnd(2);
		position.x = (side == 0) ? -APPEAR_DIST : (Panic.MAIN_WORLD_XM-1) + APPEAR_DIST;
		position.y = Board.rowToWorld(Board.BRICK_LEVELS);	//WORLD_TOP;

		setOnBoard();
        setStatus(S_ACTIVE);

		dir = (side == 0) ? Board.RIGHT : Board.LEFT;
		setDesiredDir(dir);

		int flags = 0;
		for (int i = 0; i < MAX; i++) {
            Bonus b = (Bonus)iter.get(i);
            if (b.getStatus() == S_VACANT) continue;
			flags |= (1 << b.type);
		}

        if (VidGame.getLevel() == nextBonusLifeLevel) {
          nextBonusLifeLevel += BONUS_LIFE_INTERVAL;
          type = TY_LIFE;
        } else {
			if (flags == 0x0f)
				flags = 0;
			do {
				type = MyMath.rnd(TY_TYPES);
			} while ((flags & (1 << type)) != 0);
		}
	}

	private static void setAppearDelay() {
		appearDelay = MyMath.rnd(8000) + 20000;
	}

	protected void moveOne() {
//		db.pr("Bonus.moveOne, pos="+position);

		int distMoved = 0;
		int speed = 0;
		int possMoves = 0;

		do {

			// Calculate possible moves

			possMoves = 0;

			// If appearing or disappearing, we may be off the screen to the left or right.
			// In this case, assume horizontal motion only.

			if (Board.offInX(position.x) != 0) {
				possMoves = (1 << Board.LEFT) | (1 << Board.RIGHT);
			} else {
				possMoves = Board.detMoves(position, 2);

				// If on the floor, make sure we can move both left and right to allow
				// object to exit the board.

				if (position.y == Board.WORLD_FLOOR_Y)
					possMoves |= (1 << Board.LEFT) | (1 << Board.RIGHT);
			}

			// Some of the time, move down if possible (and not reversing direction to do so)
			if (MyMath.rnd(2) == 0) {
				if (
					Board.movePossible(Board.DOWN, possMoves)
				 && dir != Board.UP
				)
					possMoves = (1 << Board.DOWN);
			}

			// Determine speed

			speed = BEngine.TICK * 16 * 30;

			// Determine desired direction of movement.

			int desPoss = filterReverse(possMoves, dir);

			setDesiredDir(chooseRandomDir(desPoss));

			int step = speed - distMoved;
			if (step > 0) {
				step = moveOne(step, possMoves);
				distMoved += step;
			}

		} while (distMoved < speed);

		// Don't increment frame if falling; we don't want bouncing to occur in this case.
		if (!Board.falling(possMoves)) {
			frame++;
			if (
				((frame >> 1) & 0xf) == 0
			 && Board.offInX(position.x) == 0
			) {
				Sfx.play(parent.E_BMOVE);
			}
		}

		if (Board.offInX(position.x) > APPEAR_DIST)
            setStatus(S_VACANT);
	}

	public static boolean active() {
		return activeCount != 0;
	}

	// Test if any bonus objects have collided with the player.
	public static void hitPlayer(Pt loc, Sprite p) {
        iter.toFirst();
        while (true) {
            Bonus s = (Bonus)iter.getNextActive();
            if (s == null) break;

			if (!p.collided(loc.x, loc.y, s.position.x, s.position.y, ims[S_LARGE]))
                continue;

            s.setStatus(S_VACANT);
			SpriteExp.add(ims[S_LARGE + s.type], s.position, 1400, 0, 30, 20, 400);
			Sfx.play(parent.E_GOTBONUS);

			// Set bonus in effect

			switch (s.type) {
			case TY_LIFE:
				VidGame.adjustLives(1);
				break;

			case TY_AIR:
				Board.resetOxygen();
				activateBonus(s.type);
				break;

			case TY_TIME:
				Spider.processTimeBonus();
				activateBonus(s.type);
				break;

			default:
				activateBonus(s.type);
				break;
			}
		}
	}

	private static void activateBonus(int type) {
		bonusTimes[type] = BONUS_TIME;
	}

	public static boolean active(int type) {
		return (bonusTimes[type] != 0);
	}

	public static void disable() {
		for (int i = 0; i < TY_TYPES; i++) {
			bonusTimes[i] = 0;
		}
	}

	public static void plotStatus() {
        boolean valid = BEngine.layerValid();

		// Determine which ones appear.
		// We will have an int that contains nybbles, representing:
		//	[0..2]	1 + type of bonus, or 0 if none
		//	[3]		Set if visible, clr if not visible (blinking)

		int flags = 0;
		int i;
		for (i = TY_TYPES-1; i>=0; i--) {
			if (bonusTimes[i] == 0) continue;

			boolean vis = true;
			if (
				bonusTimes[i] < 0x1800
			 && (bonusTimes[i] & 0x200) > 0x170
			)
				vis = false;

			flags <<= 4;
			flags |= (i+1) | (vis ? 8 : 0);
		}

		int fOld = drawnFlags;
		int fNew = flags;

		BEngine.setColor(Color.black);
		for (i = 0; i < TY_TYPES; i++, fOld >>= 4, fNew >>= 4) {
			if (valid && ((fOld ^ fNew) & 0xf) == 0) continue;

			int x = 2 + (i * 16);
			int y = 2;

			// Erase old

			BEngine.fillRect(x, y, 16, 20);

			int type = (fNew & 0xf);
			if (type < 0x08) continue;

			BEngine.drawSprite(ims[S_SMALL + type - 0x09], x, y);
		}
		drawnFlags = flags;
	}

    private static final int BONUS_LIFE_INTERVAL = 10;
    private static final int BONUS_PER_LEVEL = 1;
	private static final int BONUS_TIME = 80 * 1000;

	private static final int TY_LIFE = 4;
	private static final int TY_TOTAL = 5;

	private static Panic parent;
	private static int drawnFlags;

	private static final int S_LARGE = 0;
	private static final int S_SMALL = (S_LARGE+TY_TOTAL);
	private static final int S_TOTAL = (S_SMALL+TY_TYPES);

	private static int remaining;			// # bonus objects remaining to appear this level
    private static int activeCount;
//	private static int active;				// # bonus objects currently moving around
	private static int appearDelay;
	private static int bonusTimes[];
    private static int nextBonusLifeLevel;          // next level to attempt awarding a bonus life at

//	private static Bonus list[];

	private static final int MAX = BONUS_PER_LEVEL;
//	private static Pt workPt;
	private static Sprite ims[];

	private int type;
	private static final int APPEAR_DIST = BEngine.ONE * 32;

    private static final int S_ACTIVE = 1;
}

