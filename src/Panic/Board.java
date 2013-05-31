package Panic;

import vgpackage.*;
import mytools.*;
import java.awt.*;

public final class Board {
/*
	// Constructor
	public Board() {
		setMaze();
	}
*/
	// Determine the amount of oxygen remaining
	public static int oxygen() {
		return oxygen;
	}

	// Reset the oxygen to the appropriate amount for the level
	public static void resetOxygen() {
		oxygenStart = Math.max(OXYGEN_MIN, OXYGEN_TOTAL - level * 3500);

		if (VidGame.DEBUG && false) oxygenStart = 25*1000;
		oxygen = oxygenStart;
	}

	// Initialize the class
	public static void init(Panic parent) {
		Board.parent = parent;
//		Board.ge = ge;
		Board.firstDemo = true;

		// Create an array to hold the brick states.
		Board.bricks = new byte[CELLS_WIDE * BRICK_LEVELS];
		Board.drawn = new byte[CELLS_WIDE * BRICK_LEVELS];

		Sprite s = new Sprite(parent.getSprite(), 0,0,84,104);
		Board.spr = new Sprite[S_TOTAL];
		int i = 0;
		int j = 0;
		final int data[] = {
			0,1,24,3,
			0,5,24,5,
			0,11,24,7,
			0,19,24,9,
			0,29,24,11,
			0,41,24,13,

			0,55,8,24,

			25,1,45,18,

			0,80,6,15,
			7,80,6,15,
			14,80,6,15,
		};

		for (i = 0; i < S_BRICKS; i++) {
			Board.spr[i] = new Sprite(s, data[j+0], data[j+1], data[j+2], data[j+3]);
			j += 4;
		}
		for (int k = 0; k < BRICK_TYPES; k++) {
			for (int n = 0; n < 4; n++) {
				Board.spr[i++] = new Sprite(s, 25 + (8 * n), 20 + 14 * k, 8, 13);
			}
		}
		for (int k = 0; k < BRICK_TYPES; k++) {
			for (int n = 0; n < 2; n++) {
				Board.spr[i++] = new Sprite(s, 59, 20 + 17 * k + 8 * n, 24, 8);
			}
		}

		colors = new Color[STAR_COLORS];
		final int starColors[] = {
	        250, 80, 40, 80, 250, 120, 40, 60, 250, 200,
	        200, 70, 70, 200, 200, 200, 70, 200, 160, 180,
	        230, 230, 160, 180
		};
		for (i = 0, j = 0; i < STAR_COLORS; i++, j+=3) {
			colors[i] = new Color(starColors[j+0],starColors[j+1],starColors[j+2]);
		}
      setMaze();
	}

    private static final byte board_0[] = {
		2,0,5,
		14,2,3,
		24,0,2,
		24,4,5,
		34,3,4,
		46,0,5,	-1
    };
    private static final byte board_1[] = {
		2,1,3,
		10,0,2,	10,4,5,
		22,3,4,
		30,0,1,	30,2,4,
		46,1,5,	-1
    };
    private static final byte board_2[] = {
		2,0,1,	2,3,5,
		10,1,2,
		18,2,3,
		26,0,2,	26,3,5,
		36,2,3,
		44,1,2,
		46,3,5,	-1
    };
	private static final byte board_3[] = {
		2,0,1,
		8,2,3,
//		18,3,4,
		18,0,2,
//		22,3,5,
                18,3,5,
                32,3,5,
//                28,3,5,
		32,0,2,
//		32,3,4,
		42,2,3,
                2,4,5,
                46,4,5,
		46,0,1,	-1
	};
	private static final byte board_4[] = {
		2,0,3,	2,4,5,
		10,0,3,
		20,2,5,
		30,0,4,
		38,4,5,
		44,0,4,	-1
	};
	private static final byte board_5[] = {
		2,0,2,	2,3,5,
		12,2,3,	12,4,5,
		20,3,4,
		28,1,2,
		36,0,1,	36,2,3,
		46,0,2,	46,3,5, -1
	};
	private static final byte board_6[] = {
		2,0,2,
		14,2,5,
		24,0,3,24,4,5,
		34,2,5,
		46,0,2,	-1
	};
	private static final byte board_7[] = {
		0,0,5,
		10,0,2,	10,4,5,
		18,2,3,
		26,2,3,
		34,3,4,
		38,0,1,
		42,3,4,
		48,0,5,	-1
	};

