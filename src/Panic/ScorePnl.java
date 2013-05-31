package Panic;

import vgpackage.*;
import java.awt.*;

public class ScorePnl extends Scoreboard {

	private Panic parent;
	private Sprite spr[];

	private final int sData[] = {
		0,1,	16,24,	8,12,
		18,1,	22,25,	12,12,
		42,1,	22,25,	12,12,
		66,1,	22,25,	12,12,
		0,27,	15,29,	8, 15,
	};

	private final static int S_MARKERS = 0;
	private final static int S_MAN = 4;
	private final static int S_TOTAL = 5;

	public ScorePnl(Panic parent) {
        super(scoreLocs, parent.getCharSet(1));

		this.parent = parent;

		Sprite m = new Sprite(parent.getSprite(), 0,108,90,57);

		spr = new Sprite[S_TOTAL];
		int j = 0;
		for (int i = 0; i < S_TOTAL; i++, j+=6) {
			spr[i] = new Sprite(m, sData[j+0],sData[j+1],sData[j+2],sData[j+3],sData[j+4],sData[j+5]);
		}
	}

	public void plotChanges() {
        super.plotChanges();//g,valid);
		if (!BEngine.layerValid()) {
			Sprite title = new Sprite(parent.getSprite(), 197,100,218,180);
			BEngine.drawSprite(title, 0, 0);
		}

		plotMsg();
		plotLevel();
		plotMen();
	}

    private static final int[] scoreLocs = {28,390,28,410};

	private final static int MEN_X = 18;
	private final static int MEN_Y = 330;
	private final static int MEN_SPACING = 24;
	private final static int MEN_TOTAL = 6;
	private final static int MEN_WIDTH = 20;
	private final static int MEN_HEIGHT = 30;

	private int menPlotFlags;

	private void plotMen() {
		int newPlotFlags = 0;
        boolean valid = BEngine.layerValid();
		for (int i = 0; i < MEN_TOTAL; i++) {
			int flag = 1 << i;
			int plotFlag = 0;

			if (VidGame.getMode() == VidGame.MODE_PLAYING)
				plotFlag = (VidGame.getLives() > i) ? flag : 0;

			if (!valid || ((menPlotFlags & flag) != plotFlag)) {

				int x = MEN_X + MEN_SPACING * i;

				BEngine.setColor(Color.black);
				BEngine.fillRect(x-MEN_WIDTH/2, MEN_Y - MEN_HEIGHT/2, MEN_WIDTH, MEN_HEIGHT);
				if (plotFlag != 0)
					BEngine.drawSprite(spr[S_MAN], x, MEN_Y);
			}
			newPlotFlags |= plotFlag;
		}
		menPlotFlags = newPlotFlags;
	}
	private final int MSG_LOADING = 1<<0;
	private final int MSG_SPACEBAR = 1<<1;
	private final int MSG_SPACEBAR2 = 1<<2;
	private final int MSG_SPACEBAR3 = 1<<3;
	private final int MSG_CONTROLS = 1<<4;
	private final int MSG_CONTROLS2 = 1<<5;
	private final int MSG_CONTROLS3 = 1<<6;
	private final int MSG_CONTROLS4 = 1<<7;
	private final int MSG_CONTROLS5 = 1<<8;
	private final int MSG_CONTROLS6 = 1<<9;
	private final int MSG_GAMEOVER = 1<<10;
	private final int MSG_PAUSED = 1<<11;
	private final int MSG_TOTAL = 12;

	private int prevMsgBits;

	private void plotMsg() {
        boolean valid = BEngine.layerValid();

		// Determine what messages to plot

		int msgBits = 0;

		if (VidGame.getMode() == VidGame.MODE_GAMEOVER)
			msgBits |= MSG_GAMEOVER;

		if (VidGame.paused())
			msgBits |= MSG_PAUSED;

		if (VidGame.getMode() <= VidGame.MODE_PREGAME) {
			int time = (VidGame.getTime() * (1024 / VidGame.FPS)) % 14000;
			if (VidGame.loading())
				msgBits |= MSG_LOADING;
			else if (time < 6000)
				msgBits |= MSG_SPACEBAR | MSG_SPACEBAR2 | MSG_SPACEBAR3;
			else {
				for (int i = 0; i<4; i++) {
					if (time >= (6000 + i*1024)) {
						final int ctrlMsg[] = {MSG_CONTROLS,
							MSG_CONTROLS2|MSG_CONTROLS3|MSG_CONTROLS4,
							MSG_CONTROLS5,
							MSG_CONTROLS6,
						};
						msgBits |= ctrlMsg[i];
					}
				}
			}
		}

		// Now that we know what to plot, plot any changes that have occurred.

		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < MSG_TOTAL; i++) {
				if (valid && (((msgBits ^ prevMsgBits) & (1 << i)) == 0)) continue;

				final int msgY[] = {
					235,

					235,
					255,
					275,

					210,
					240,
					260,
					280,
					310,
					330,

					255,

					255,
				};

				final int MSG_X = 10;

				if (pass == 0) {
					BEngine.setColor(Color.black);
					parent.getCharSet(1).clearBounds(MSG_X,msgY[i],20);
					continue;
				}

				if ((msgBits & (1 << i)) != 0) {
				final String msgText[] = {
					"LOADING...",
					"PRESS",
				 	"SPACE BAR",
					"TO START",
					"CONTROLS",
					"  ^           ",
					"<   >  MOVE   ",
					"  ~           ",
					"  F    DIG    ",
					"SPACE  FILL IN",

					"GAME OVER",
					"GAME PAUSED",
				};
					parent.getCharSet(1).centerString(msgText[i],BEngine.viewR.width/2, msgY[i]);
				}
			}
		}
		prevMsgBits = msgBits;
	}

	private final static int LEVEL_X = 4;
	private final static int LEVEL_Y = 350;
	private final static int LEVEL_WIDTH = 	200;
	private final static int LEVEL_HEIGHT = 26;

	private int oldLevel;
	private final int markerValues[] = {1,5,10,50};

	private void plotLevel() {
        boolean valid = BEngine.layerValid();
		int newLevel = VidGame.getLevel() + 1;

		if (VidGame.getMode() <= VidGame.MODE_PREGAME)
			newLevel = 0;

		if (newLevel != oldLevel || !valid) {
			BEngine.setColor(Color.black);
			BEngine.fillRect(LEVEL_X, LEVEL_Y, LEVEL_WIDTH, LEVEL_HEIGHT);
			oldLevel = newLevel;

			int rem = newLevel;
			int x = LEVEL_X;

			while (rem > 0) {

				int s = 3;
				while (rem < markerValues[s]) s--;

				rem -= markerValues[s];

				int w = 26;
				if (s == 0)
					w = 18;
				BEngine.drawSprite(spr[s], x + w/2, LEVEL_Y + 12);
				x += w;
			}
		}
	}
}