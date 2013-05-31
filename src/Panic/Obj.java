package Panic;

import vgpackage.*;
import mytools.*;

public abstract class Obj extends GameObj {
	protected static final int FALL_SPEED = BEngine.TICK * 16 * 150;

	protected int getDesiredDir() {
		return desiredDir;
	}

	protected void setDesiredDir(int d) {
		desiredDir = d;
	}

	protected void setOnBoard() {
		lastMovedDir = -1;
		setDesiredDir(-1);
		dir = Board.RIGHT;
		resetAnim();
	}

	// Move object
	// Precondition:
	//	distance = maximum distance to move
	//	possMoves = possible directions of movement
	// Postcondition:
	//	returns distance moved; this may be less than the maximum distance, if the object
	//	has reached an intersection.  In this case, the distance moved should be subtracted
	//	from the original distance and this function called again.
	//	movedDir = direction of movement; -1 if not moved; FALL if falling
	protected int moveOne(int distance, int possMoves) {
		int newDir = -1;
		int desDir = getDesiredDir();

		movedDir = -1;

		// If falling, make sure we move DOWN.
		if (Board.falling(possMoves))
			desDir = Board.DOWN;

		if (desDir >= 0) {
			if (!Board.movePossible(desDir, possMoves))
				desDir = lastMovedDir;
		} else {
			// If not at an intersection, keep moving.
			if (lastMovedDir >= 0) {
				if (!Board.atIntersection(position, (lastMovedDir == Board.LEFT || lastMovedDir == Board.RIGHT))) {
					desDir = lastMovedDir;
				}
			}
		}

		// If we want to stop, or can no longer move in the desired direction,
		// act as if we moved the entire distance.
		if (desDir < 0 || !Board.movePossible(desDir, possMoves))
			return distance;

		int dist;
		if (Board.falling(possMoves)) {
			dist = Math.max(0, Board.calcHoleEnd(position) - position.y);
		} else
			dist = Board.distFromCell(position, desDir);

		dist = Math.min(distance, dist);

		if (VidGame.DEBUG) Debug.ASSERT(dist > 0, "dist = 0 in move()");

		dir = desDir;

		if (Board.falling(possMoves))
			movedDir = Board.FALL;
		else {
			movedDir = dir;
	  		lastMovedDir = desDir;
		}

		moveInDir(dist);

		return dist;
	}


	// Adjust the animation frame if the object has moved since the last call
	// Precondition:
	//	length = length of animated sequence (frame will wrap to zero at this value)
	// Postcondition:
	//	frame has been adjusted.
	protected void animate(int length) {
		if (movedFlag) {
			frame++;
			if (frame >= length)
				frame = 0;
		}
		movedFlag = false;
	}

	protected void resetAnim() {
		frame = 0;
		movedFlag = false;
	}

	protected void moveInDir(int distance) {
		moveInDir(distance, dir);
	}

	protected void moveInDir(int distance, int d) {
		position.x += Board.xMoves[d] * distance;
		position.y += Board.yMoves[d] * distance;
		movedFlag = true;
	}

	protected static int filterReverse(int moves, int cdir) {
		int fMoves = (moves & ~(1 << (cdir ^ 2)));
		if (fMoves != 0)
			return fMoves;
		return moves;
	}

	// Test if an object has reached or passed a particular position.
	// If so, places at that position and returns true.
	protected boolean reachedPosition(Pt prevLoc, Pt dest) {

		boolean result = false;

		Pt start = new Pt();
		Pt end = new Pt();

		boolean vertFlag;

		if (Math.abs(position.x - prevLoc.x) > Math.abs(position.y - prevLoc.y)) {
			// Mainly moving horizontally.
			start.set(prevLoc.x, (prevLoc.y + position.y) >> 1);
			end.set(position.x, start.y);
			vertFlag = false;
		} else {
			start.set((prevLoc.x + position.x) >> 1, prevLoc.y);
			end.set(start.x, position.y);
			vertFlag = true;
		}

		// Flip coordinates so we are dealing with x.

        if (vertFlag) {
			start.swap();
			end.swap();
        }

		// Exchange so start is always less.

		if (start.x > end.x) {
			start.swapWith(end);
        }

        if (
            start.x <= dest.x
         && end.x >= dest.x
         && Math.abs(start.y - dest.y) < Board.WORLD_CELL_XM / 4
        ) {
            result = true;
			dest.copyTo(position);
        }
        return result;
	}