    private static final byte[] boards[] = {
        board_0,
        board_1,
        board_2,
        board_3,
        board_4,
        board_5,
        board_6,
        board_7,
    };

	// Initialize the board to an appropriate ladder/brick configuration for the level
	public static void setMaze() {

		level = VidGame.getLevel();
		resetOxygen();
		levelDrawn = 0;

		int maze = level % boards.length;
		if (VidGame.getMode() == VidGame.MODE_PREGAME) {
			if (!firstDemo)
				maze = MyMath.rnd(Math.min(4, boards.length));
			firstDemo = false;
		}

	    int i;

		// Clear bricks to solid
		for (i = 0; i < CELLS_WIDE * BRICK_LEVELS; i++)
			bricks[i] = (byte)BC_BRICK;

		byte[] script = boards[maze];

		i = 0;
		while (script[i] != -1) {
            int x = script[i+0] & ~1;
			int start_level = script[i+1];
			int end_level = script[i+2];
			i += 3;

			for (int j = start_level; j < end_level; j++) {
				for (int k = 0; k < HOLE_CELLS; k++)
					storeCell(x + k, j+1, BC_LADDER | (k << BCF_OBJSHIFT)); //k == 0 ? BC_LADDER : BC_LADDER2);
            }
        }
	}

	// Plot any changes that have occurred with the board
	public static void plotChanges() {

		if (level + 1 != levelDrawn)
            BEngine.invalidate();
//			valid = false;

        boolean valid = BEngine.layerValid();

		if (!valid) {
			BEngine.clearView();
			plotStars();
		}

		Bonus.plotStatus();

		int bType = level >> 2;
		if (bType >= BRICK_TYPES)
			bType = level % BRICK_TYPES;

		brickOffset = S_BRICKS + bType * 4;
		ladderOffset = S_LADDERS + bType * 2;

		levelDrawn = level + 1;

		for (int r = 1; r <= BRICK_LEVELS; r++) {
			int bo = (r - 1) * CELLS_WIDE;
			int sy = VIEW_FLOOR_Y - (r * VIEW_FLOOR_YM);

			for (int x = 0; x < CELLS_WIDE; x++) {

				int c = bricks[bo + x];
				if (!valid || drawn[bo + x] != c) {

					int sx = x * VIEW_CELL_XM;

					drawn[bo + x] = (byte)c;

					int c2 = c & BCF_CONTENTS;

					if (c2 <= BC_BRICK) {
						BEngine.drawSprite(spr[brickOffset + (x & 3)], sx, sy);
						continue;
					}

					// It's a hole.
					BEngine.drawSprite(spr[brickOffset + (x & 3)], sx, sy);

					// If it's the last part of the hole, plot
					// the hole bitmap to the left.
					if ((c & BCF_OBJOFFSET) == ((HOLE_CELLS-1) << BCF_OBJSHIFT))
						BEngine.drawSprite(spr[S_HOLES + (c2 - BC_HOLE)],
							sx - (HOLE_CELLS - 1) * VIEW_CELL_XM, sy);
				}
			}
		}

		// If entire view is invalid, draw the ladders.

		if (!valid) {
			{
				for (int x = 0; x < CELLS_WIDE; x++)
					BEngine.drawSprite(spr[S_FLOOR], x * VIEW_CELL_XM, VIEW_FLOOR_Y);
				BEngine.drawSprite(spr[S_AIR], 4, VIEW_FLOOR_Y + 4);
			}

			for (int r = 1; r <= BRICK_LEVELS; r++) {
				int bo = (r - 1) * CELLS_WIDE;
				for (int x = 0; x < CELLS_WIDE; x++) {
					int c = (bricks[bo + x]);	// & BCF_CONTENTS;
					if (c != BC_LADDER) continue;

//					db.pr("ladder found at row "+r+", x ="+x);

					// Is this the top of the ladder?

					if (
						r < BRICK_LEVELS
					 && (bricks[bo + CELLS_WIDE + x] /* & BCF_CONTENTS*/) == BC_LADDER
					) {
						continue;
					}

					 // Determine how far down the ladder goes.

					int length = 1;
					for (int r2 = r - 1; r2 > 0; r2--, length++) {
						if ((bricks[(r2-1) * CELLS_WIDE + x] /*& BCF_CONTENTS*/) != BC_LADDER) break;
					}

					int yCell = (r * CELLS_SEP_ROWS) + LADDER_EXTRA;
					int yCellEnd = (r - length) * CELLS_SEP_ROWS + 1;

					int sx = x * VIEW_CELL_XM;
					int sy = VIEW_FLOOR_Y - yCell * VIEW_CELL_YM;

					while (yCell >= yCellEnd) {
						BEngine.drawSprite(spr[ladderOffset + (yCell & 1)], sx, sy);
						yCell--;
						sy += VIEW_CELL_YM;
					}
				}
			}
		}

		updateAirMeter();
	}

