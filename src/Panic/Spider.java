package Panic;

import vgpackage.*;
import mytools.*;
import java.awt.*;
import java.applet.Applet;
import java.net.*;

public class Spider extends Obj {
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
    		appearDelay = 0;
    		smarts = Math.min(5 + VidGame.getLevel() * 3, 60);
			formation = VidGame.getLevel();
            if (formation >= 4)
                formation = MyMath.rnd(4);
            types = VidGame.getLevel();
            if (types >= TYPE_LEVELS)
                types = MyMath.rnd(4) + TYPE_LEVELS - 4;
            break;
        }
    }

    /**
     * Determines if a game level has been completed.
     * Override this method as required.  The default method always
     * returns true.
     */
    public static boolean levelCompleted() {
    	return (VidGame.getStage() == parent.GS_NORMAL && activeCount == 0);
    }

    private static boolean climbEffect;
    /**
     * Move all the objects.  Also, process any other logic.
     */
    public static void move() {
	// Move the spiders
		climbEffect = false;

        if (VidGame.getStage() == Panic.GS_INTRO) {
            appearDelay += VidGame.CYCLE;
            if (
                appearDelay >= APPEAR_DEL
             || VidGame.getMode() != VidGame.MODE_PLAYING
            ) {
                appearDelay = 0;
                int nextType = 0;
                if (activeCount < MAX)
                    nextType = startTypes[types * MAX + activeCount];
                if (nextType == 0)
                    VidGame.setStage(Panic.GS_NORMAL);
                else {
                    Spider s = (Spider)iter.get(activeCount);
                    s.setOnBoard();

                    int fi = formation * 16 + activeCount * 2;

                    s.position.x = Board.WORLD_CELL_XM * (int)formations[fi+1];
                    s.position.y = Board.rowToWorld(formations[fi+0]);

                    s.setStatus(ST_NORM);

                    s.type = nextType - 1;
                    s.reverseDelay = MyMath.rnd(12000) + 4000;
                    s.flags = 0;
                    s.mouthFrame = MTH_NORM;
                    Sfx.play(parent.E_APPEAR);
                }
            }
        }
        move(iter);
		if (!climbEffect)
			Sfx.stop(parent.E_SCLIMB);
		else
			Sfx.play(parent.E_SCLIMB);
    }

    /**
     * Plot all the objects.
     */
    public static void draw() {
        draw(iter);
    }

    protected void drawOne() {
        final Pt workPt = new Pt();

		spriteIndex = S_BODY + ((frame >> 2)&1) + (type << 1);

		Board.worldToView(position.x, position.y, workPt);
		BEngine.drawSprite(ims[spriteIndex], workPt);
		BEngine.drawSprite(ims[S_EYES + eyesFrame], workPt);
		BEngine.drawSprite(ims[S_MOUTH + mouthFrame + (type << 2)], workPt);
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

	public static void init(Panic p) {
//        Debug.assert(false,"Player 73");
        parent = p;
        player = Player.getPlayer();


		workPt = new Pt();

		prepareSprites();

        iter = new GameObjIterator(MAX);
        while (iter.isRoom())
            iter.store(new Spider());
	}

	public static void processTimeBonus() {
        iter.toFirst();
        while (true) {
            Spider s = (Spider)iter.getNextActive();
            if (s == null) break;
			if (s.getStatus() == ST_STUCK)
				s.setStuckDelay();
		}
	}

	// Test if any spiders have collided with the player.
	public static boolean hitPlayer(Pt loc, Sprite p) {
        iter.toFirst();
        while (true) {
            Spider s = (Spider)iter.getNextActive();
            if (s == null) break;
//			if (VidGame.DEBUG) continue;

			if (p.collided(loc.x, loc.y, s.position.x, s.position.y, ims[S_BODY])) {
				// Make spider jump to player's head region.
				s.setStatus(ST_ATTACK);
				s.mouthFrame = MTH_GLAD;
				Sfx.play(parent.E_ATTACK);
				return true;
			}
		}
		return false;
	}


	private static void prepareSprites() {
		Sprite s = new Sprite(parent.getSprite(), 84,0,83,114);
		ims = new Sprite[S_TOTAL];

		final short data[] = {
			1,1,	30,24,	15,20,
			32,1,	30,24,	15,20,
			1,26,	30,24,	15,20,
			32,26,	30,24,	15,20,
			1,51,	30,24,	15,20,
			32,51,	30,24,	15,20,

			2,78,	15,5,	7,12,
			2,84,	15,5,	7,12,
			2,90,	15,5,	7,12,
			2,96,	15,5,	7,12,
			2,102,	15,5,	7,12,

			20,78,	20,8,	9,8,
			20,87,	20,8,	10,6,
			20,96,	20,8,	10,6,
			20,105,	20,8,	10,6,

			41,78,	20,8,	10,6,
			41,87,	20,8,	10,6,
			41,96,	20,8,	10,6,
			41,105,	20,8,	10,6,

			62,78,	20,8,	10,6,
			62,87,	20,8,	10,6,
			62,96,	20,8,	10,6,
			62,105,	20,8,	10,6,

		};

		int i, j;
		for (i = 0, j = 0; i < S_TOTAL; i++, j += 6) {
			ims[i] = new Sprite(s, data[j],data[j+1],data[j+2],data[j+3],data[j+4],data[j+5]);

			// Add collision rectangle only for the first spider body; assume others are same.

			if (i == S_BODY) {
				final int colRects[] = {8,5,21,18,	5,7,24,13};
				ims[i].addColRect(colRects);
			}
		}

	}

	private static void removeAll() {
        iter.toFirst();
        while (iter.isNext()) {
            Spider s = (Spider)iter.getNext();
            s.setStatus(S_VACANT);
        }
	}

	private static final byte startTypes[] = {
		1,1,1,0,0,0,0,0,
		1,1,1,1,1,0,0,0,
		1,1,1,1,1,1,0,0,
		2,2,1,1,0,0,0,0,
		2,2,1,1,1,1,0,0,
		2,2,2,1,1,1,1,1,
		3,2,1,1,1,0,0,0,
		3,2,2,1,1,1,0,0,
		3,2,2,2,1,1,1,1,
		3,3,2,2,1,1,0,0,
		3,3,2,2,1,1,1,1,
		3,3,3,2,2,2,2,1,
		3,3,3,3,2,2,2,2,
		3,3,3,3,3,2,1,1,
                3,3,3,3,2,2,2,2,
	};
	private static final int TYPE_LEVELS = 15;

	private static final byte formations[] = {
		5,2,
		5,8,
		4,12,
		4,18,
		3,22,
		3,28,
		4,32,
		4,38,

		5,6,
		4,20,
		4,40,
		3,8,
		5,34,
		3,30,
		3,40,
		3,20,

		5,24,
		4,14,
		4,24,
		4,36,
		3,26,
		3,42,
		2,46,
		2,32,

		5,14,
		5,24,
		5,36,
		4,4,
		4,44,
		3,16,
		3,26,
		3,34,

	};

	// Test for collisions with other spiders
	private int checkCollisions(int possMoves) {
        int status = getStatus();

        for (int i = 0; i < MAX; i++) {
            Spider s = (Spider)iter.get(i);//)cIter.getNextActive();
            if (!active(s)) continue;
            if (s == this) continue;

			if (!ims[S_BODY].collided(position.x, position.y,
                s.position.x, s.position.y, ims[S_BODY])) continue;

            if (s.getStatus() == ST_FALL) {
                // If we are stuck, or climbing out of a hole that is
                // still complete, make us fall as well.
                if (
                    status == ST_STUCK
                 || (status == ST_CLIMB
                      && Board.calcHoleEnd(position, true) > position.y + (BEngine.ONE * 30))
                ) {
                    setStatus(ST_FALL);
                } else {
                    // If we are being hit by enough weight, destroy us.
                    int impactRows = fallStartRow - Board.startRow(position.y);
                    if (impactRows >= (type + 1)) {

                        // If we were climbing out of a hole, clear its
                        // GHOSTCLIMB flag.
                        if (status == ST_CLIMB)
                            Board.clearGhostHole(position);

                        //	Explode, and add to score
                        explode(impactRows * typeScores[type]);
                        Sfx.play(parent.E_EXP);
                    }
                }
                break;
            } else {
                // Cancel the movement that takes us closer.
                if (position.x < s.position.x)
                    possMoves &= ~(1 << Board.RIGHT);
                if (position.x > s.position.x)
                    possMoves &= ~(1 << Board.LEFT);
                if (position.y < s.position.y)
                    possMoves &= ~(1 << Board.DOWN);
                if (position.y > s.position.y)
                    possMoves &= ~(1 << Board.UP);
            }
		}
		return possMoves;
	}

	private static final int ATTSPD = BEngine.TICK * 16 * 300;

	private void doAttack() {
		int seekX = player.position.x;
		int seekY = player.position.y - VidGame.ONE * 30;
		int spd = ATTSPD;
		// Seek the player's head.
		int dx = MyMath.clamp(seekX - position.x, -spd, spd);
		int dy = MyMath.clamp(seekY - position.y, -spd, spd);
		position.x += dx;
		position.y += dy;
		movedFlag = true;
		eyesFrame = 4;
		frame += 2;

		// If player has exploded, make us disappear.
        if (player.getStatus() == S_VACANT)
            setStatus(S_VACANT);
	}

	private void doStuck() {
		// If hole has been filled in above us, fall.

		int startY = Board.startRowY(position.y);

		if (Board.calcHoleEnd(position) == startY) {
			fallStartRow = Board.startRow(position.y);

			VidGame.setStage(parent.GS_FALLING);
			setStatus(ST_FALL);
			Board.clearGhostHole(position);
			Sfx.play(parent.E_FALL);

			// Determine how far to fall.

			fallToY = Board.rowToWorld(fallStartRow - 1);
//			fallRows = 1;
//			db.pr(" fallStartRow="+fallStartRow+", fallRows init to 1");
//			fallToY = startY + Board.WORLD_FLOOR_YM;
            fallingSpider = this;
			fallFillY = Board.startRow(startY);	//worldToRow(startY) - 1;
//			db.pr(" set fallFillY to "+fallFillY);
			noGloat = false;

//			db.pr(" calculating fallToY, starting with "+(fallToY >> BEngine.FRACBITS) );
			while (true) {
				workPt.x = position.x;
				workPt.y = fallToY;
				int nextY = Board.calcHoleEnd(workPt, true);
//				db.pr("  calcHoleEnd for "+(workPt.y >> BEngine.FRACBITS)+" produced "+(nextY >> BEngine.FRACBITS) );
				if (nextY == fallToY) break;
				fallToY = nextY;
//				fallRows++;
//				db.pr(" we will be able to fall to "+nextY+", fallRows incr to "+fallRows);
			}
			fallRows = fallStartRow - Board.startRow(fallToY);

//			db.pr(" fallToY is "+(fallToY >> BEngine.FRACBITS) );

//			fallStartY = startY;
			fallScore = 0;
			return;
		}

		checkCollisions(0);
		if (getStatus() != ST_STUCK) return;

		if (VidGame.getStage() != parent.GS_FALLING) {
			if ((++stuckAnim & 3) == 0) {
				eyesFrame = MyMath.rnd(4);
			}

			stuckDelay -= VidGame.CYCLE;
			if (stuckDelay <= 0) {
				setStatus(ST_CLIMB);
				Board.setGhostClimb(position);
			}
		}
	}

//	private static int calcFallRows(int posY) {
//		return (posY + (Board.WORLD_FLOOR_YM>>1) - fallStartY) / Board.WORLD_FLOOR_YM;
//	}

	private void explode(int score) {
		SpriteExp.add(ims[spriteIndex], position, 1800, 0, 30, 30, 420);
        setStatus(S_VACANT);
//		onBoard = false;
//		active--;
		noGloat = true;
		if (score > 0) {
			VidGame.adjScore(score);
			// Plot score as well.
			ScoreObj.add(score, position, 1200);
		}
	}

	private void doClimb() {
		if (VidGame.getStage() != parent.GS_FALLING) {
            climbEffect = true;
			eyesFrame = Board.UP;
			mouthFrame = MTH_NORM;
			int seekY = Board.startRowY(position.y);

			int dist = position.y - seekY;
			int speed = Math.min(CLIMB_SPEED, dist);

//			dir = Board.UP;
			moveInDir(speed, Board.UP);
			dist -= speed;

			int hDepth = Board.worldToHoleDepth(dist);
			if (hDepth == Board.HOLE_DEPTH)
				hDepth = Board.HOLE_DEPTH-1;

			Board.mendHole(position, hDepth, true);

			if (dist == 0) {
				Board.clearGhostHole(position);
				setStatus(ST_NORM);

				if ((flags & FLG_WASSTUCK) != 0) {
					// On higher skill levels, promote ghost to higher type.
					if (VidGame.getLevel() >= 3) {
						if (type < TYPES-1)
							type++;
					}
					flags &= ~FLG_WASSTUCK;
				}
			}
		}
		checkCollisions(0);
	}

	private void doTaunt() {
		eyesFrame = 4;
		tauntTime -= VidGame.CYCLE;
		if (tauntTime <= 0) {
			setStatus(ST_NORM);
			VidGame.setStage(parent.GS_NORMAL);
			Sfx.stop(parent.E_GLOAT);
			mouthFrame = MTH_NORM;
		}

		// Make him look up and move his feet back and forth slowly.

		dir = Board.UP;
		if ((VidGame.getTime() & 1) == 0)
			frame++;
	}

	protected void moveOne() {
        int stage = VidGame.getStage();
        int status = getStatus();

		if (
			(stage == Panic.GS_INTRO || stage >= Panic.GS_FINISHLEVEL)
		 && status != ST_ATTACK
		 && status != ST_TAUNT
		) return;

		switch (status) {
		case ST_CLIMB:
			doClimb();
			return;
		case ST_STUCK:
			doStuck();
			return;
		case ST_TAUNT:
			doTaunt();
			return;
		case ST_ATTACK:
			doAttack();
			return;
		}

		int distMoved = 0;
		int speed = 0;

		int rval = MyMath.rnd(100);
		boolean filterReverse = true;
		reverseDelay -= VidGame.CYCLE;
		if (reverseDelay < 0) {
			reverseDelay = MyMath.rnd(12000) + 4000;
			filterReverse = false;
		}

		do {

			// Calculate possible moves

			int possMoves = (1 << Board.DOWN) | (1 << Board.FALL);
			if (status != ST_FALL) {
				possMoves = Board.detMoves(position, 0);

				// Check for collisions with other spiders.
				if (!Board.falling(possMoves)) {
					possMoves = checkCollisions(possMoves);
//					if (!onBoard) return;
				}

				if (
					VidGame.getStage() == parent.GS_FALLING
				 && !Board.falling(possMoves)
				) return;

				// Determine speed

				speed = FALL_SPEED;
				if (!Board.falling(possMoves)) {
                                  final int scaleFactors[] = {
                                    64,64,67,67,
                                    70,70,72,72,
                                    73,73,73,74,
                                    74,74,74,75,
                                    75,75,75,76,
                                    76,76,76,77,
                                    77,77,77,78
                                  };
                                  int scaleIndex = VidGame.getLevel();
                                  if (VidGame.getMode() == VidGame.MODE_PREGAME)
                                    scaleIndex = 0;

                                  speed = (SPEED_NORM * scaleFactors[Math.min(scaleIndex,28-1)]) >> 6;
				}


				// Determine desired direction of movement.

				int intel = smarts + (type << 4);
				{
					int desPoss = possMoves;
					if (filterReverse)
						desPoss = filterReverse(desPoss, dir);

					if (intel > rval)
						desPoss = seek(player.position, desPoss, 0);

					setDesiredDir(chooseRandomDir(desPoss));
				}
				eyesFrame = getDesiredDir();

				int step = speed - distMoved;
				if (step > 0) {
//					db.pr(" moving step of "+step+", possMoves="+possMoves);
//					db.pr("  current loc = "+position.x+","+position.y);
					step = moveOne(step, possMoves);
					distMoved += step;
//					db.pr("  adjusted distMoved by "+step+" to "+distMoved);
//					db.pr("  dest loc = "+position.x+","+position.y);
				}
			} else {
				mouthFrame = MTH_FALL;
				eyesFrame = Board.DOWN;
				speed = FALL_SPEED;
				int step = Math.min(fallToY - position.y, speed);
//				dir = Board.DOWN;
				moveInDir(step, Board.DOWN);
				distMoved = speed;

				// Fill in bricks as we fall through them, if we are the instigator.
				if (this == fallingSpider) {
					int currFillY = Board.startRow(position.y - BEngine.ONE * 18);	//worldToRow(position.y - BEngine.ONE * 18);
					if (currFillY < fallFillY) {
						fallFillY = currFillY;
//						db.pr(" mending hole, setting fallFillY to "+fallFillY);
						Board.mendHole(position, 0, false);
						Board.clearGhostHole(position);
						Sfx.play(parent.E_FALL);
					}
				}
			}


			// If falling, test whether to change status to stuck or climbing out.

			if (Board.falling(possMoves)) {
				int hEnd = fallToY;
				if (status != ST_FALL) {
					hEnd = Board.calcHoleEnd(position);
					int startY = Board.startRowY(position.y);

//					db.pr(" falling, hEnd = "+hEnd+", startY="+startY+", HOLE_DEPTH="+Board.HOLE_DEPTH);

					if (position.y >= startY + Board.HOLE_DEPTH) {
						position.y = startY + Board.HOLE_DEPTH;
						setStatus(ST_STUCK);
						Sfx.play(parent.E_STUCK);
						mouthFrame = MTH_STUCK;
						flags |= FLG_WASSTUCK;
						setStuckDelay();
						stuckAnim = 0;
						Board.setGhostStuck(position);
						break;
					}
				}

				if (position.y >= hEnd) {
					position.y = hEnd;
					if (status != ST_FALL) {
						setStatus(ST_CLIMB);
						Board.setGhostClimb(position);
					} else {
						// Maybe explode, maybe taunt him.
//						int fallRows = calcFallRows(position.y);
						if (fallRows >= (type + 1)) {
							fallScore += fallRows * typeScores[type];
							explode(this == fallingSpider ? fallScore : 0);
							Sfx.play(parent.E_EXP);
						} else {
							setStatus(ST_NORM);
						}
						if (this == fallingSpider) {
							VidGame.setStage(parent.GS_NORMAL);
							mouthFrame = MTH_NORM;
							if (!noGloat) {
								setStatus(ST_TAUNT);
								tauntTime = 1200;
								mouthFrame = MTH_GLAD;
								Sfx.play(parent.E_GLOAT);
							}
						}
					}
					break;
				}
			}
		} while (distMoved < speed);
		animate(8);

	}

/*
	private void setStatus(int s) {
//		db.pr("Status changing from "+status+" to "+s);
		status = s;
	}
*/

	private void setStuckDelay() {
		stuckDelay = Bonus.active(Bonus.TY_TIME) ? 15000 : 9000;
	}

	private static final int TYPES = 3;
	private static final int S_BODY = 0;
	private static final int S_EYES = S_BODY + (2 * TYPES);
	private static final int S_MOUTH = S_EYES + 5;
	private static final int S_TOTAL = S_MOUTH + (4 * TYPES);
	private static final int SPEED_NORM = BEngine.TICK * 16 * 50;

	private static final int typeScores[] = {100,200,300};

	private static Sprite ims[];

	private static final int MAX = 8;
//	private static Spider list[];
	private static Pt workPt;

	private static Panic parent;
	private static Player player;
//	private static int active;			// # active spiders
	private static int appearDelay;		// time to wait before bringing next one on
	private static int smarts;			// how intelligent spiders are for current level
	private static int formation;		// formation index for spider starting locations
	private static int types;			// type index level

	private static final int APPEAR_DEL = 1000/3;

	// Speed at which spider climbs out of hole:
	private static final int CLIMB_SPEED = BEngine.TICK * 16 * 10;

	private static final int FLG_WASSTUCK = 0x0001;

	private static int fallStartRow;	// row where spider started falling
	private static int fallToY;
//	private static int current;
//	private static int fallIndex;
    private static Spider fallingSpider;
	private static int fallFillY;		// last level where hole was auto-filled during fall
	private static int fallScore;
	private static int fallRows;

	private static boolean noGloat;

	private static final int MTH_NORM = 0;
	private static final int MTH_STUCK = 1;
	private static final int MTH_GLAD = 2;
	private static final int MTH_FALL = 3;

	private static final int ST_NORM	= 1;
	private static final int ST_STUCK	= 2;
	private static final int ST_CLIMB	= 3;
	private static final int ST_FALL	= 4;
	private static final int ST_TAUNT	= 5;
	private static final int ST_ATTACK	= 6;

    private static GameObjIterator iter;
    private static int activeCount;

	private int type;
	private int spriteIndex;
	private int reverseDelay;
	private int mouthFrame;
	private int eyesFrame;
	private int stuckDelay;		// if non-zero, delay before starting to climb out
	private int stuckAnim;
	private int tauntTime;
	private int flags;			// FLG_xxx
}
