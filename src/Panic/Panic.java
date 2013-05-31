package Panic;

import vgpackage.*;
import mytools.*;
import java.awt.*;

public class Panic extends java.applet.Applet
   implements Runnable, VidGameInt {

	private static final int APPLET_SCRN_XM = 628;
	private static final int APPLET_SCRN_YM = 434;

	public static final int MAIN_SCRN_XM = 408;
	public static final int MAIN_SCRN_YM = APPLET_SCRN_YM;
	public static final int MAIN_WORLD_XM = (MAIN_SCRN_XM * BEngine.ONE);
	public static final int MAIN_WORLD_YM = (MAIN_SCRN_YM * BEngine.ONE);

	public static final int VIEW_STATUS = 1;
	public static final int VIEW_MAIN = 2;

	public static final int E_DIG = 0;
	public static final int E_FILL = 1;
	public static final int E_FALL = 2;
	public static final int E_EXP = 3;
	public static final int E_GOTBONUS = 4;
	public static final int E_BMOVE = 5;
	public static final int E_PDEATH = 6;
	public static final int E_LEVEL = 7;
	public static final int E_LIFE = 8;
	public static final int E_STUCK = 9;
	public static final int E_APPEAR = 10;
	public static final int E_GLOAT = 11;
	public static final int E_ATTACK = 12;
	public static final int E_SCLIMB = 13;
	public static final int E_OXY = 14;
	public static final int E_GASP = 15;
	public static final int E_RUNA = 16;
	public static final int E_RUNB = 17;
	public static final int E_TOTAL = 18;

	// This checksum is for copy protection.
	// It's calculated from the URL of the applet.
	// See Spider.init().
	public static final int CSUM = 8391;	// qbert webpage

	public static final int GS_INTRO = 0;		// introducing new level
	public static final int GS_NORMAL = 1;		// running around
	public static final int GS_FALLING = 2;		// alien is falling
	public static final int GS_FINISHLEVEL = 3;	// finishing level
	public static final int GS_DYING = 4;		// dying

	// ===================================
	// Applet interface
	// ===================================
	public void init() {

		VidGame.doInit(this);
		VidGame.setBonusScore(2500);
		VidGame.setHighScore(2000);

		BEngine.open();

		sprite = new Sprite("imgmap");

		Board.init(this /*,ge*/);

//		Obj.init(this /*, ge*/);//, Board);

//		player = new Player(this);
        Player.init();

		Spider.init(this);//, player);
		Bonus.init(this);//, player);
		ScoreObj.init(this);
		SpriteExp.init();

		charSet0 = new CharSet(new Sprite(sprite,0,167,92,13),
         8,12,1,1,"0123456789");
		charSet1 = new CharSet(new Sprite(sprite,1,182,192,72),
         15,17,1,1,"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ.,!<>^~:%()?");
		charSet1.setSpacingX(-4);
		scorePnl = new ScorePnl(this);//, player);
	}

	public void start() {
		Sfx.open(sfxNames);
		VidGame.setBonusSfx(E_LIFE);

		VidGame.doStart();
	}

	public void run() {
		VidGame.doRun();
	}

	public void stop() {
		VidGame.doStop();
		Sfx.close();
	}

	public void destroy() {
		BEngine.close();
		VidGame.doDestroy();
	}

	// ===================================
	// VidGameInt interface
	// ===================================
	public void processLogic() {
		updateStage();
		if (
            VidGame.stageStart(GS_INTRO)
         && VidGame.getMode() == VidGame.MODE_PLAYING
        )
   			Board.setMaze();

        Player.move();
		Spider.move();
		Bonus.move();
		ScoreObj.move();
		SpriteExp.move();		// should be done last.

		if (!Spider.levelCompleted()
         && !VidGame.getAdvanceLevelFlag()
        ) {
			spidersGoneTime = 0;
		} else {
			spidersGoneTime += VidGame.CYCLE;
			if (!Bonus.active() || spidersGoneTime > 5000
             || VidGame.getAdvanceLevelFlag()
            ) {
				VidGame.setStage(GS_FINISHLEVEL);
                if (!VidGame.getAdvanceLevelFlag())
  				  Sfx.play(E_LEVEL, 1000, 0);
			}
		}
	}

	private void updateStage() {
//        Debug.print("updateStage mode="+VidGame.getMode()+", time="+VidGame.getTime() );
		if (VidGame.initFlag()) {
            VidGame.initStage(GS_INTRO);
			VidGame.adjustLives(-1);

            Player.prepare(GameObj.GAME);
            Spider.prepare(GameObj.GAME);
            Bonus.prepare(GameObj.GAME);
            ScoreObj.prepare(GameObj.GAME);
		}

        int stage = VidGame.getStage();
        int stageTime = VidGame.getStageTime();

//        Debug.print("stage="+stage+", time="+stageTime);
		switch (stage) {

         case GS_INTRO:
            if (stageTime == 0) {
                Player.prepare(GameObj.LEVEL);
                Spider.prepare(GameObj.LEVEL);
                Bonus.prepare(GameObj.LEVEL);
                ScoreObj.prepare(GameObj.LEVEL);
            }
            break;

         case GS_NORMAL:
			if (
				VidGame.getMode() == VidGame.MODE_PREGAME
			 && stage == GS_NORMAL
			 && stageTime > 20000
			) {
				VidGame.setStage(GS_INTRO);
				Board.setMaze();
			}
            break;

         case GS_DYING:
            if (stageTime > 4000) {
				Bonus.disable();
				if (VidGame.getLives() == 0) {
					VidGame.setMode(VidGame.MODE_GAMEOVER);
				} else {
					VidGame.adjustLives(-1);
					VidGame.setStage(GS_INTRO);
				}
			}
			if (stageTime > 8000) {
				VidGame.setStage(GS_INTRO);
			}
            break;

		 case GS_FINISHLEVEL:
		 	{
            if (!VidGame.getAdvanceLevelFlag()) {
             if (stageTime < 1400) {
                     finishTime = 0;
                     break;
             }

             int currOxy = Math.min(Board.oxygen(), VidGame.CYCLE * 50);

             VidGame.adjScore(currOxy >> 9);

             if (Board.reduceOxygen(currOxy) || VidGame.getAdvanceLevelFlag()) {
                     finishTime += VidGame.CYCLE;
                     Sfx.stop(E_OXY);
             } else {
                     Sfx.play(E_OXY);
             }
           }

			 	if (finishTime > 500 || VidGame.getAdvanceLevelFlag()) {
               VidGame.clearAdvanceLevelFlag();
					VidGame.adjLevel(1);
					VidGame.setStage(GS_INTRO);
				}
			}
			break;
        }

        VidGame.updateStage();
	}
	private static int finishTime;
	private static int spidersGoneTime;

	// ===================================

   public static Sprite getSprite() {
      return sprite;
   }

   public static CharSet getCharSet(int i) {
      return (i == 1) ? charSet1 : charSet0;
   }

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		if (!VidGame.beginPaint()) return;

		// Prepare for update.  Constructs offscreen buffers if required.
		BEngine.prepareUpdate();

		BEngine.openLayer(BEngine.L_BGND);
		plotBgnd();
        BEngine.closeLayer();

		// Process sprite layer

		BEngine.openLayer(BEngine.L_SPRITE);
		BEngine.selectView(VIEW_MAIN);
		BEngine.erase();

		plotSprites();
        BEngine.closeLayer();

		// Update the screen
		BEngine.updateScreen(g);

		VidGame.endPaint();
	}

	private void plotBgnd() {
		boolean valid = BEngine.layerValid();

		if (!valid) {
			if (VidGame.DEBUG)
				BEngine.clearView(Color.red);
			BEngine.defineView(VIEW_MAIN, 0, 0, MAIN_SCRN_XM, MAIN_SCRN_YM);
			BEngine.defineView(VIEW_STATUS, MAIN_SCRN_XM, 0,
            APPLET_SCRN_XM - MAIN_SCRN_XM, APPLET_SCRN_YM);
		}

		BEngine.selectView(VIEW_STATUS);
		scorePnl.plotChanges();
		BEngine.selectView(VIEW_MAIN);
		Board.plotChanges();
	}

	private void plotSprites() {
		BEngine.selectView(VIEW_MAIN);
		SpriteExp.draw();
        Player.draw();
		Spider.draw();
		Bonus.draw();
		ScoreObj.draw();
	}

	private static Sprite sprite;
	private static ScorePnl scorePnl;
	private static CharSet charSet0, charSet1;

	private static final String sfxNames[] = {
		"dig",		// digging a hole
		"fill",		// filling a hole in
		"fall",		// spider falling through a level
		"exp",		// spider exploding
		"gotbonus",	// picked up a bonus object
		"bmove",	   // bonus object bouncing around
		"pdeath",	// player exploding
		"level",	   // finished a level
		"life",		// awarded a bonus life
		"stuck",	   // spider just fell into a hole
		"appear",	// spider appearing on board at start of level
		"*gloat",	// spider gloating after surviving a fall
		"*attack",	// spider has jumped on player
		"*sclimb",	// spider is climbing out of a hole
		"*oxy",		// bonus points awarded for remaining oxygen
		"*gasp",		// player is running out of oxygen
		"*runa",		// running or climbing, normal speed
		"*runb",		// running or climbing, fast speed
	};
}