	// Determine how far we have to move in a particular direction
	// before hitting an intersection of cells.
	// Precondition:
	//	pos = location in world
	//	dir = direction of movement
	public static int distFromCell(Pt pos, int dir) {

		// Calculate the distance to the next intersection in the current direction

		int coord = 0;

		switch (dir) {
		case UP:
			coord = pos.y - WORLD_TOP;
			break;
		case DOWN:
			coord = WORLD_TOP - pos.y;
			break;
		case RIGHT:
			coord = -pos.x;
			break;
		default:	// LEFT
			coord = pos.x;
			break;
		}
		int dist = MyMath.mod(coord, WORLD_CELL_SIZE);
		if (dist == 0)
			dist = WORLD_CELL_SIZE;
		return dist;
	}

	//  horzFlag = true if restricting horizontal stop points to odd-numbered cells
	public static boolean atIntersection(Pt pos, boolean horzFlag) {
		// Don't allow player to stop at even-numbered cells.  This makes lining up the
		// holes a little easier.
		if (MyMath.mod((pos.y - WORLD_TOP), WORLD_CELL_YM) != 0) return false;
		if (horzFlag)
			return (MyMath.mod(pos.x, (WORLD_CELL_XM << 1)) == WORLD_CELL_XM);
		else
			return (MyMath.mod(pos.x, WORLD_CELL_XM) == 0);
	}

	public static boolean falling(int possMoves) {
		return (possMoves & (1 << FALL)) != 0;
	}

	public static int holeDepthToWorld(int holeDepth) {
		if (holeDepth == HOLE_STEPS)
			return HOLE_DEPTH;
		if (holeDepth == 0)
			return 0;

		return ((holeDepth << 1) + 1) << BEngine.FRACBITS;
	}

	public static int worldToHoleDepth(int ht) {
		int depth = ((ht >> BEngine.FRACBITS) - 1) >> 1;
		depth = MyMath.clamp(depth, 0, HOLE_STEPS);
		return depth;
	}

	// Calculate where a hole ends, given position of object
	public static int calcHoleEnd(Pt pos) {
		return calcHoleEnd(pos, false);
	}

	// Calculate where a hole ends, given position of object
	// Precondition:
	//	pos = position of object
	//	align = true if holes must be aligned with object to fall into them
	public static int calcHoleEnd(Pt pos, boolean align) {
		int cy = startRow(pos.y);
		int cx = pos.x / WORLD_CELL_XM;

		int contents = BC_BRICK;
		if (cy > 0) {
			contents = getCell(cx,cy);
			if (
				((contents & BCF_CONTENTS) < BC_HOLE)
			|| (contents & BCF_OBJOFFSET) != 0
			)
				contents = BC_BRICK;
		}

		return calcHoleEnd(cy, contents);
	}

	public static int offInX(int worldX) {
		if (worldX < 0)
			return -worldX;
		if (worldX >= (CELLS_WIDE * WORLD_CELL_XM))
			return (worldX + 1 - (CELLS_WIDE * WORLD_CELL_XM));
		return 0;
	}

