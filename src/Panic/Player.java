package Panic;

import vgpackage.*;
import mytools.*;
import java.awt.*;

public class Player extends Obj {
    // ==== GameObj methods

    /**
     * Prepares for a new game or level.  Override this method; the
     * default method does nothing.
     * @param type GAME, LEVEL, or user-defined value
     */
    public static void prepare(int type) {
        switch (type) {
        case LEVEL:
			// Bring on the player if we're not in the demo mode.
			if (VidGame.getMode() == VidGame.MODE_PLAYING)
                player.prepareLevel();
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
        player.moveOne();
    }

    /**
     * Plot all the objects.
     */
    public static void draw() {
        player.drawOne();
    }

    protected void drawOne() {
        if (NOPLAYER) return;
        if (getStatus() == S_VACANT) return;

//		if (!onBoard) return;
		final Pt sLoc = new Pt();
		Board.worldToView(position.x,position.y,sLoc);
		BEngine.drawSprite(ims[calcSprite()], sLoc);
	}

    //===================================

    private void prepareLevel() {
        position.x = Board.WORLD_CELL_XM * 5;
        position.y = Board.WORLD_FLOOR_Y;
        setOnBoard();
        setStatus(S_NORMAL);
        lastMovedDir = Board.RIGHT;
        digDelay = 0;
        spriteIndex = S_STAND;
        spriteMirror = false;
        outOfAir = false;
    }

    public static void init() {
		Sprite s = new Sprite(Panic.getSprite(), 167,0,270,100);
		ims = new Sprite[S_TOTAL];

		for (int i = 0; i < S_TOTAL; i++) {
			int x = (i % 9) * 30 + 1;
			int y = (i / 9) * 50;

			ims[i] = new Sprite(s, x, y, 29, 49, 15, 49);

			// Add collision rectangle only for the first body; assume others are same.

			if (i == S_STAND) {
				final int colRects[] = {10,9,19,48, 7,21,22,34};

                // I don't know why this line crashes in Internet Explorer.
                // The second one, with the explicit argument count, works.
                ims[i].addColRect(colRects);
//				ims[i].addColRect(colRects,2);
			}
		}
       // Debug.assert(false,"Player 73");

        player = new Player();
	}

    public static Player getPlayer() {
        return player;
    }

	protected void moveOne() {
        if (NOPLAYER) return;

        int status = getStatus();
		int runEffect = -1;

		boolean gasp = (VidGame.getStage() == Panic.GS_NORMAL && Board.oxygen() < Board.OXY_LOW);
		if (!gasp)
			Sfx.stop(Panic.E_GASP);
		else
			Sfx.play(Panic.E_GASP);

		digDelay -= DIG_DELAY_ADJ;

		do {
			if (VidGame.getStage() == Panic.GS_FALLING) {
				spriteIndex = S_STAND;
				break;
			}

			if (VidGame.getStage() == Panic.GS_DYING) {
				Sfx.stop(Panic.E_GASP);
				if (outOfAir) {
					spriteIndex = S_EXHAUST;
					if (VidGame.getStageTime() > 500)
						spriteIndex++;
				} else {
					spriteIndex = S_ATTACKED + ((++frame >> 1) & 1);
					if (status != S_VACANT && VidGame.getStageTime() >= 800) {
						setStatus(S_VACANT);
						SpriteExp.add(ims[calcSprite()], position, 3000, 0, 30, 50, 730);
						Sfx.stop(Panic.E_ATTACK);
						Sfx.play(Panic.E_PDEATH, 150, 0);
					}
					break;
				}
			}
/*
			// Bring on the player if it's the first frame of the intro stage,
			// and we're not in the demo mode.
			if (
				VidGame.getMode() == VidGame.MODE_PLAYING
			 && VidGame.stageStart(Panic.GS_INTRO)
			) {
				position.x = Board.WORLD_CELL_XM * 5;
				position.y = Board.WORLD_FLOOR_Y;
				setOnBoard();
				lastMovedDir = Board.RIGHT;

				digDelay = 0;
				spriteIndex = S_STAND;
				spriteMirror = false;
				outOfAir = false;
			}
*/
			if (
				status == S_VACANT
			 || (	VidGame.getStage() != Panic.GS_NORMAL
				 && VidGame.getStage() != Panic.GS_DYING
				)
			) break;

			int distMoved = 0;
			int speed = 0;

			do {
				// Determine if player wants to dig or fill.  If so, leave the move loop.
				if (!outOfAir) {
					checkDigOrFill();
					if (digAction != 0)
						break;
				}

				// Calculate possible moves

				int possMoves = Board.detMoves(position, 1);

				// Determine speed

				speed = FALL_SPEED;
				if (!Board.falling(possMoves)) {
					int scale = Math.min(256, 77 + (Board.oxygen() * (256-77)) / 16384);
					int defSpeed = Bonus.active(Bonus.TY_SPEED) ? SPEED_FAST : SPEED_NORM;
					if (Spider.levelCompleted())
						scale = 256;
					speed = (defSpeed * scale) >> 4;
                }

				// Determine desired direction of movement from joystick.
				{
					final int posToDir[] = {-1,
						Board.UP, Board.UP, Board.RIGHT,
						Board.DOWN,Board.DOWN,Board.DOWN,
						Board.LEFT,Board.UP};

					setDesiredDir(posToDir[VidGame.getJoystick().pos()]);
				}

				if (outOfAir) {
					setDesiredDir(-1);

					// If collapsed on ladder, simulate a fall.

	//				db.pr("out of air, possMoves="+possMoves+", stageTime="+parent.stageTime() );
					if (
						Board.movePossible(Board.DOWN,possMoves)
					 && VidGame.getStageTime() > 500
					) {
						setDesiredDir(Board.DOWN);
						speed = FALL_SPEED;
					}
				}

				int step = speed - distMoved;
				if (step > 0) {
					step = moveOne(step, possMoves);
					distMoved += step;
				}

				if (outOfAir)
					continue;

				animate(8);

	//			runSfxDelay -= VidGame.CYCLE;

				if (movedDir == -1) {
					if (spriteIndex != S_CLIMB) {
						spriteIndex = S_STAND;
						frame = 0;
					}
				} else if (movedDir != Board.FALL) {
					final int s[] = {S_CLIMB, S_RUN, S_CLIMB, S_RUN};
					spriteIndex = s[movedDir];
					runEffect = Bonus.active(Bonus.TY_SPEED) ? Panic.E_RUNB : Panic.E_RUNA;

				} else {
					frame = 0;
				}

				if (lastMovedDir == Board.LEFT || lastMovedDir == Board.RIGHT)
					spriteMirror = (lastMovedDir == Board.LEFT);

			} while (distMoved < speed);

			if (digAction != 0) {
				spriteIndex = (S_DIG + ((++frame >> 2) & 1));
				spriteMirror = (digSide == 0);
				if (digDelay <= 0) {
					digDelay = 1000;
					Board.processDigAction(position, digSide, digAction,
						Bonus.active(Bonus.TY_SHOVEL) ? 3 : 2);
					Sfx.play(Panic.E_DIG + (digAction-1));
				}
			}

			if (outOfAir) break;

			if (Spider.hitPlayer(position, ims[S_STAND])) {
				VidGame.setStage(Panic.GS_DYING);
			}

			if (VidGame.getStage() == Panic.GS_NORMAL) {
				Bonus.hitPlayer(position, ims[S_STAND]);
				if (!Spider.levelCompleted()) {
					if (Board.reduceOxygen(Bonus.active(Bonus.TY_AIR) ? (VidGame.CYCLE>>1) : VidGame.CYCLE)) {
						VidGame.setStage(Panic.GS_DYING);
						outOfAir = true;
					}
				}
			}
		} while (false);

		for (int j = Panic.E_RUNA; j <= Panic.E_RUNB; j++) {
			if (runEffect != j)
				Sfx.stop(j);
			else
				Sfx.play(j);
		}
	}

	private int calcSprite() {
		int n = spriteIndex;
		if (n < S_CLIMB && spriteMirror)
			n += (S_MIRRORED - S_STAND);
		if (spriteIndex == S_RUN || spriteIndex == S_CLIMB)
			n += ((frame >> 2) & 1);
		return n;
	}

	private void checkDigOrFill() {

		digAction = 0;

		digSide = -1;
		if (dir == Board.LEFT) digSide = 0;
		if (dir == Board.RIGHT) digSide = 1;

		if (digSide < 0) return;

		int digFlags = Board.digActionPossible(position, digSide);

		for (int btn = 0; btn < 2; btn++) {
			if (!VidGame.getJoystick().fireButtonState(btn)) continue;

			if ((digFlags & (1 << btn)) == 0) break;

			// we can dig here.

			digAction = btn + 1;

			break;
		}
	}

	private static final boolean NOPLAYER = false;

	private static final int SPEED_NORM = BEngine.TICK * 85;
	private static final int SPEED_FAST = BEngine.TICK * 115;

	private static final int DIG_DELAY_ADJ = 3800/VidGame.FPS;
	private static final int S_STAND = 0;
	private static final int S_RUN = 1;
	private static final int S_DIG = 3;
	private static final int S_EXHAUST = 5;
	private static final int S_CLIMB = 7;
	private static final int S_MIRRORED = 9;
	private static final int S_ATTACKED = 16;
	private static final int S_TOTAL = 18;

    private static final int S_NORMAL = 1;

	private static Sprite ims[];
    private static Player player;

	private int digAction;	// 0:none 1:dig 2:fill
	private int digSide;	// 0:right 1:left
	private int digDelay;	// Must count down to zero before affecting hole
	private int spriteIndex;
	private boolean outOfAir;
	private boolean spriteMirror;
}