	// Determine which moves will seek a particular position
	// Precondition:
	//	type =  0:normal
	//			1:a more rigorous method, produces more accurate but less varied results
	protected int seek(Pt desired, int moves, int type) {
        int code = 0;   /*  Two dimensional */

        if (type != 0) {
            int xD = Math.abs(position.x - desired.x);
            int yD = Math.abs(position.y - desired.y);
			if (xD > yD*4
			 && xD > Board.WORLD_CELL_XM * 3
			)
				code = 1;
			else if (yD > xD * 4
		 	 && yD > Board.WORLD_CELL_XM * 3
			)
				code = 2;
        }

		int newMoves = 0;
        if (code != 2) {
			if (position.x + Board.WORLD_CELL_XM < desired.x)
                newMoves |= 1 << Board.RIGHT;
            else if (position.x - Board.WORLD_CELL_XM > desired.x)
				newMoves |= 1 << Board.LEFT;
        }
        if (code != 1) {
			if (position.y + Board.WORLD_CELL_XM < desired.y)
                newMoves |= 1 << Board.DOWN;
            else if (position.y - Board.WORLD_CELL_XM > desired.y)
				newMoves |= 1 << Board.UP;
        }
		newMoves &= moves;

        if (newMoves == 0) {
			newMoves = moves;
            /*  Try again, but eliminate moves that are clearly wrong.  */
            if (code == 1) {
                if (position.x < desired.x)
					newMoves &= ~(1 << Board.LEFT);
				else if (position.x > desired.x)
					newMoves &= ~(1 << Board.RIGHT);
            } else if (code == 2) {
                if (position.y < desired.y)
					newMoves &= ~(1 << Board.UP);
				else if (position.y > desired.y)
					newMoves &= ~(1 << Board.DOWN);
            }
        }

        if (newMoves == 0)
			newMoves = moves;
		return newMoves;
	}

	// Choose a random direction
	protected static int chooseRandomDir(int moves) {
		final short orders[] = {
			(0<<6)|(3<<4)|(1<<2)|2,
			(1<<6)|(3<<4)|(0<<2)|2,
			(0<<6)|(3<<4)|(2<<2)|1,
			(2<<6)|(3<<4)|(0<<2)|1,
			(2<<6)|(3<<4)|(1<<2)|0,
			(1<<6)|(3<<4)|(2<<2)|0,
			(3<<6)|(0<<4)|(1<<2)|2,
			(3<<6)|(1<<4)|(0<<2)|2,
			(3<<6)|(0<<4)|(2<<2)|1,
			(3<<6)|(2<<4)|(0<<2)|1,
			(3<<6)|(2<<4)|(1<<2)|0,
			(3<<6)|(1<<4)|(2<<2)|0,
			(0<<6)|(1<<4)|(2<<2)|3,
			(1<<6)|(0<<4)|(2<<2)|3,
			(0<<6)|(2<<4)|(1<<2)|3,
			(2<<6)|(0<<4)|(1<<2)|3,
			(2<<6)|(1<<4)|(0<<2)|3,
			(1<<6)|(2<<4)|(0<<2)|3,
			(0<<6)|(1<<4)|(3<<2)|2,
			(1<<6)|(0<<4)|(3<<2)|2,
			(0<<6)|(2<<4)|(3<<2)|1,
			(2<<6)|(0<<4)|(3<<2)|1,
			(2<<6)|(1<<4)|(3<<2)|0,
			(1<<6)|(2<<4)|(3<<2)|0
		};
		int order = orders[MyMath.rnd(24)];

		if ((moves & (1|2|4|8)) == 0) return 0;

		while (true) {
			int dir = order & 3;
			if ((moves & (1 << dir)) != 0)
				return dir;
			order >>= 2;
		}
	}

	private int desiredDir;		// desired direction of movement; -1 if stop
	protected Pt position = new Pt();   // position in world
	protected int dir;

	protected int lastMovedDir;		// direction of last movement; -1 if undefined
	protected int movedDir;

	protected int frame;			// animation frame; incremented by animate()
	protected boolean movedFlag;	// true if moved since last call to animate()

}