	// Determine what moves are available from a location
	// Precondition:
	//	pos = location, in world coordinates
	//	objCode =	0: spider
	//				1: human
	//				2: bonus
	// Postcondition:
	//	int returned, with bits for possible movement:
	//		0:	up
	//		1:	right
	//		2:	down
	//		3:	left
	//		4:  fall (if hole)
	//	sets holeEnd = bottom of current hole, if falling; else 0
	public static int detMoves(Pt pos, int objCode) {
//		db.pr("detMoves pos="+pos);

		int moves = 0;
		int contents;

		int ry = pos.y - WORLD_TOP;
		int cy = startRow(pos.y);

		int cx = pos.x / WORLD_CELL_XM;

		int xRem = MyMath.mod(pos.x, WORLD_CELL_XM);
		int yRem = MyMath.mod(ry, WORLD_FLOOR_YM);

		if (yRem != 0) {

			// If there is not a ladder extending down from above, fall.

			contents = getCell(cx,cy);

			if (contents == BC_LADDER)
				return (1 << UP) | (1 << DOWN);

			// Determine how far we can fall.

			// If bonus object, act as if empty hole.
			if (objCode == 2)
				contents = BC_HOLEC;

			int holeEnd = calcHoleEnd(cy, contents);

			if (pos.y < holeEnd)
				return (1 << DOWN) | (1 << FALL);
			return (1 << FALL);	// We're falling, but cannot move down further.
		}

		if (xRem != 0)
			return (1 << LEFT) | (1 << RIGHT);

		// Examine what is under our feet.

		contents = BC_BRICK;
		if (cy > 0)
			contents = getCell(cx,cy);
//		db.pr(" contents="+contents);

		// Special case for bonus object:  don't fall into occupied hole or partially dug
		// hole.

		if (
			objCode == 2
		 && contents > BC_LADDER
		 && contents != BC_HOLEC
		)
			contents = BC_BRICK;

		// Is there a hole?  If so, we can only fall.

		if (
			(contents & BCF_CONTENTS) >= BC_HOLE
		 && (contents & BCF_OBJOFFSET) == 0
		) {

			// Determine how far we can fall.

			int holeEnd = calcHoleEnd(pos.y, contents);
			if (pos.y < holeEnd)
				return (1 << DOWN) | (1 << FALL);
			return 0;
		}

		// Is there a ladder?
		if (contents == BC_LADDER) {
			moves |= (1 << DOWN);
		}

		int contents2 = BC_BRICK;
		if (cy < BRICK_LEVELS)
			contents2 = getCell(cx, cy + 1);
		if (contents2 == BC_LADDER)	//(contents2 & BCF_CONTENTS) == BC_LADDER)
			moves |= (1 << UP);

		// Determine if we can move left & right.
		// We cannot be too close to
		//		[] left edge
		//		[] partial hole or hole with alien in it

		for (int dir = 0; dir < 2; dir++) {
			if (dir == 0 && cx == 0) continue;
			if (dir == 1 && cx >= CELLS_WIDE - OBJ_CELLS) continue;

			int tx = cx + (dir == 0 ? -1 : OBJ_CELLS);
			contents2 = BC_BRICK;
			if (cy > 0)
				contents2 = getCell(tx, cy);

			if (
				(contents2 & (BCF_GHOSTSTUCK|BCF_GHOSTCLIMB)) == 0
			 && ((objCode != 1) || (contents2 & BCF_CONTENTS) < BC_HOLE)
			)
				moves |= (dir == 0) ? (1 << LEFT) : (1 << RIGHT);

			// If it's a complete hole, and no aliens, check for status of floors below.

			boolean allowMovement = false;

			if ((contents2 & ~BCF_OBJOFFSET) == BC_HOLEC) {
				allowMovement = true;
				if (objCode == 1) // Only perform this test for humans
				 for (int ny = cy-1; ; ny--) {
					int contents3 = BC_BRICK;
					if (ny > 0)
						contents3 = getCell(tx, ny);

					// If it's hole and it's aligned with ours, make sure
					// it is empty.
					if (
						(contents3 & BCF_CONTENTS) >= BC_HOLE
					 && ((contents3 ^ contents2) & BCF_OBJOFFSET) == 0
					) {
						if ((contents3 & ~BCF_OBJOFFSET) != BC_HOLEC) {
							allowMovement = false;
							break;
						}
					} else
						break;
				}
			}
			if (allowMovement)
				moves |= (dir == 0) ? (1 << LEFT) : (1 << RIGHT);
		}
//		db.pr(" returning moves "+moves);
		return moves;
	}

	public static boolean movePossible(int dir, int dirFlags) {
		return ((1 << dir) & dirFlags) != 0;
	}

	public static void worldToView(int cx, int cy, Pt loc) {
		loc.x = ((cx + OBJ_H_CENTER) >> BEngine.FRACBITS);
		loc.y = (cy >> BEngine.FRACBITS);
	}

	public static int digActionPossible(Pt pos, int side) {
//		boolean pr = false;
//		if (VidGame.DEBUG && ((VidGame.getTime() & 0x1f) == 0))
//			pr = true;

		if (!atIntersection(pos, true)) return 0;

//		int row = (WORLD_FLOOR_Y - pos.y) / WORLD_FLOOR_YM;
		int row = startRow(pos.y);	//worldToRow(pos.y);
//		return (WORLD_FLOOR_Y - y) / WORLD_FLOOR_YM;
		if (row == 0) return 0;

		int x = (pos.x / WORLD_CELL_XM) + (side == 0 ? -HOLE_CELLS : OBJ_CELLS);

		// Test that every brick in the hole is within range and has
		// appropriate contents.

		int contents = BC_BRICK;
		for (int j = 0; j < HOLE_CELLS; j++) {
			int c = BC_BRICK;
			int nx = x + j;

			if (nx < 0 || nx >= CELLS_WIDE) {
				if (j < 0) continue;

				return 0;
			}

			c = getCell(nx, row);

			if (j == 0)
				contents = c;

			if ((contents & BCF_CONTENTS) >= BC_HOLE) {
				if (((c & BCF_OBJOFFSET) >> BCF_OBJSHIFT) != j) {
//					if (pr) db.pr("BCF_OBJOFFSET "+((c & BCF_OBJOFFSET) >> BCF_OBJSHIFT)+" doesn't equal "+j);
					return 0;
				}
			} else {
				if (c != BC_BRICK) {
//					if (pr) db.pr(" c "+c+" != BC_BRICK");
					return 0;
				}
			}

			// If not top row, see if ladder extending down from row above.
			if (
				row < BRICK_LEVELS
			 && (getCell(nx, row + 1) & BCF_CONTENTS) == BC_LADDER	//<= BC_LADDER2
			) return 0;
		}
		if (contents == BC_BRICK) return 1;
		if ((contents & BCF_GHOSTCLIMB) != 0) {
//			if (pr) db.pr(" GHOSTCLIMB set");
			return 0;
		}
		if ((contents & BCF_GHOSTSTUCK) != 0) return 2;
		if (contents == BC_HOLEC) return 2;
		return (1|2);
	}

	public static void processDigAction(Pt pos, int side, int action, int speed) {
//		int row = (WORLD_FLOOR_Y - pos.y) / WORLD_FLOOR_YM;
		int row = startRow(pos.y);	//worldToRow(pos.y);
		//return (WORLD_FLOOR_Y - y) / WORLD_FLOOR_YM;
		int x = (pos.x / WORLD_CELL_XM) + (side == 0 ? -HOLE_CELLS : OBJ_CELLS);
		int contents = getCell(x, row);
		int depth = (contents & BCF_CONTENTS) - BC_BRICK;
		if (action == 2)
			speed = -speed;
		depth += speed;
		depth = MyMath.clamp(depth, 0, HOLE_STEPS);
		for (int j = 0; j < HOLE_CELLS; j++) {
			int nc = BC_BRICK;
			if (depth > 0)
				nc += (depth + (j << BCF_OBJSHIFT));
//			db.pr(" dig action, setting "+(x+j)+", row "+row+" to "+ ((contents & ~BCF_CONTENTS) | nc));
			storeCell(x+j, row, (contents & ~BCF_CONTENTS) | nc);
		}
	}

	// Mend a hole for a spider climbing out
	// Precondition:
	//	pos = position of spider
	//	depth = depth of hole (0...HOLE_STEPS)
	//	climbing = true if ghost is climbing out of hole
	// Postcondition:
	//	stores appropriate values in cells, with GHOSTCLIMB if hole not completely mended
	public static void mendHole(Pt pos, int depth, boolean climbing) {
		// Add 1 to pos.y in case he has just climbed out of the hole.  We still want
		// it to reference this row, not the one above.
		int row = startRow(pos.y);
		int x = (pos.x / WORLD_CELL_XM);

		int c2 = BC_BRICK;

//		db.pr("mending hole, row="+row+", x="+x);

		for (int j = 0; j < HOLE_CELLS; j++) {
			if (depth > 0) {
				c2 = (BC_BRICK + depth) | (j << BCF_OBJSHIFT);
			}
//			db.pr(" replacing cell "+(x+j)+" "+getCell(x+j,row)+" with "+c2);
			storeCell(x + j, row, c2 | (climbing ? BCF_GHOSTCLIMB : 0));
		}
	}

	public static void clearGhostHole(Pt pos) {
		setGhostHoleData(pos, 0);
	}

	public static void setGhostStuck(Pt pos) {
		setGhostHoleData(pos, BCF_GHOSTSTUCK);
	}

	public static void setGhostClimb(Pt pos) {
		setGhostHoleData(pos, BCF_GHOSTCLIMB);
	}

	public static int rowToWorld(int row) {
		return WORLD_FLOOR_Y - WORLD_FLOOR_YM * row;
	}

	public static int startRow(int y) {
		return (WORLD_FLOOR_Y - y + WORLD_FLOOR_YM - 1) / WORLD_FLOOR_YM;
	}


	public static int startRowY(int y) {
		return y - (y - WORLD_TOP) % WORLD_FLOOR_YM;
	}

	public static boolean reduceOxygen(int amount) {
		oxygen = Math.max(0, oxygen - amount);
		return (oxygen == 0);
	}

	// Modify brick contents
	// Precondition:
	//	row = level (1...BRICK_LEVELS) (note that first level has no bricks, and nothing
	//			should be stored there)
	//	x = horizontal cell index
	//	contents = contents to store there
	private static void storeCell(int x, int row, int contents) {
		bricks[(row - 1) * CELLS_WIDE + x] = (byte)contents;
	}

	// Calculate where a hole ends.
	// If hole is complete, returns y-coordinate of next row.  Otherwise,
	// returns y-coordinate of bottom of hole.
	private static int calcHoleEnd(int row, int contents) {
		int y = rowToWorld(row);
		int depth = (contents & BCF_CONTENTS) - BC_BRICK;

		if (depth == HOLE_STEPS)
			y += WORLD_FLOOR_YM;
		else
			y += holeDepthToWorld(depth);
		return y;
	}

	private static final int AIR_BAR_SPACING = 4;
	private static final int AIR_BARS = (Panic.MAIN_SCRN_XM - 60) / AIR_BAR_SPACING;

	private static void updateAirMeter() {
        boolean valid = BEngine.layerValid();
		// Determine how many bars to draw.
		int total = (oxygen * AIR_BARS) / oxygenStart;
		int critical = (OXY_LOW * total) / oxygenStart;
		if (oxygen > 0 && total == 0)
			total = 1;

		if (!valid)
			prevOxyDrawn = 0;

		int startBar = Math.min(prevOxyDrawn, total);
		int endBar = Math.max(prevOxyDrawn, total);

		int x = 50 + AIR_BAR_SPACING * startBar;
		while (startBar < endBar) {
			int s = S_METERE;
			if (startBar < total)
				s = (startBar < critical /*AIR_CRITICAL*/) ? S_METER1 : S_METER0;
			BEngine.drawSprite(spr[s], x, VIEW_FLOOR_Y + 6);
			x += AIR_BAR_SPACING;

			startBar++;
		}
		prevOxyDrawn = total;
	}

	private static void plotStars() {
        Graphics g = BEngine.getGraphics();
		int i;
		int j = 0;
		for (i = 0; i < MAX_STARS; i++) {
			int x = MyMath.rnd(BEngine.viewR.width) + BEngine.viewR.x;
			int y = MyMath.rnd(BEngine.viewR.height) + BEngine.viewR.y;
			g.setColor(colors[i & (STAR_COLORS-1)]);
			if (j == 0) {
				g.drawLine(x-1,y,x+1,y);
				g.drawLine(x,y-1,x,y+1);
			} else
				g.drawLine(x,y,x,y);
			if (++j == 5) j = 0;
		}
	}

	// Set BCF_GHOSTSTUCK, BCF_GHOSTCLIMB bits for a hole
	// Precondition:
	//	pos = position of ghost
	//	flags = flags to store
	private static void setGhostHoleData(Pt pos, int flags) {

		int row = startRow(pos.y);
		int x = pos.x / WORLD_CELL_XM;

		for (int j = 0; j < HOLE_CELLS; j++) {
			int c = getCell(x+j, row);
//			db.pr(" setting ghost flags "+flags+" for cell "+(x+j)+", row "+row);
			storeCell(x+j, row, (c & ~(BCF_GHOSTSTUCK|BCF_GHOSTCLIMB)) | flags);
		}
	}

	// Determine brick contents
	// Precondition:
	//	row = level (1...BRICK_LEVELS)
	//	x = horizontal cell index
	private static int getCell(int x, int row) {
		return bricks[(row - 1) * CELLS_WIDE + x];
	}


	// Directions of movement:
	public static final int UP = 0;
	public static final int RIGHT = 1;
	public static final int DOWN = 2;
	public static final int LEFT = 3;
	public static final int FALL = 4;

	public static final int OXY_LOW = 15000;
   private static final int OXYGEN_MIN = 1000 * 80;

	// Depth of hole (this is how far a spider falls to become stuck):
	public static final int HOLE_DEPTH = BEngine.ONE * 16;

	// Direction vectors.  0=up 1=right 2=down 3=left
	public static final int yMoves[] = {-1,0,1,0};
	public static final int xMoves[] = {0,1,0,-1};

	private static Panic parent;
	private static Sprite spr[];

	private static int level;				// the current level
	private static int levelDrawn;			// the last level that was drawn
	private static boolean firstDemo;		// true if first time showing demo mode,
											//	to avoid changing the displayed maze
											//	too quickly

	private static final int VIEW_CELL_SIZE = 8;
	public static final int BRICK_LEVELS = 5;		// # of brick platforms
	private static final int CELLS_WIDE = Panic.MAIN_SCRN_XM / VIEW_CELL_SIZE;
													// # cells across
	private static final int HOLE_STEPS = 6;		// # discreet hole sizes
	private static final int CELLS_SEP_ROWS = 9;	// height of each platform, in cells
	private static final int HOLE_CELLS = 3;		// # cells each hole or ladder occupies
	private static final int LADDER_EXTRA = 1;		// # cells ladder extends above top of brick
	private static final int OBJ_CELLS = HOLE_CELLS;	// width of spider, player

	private static final int BRICK_TYPES = 5;		// # different brick designs
	private static int brickOffset, ladderOffset;	// sprite indices for current brick design

	private static final int VIEW_CELL_XM = VIEW_CELL_SIZE;
	private static final int VIEW_CELL_YM = VIEW_CELL_SIZE;
	private static final int VIEW_FLOOR_YM = CELLS_SEP_ROWS * VIEW_CELL_YM;
	private static final int VIEW_FLOOR_Y = 410;
	public static final int WORLD_CELL_SIZE = VIEW_CELL_SIZE * VidGame.ONE;
	public static final int WORLD_CELL_XM = WORLD_CELL_SIZE;
	public static final int WORLD_CELL_YM = WORLD_CELL_SIZE;
	private static final int WORLD_FLOOR_YM = VIEW_FLOOR_YM * VidGame.ONE;
	public static final int WORLD_FLOOR_Y = VIEW_FLOOR_Y * VidGame.ONE;
	private static final int WORLD_TOP = WORLD_FLOOR_Y - (WORLD_FLOOR_YM * BRICK_LEVELS);
	public static final int OBJ_H_CENTER = (OBJ_CELLS * WORLD_CELL_XM) / 2;


	// Brick contents
   private static byte bricks[];					// Brick data (BCF_x)
	private static byte drawn[];					// Brick data that was last drawn

	// Brick content bits:
	private static final int BCF_CONTENTS = 0x07;	// BC_x
	private static final int BC_LADDER = 0;			// brick with ladder
	private static final int BC_BRICK = 1;			// solid brick
	private static final int BC_HOLE = 2;			// (+0...HOLE_STEPS-1) for partial hole
	private static final int BC_HOLEC = (BC_HOLE + HOLE_STEPS - 1);		// completed hole

	//	Horizontal offset for cell within a ladder or hole (2 bits, for 0..2):
	private static final int BCF_OBJSHIFT = 3;
	private static final int BCF_OBJOFFSET = (0x03 << BCF_OBJSHIFT);

	private static final int BCF_GHOSTSTUCK = 0x20;	// Ghost is fallen into hole
	private static final int BCF_GHOSTCLIMB = 0x40;	// Ghost is climbing out of hole

	// Sprite indices
	private static final int S_HOLES = 0;
	private static final int S_FLOOR = 6;
	private static final int S_AIR = 7;
	private static final int S_METER0 = 8;
	private static final int S_METER1 = 9;
	private static final int S_METERE = 10;
	private static final int S_BRICKS = 11;
	private static final int S_LADDERS = S_BRICKS + (4 * BRICK_TYPES);
	private static final int S_TOTAL = S_LADDERS + (2 * BRICK_TYPES);

	// Air gauge
	private static int oxygen;
	private static int oxygenStart;
	private static final int OXYGEN_TOTAL = 1000 * 120; // was 180;
	private static int prevOxyDrawn;

	// Starry background
	private static final int MAX_STARS = 120;
	private static final int STAR_COLORS = 8;  // must be power of 2
	private static Color colors[];
}
