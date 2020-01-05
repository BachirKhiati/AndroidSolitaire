package com.firenoid.solitaire;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firenoid.solitaire.Whiteboard.Event;
import com.firenoid.solitaire.game.JSONStorage;
import com.firenoid.solitaire.model.Card;
import com.firenoid.solitaire.model.Deck;
import com.firenoid.solitaire.model.IMove2;
import com.firenoid.solitaire.model.Move;
import com.firenoid.solitaire.model.Table;
import com.firenoid.solitaire.util.TouchHandler2;

import static com.firenoid.solitaire.model.Table.TABLEAU_DECKS_COUNT;

public class MenuController {

    private static final float ACTIVE_ALPHA = 0.5f;
    private static final float INACTIVE_ALPHA = 1f;
    private static int SHUFFLE_COUNT = 2;
    private final MainActivity mainActivity;
    private final View gameSubmenu;
    private final View btnReplay;
    private final View menuView;
    private final View leftMenu;
    private final View rightMenu;

    private final View btnSettings;
    private final View btnAutofinish;
    private final View btnUndo;
    private final Button btnShuffle;
    private final TextView btnNewGame;
    private final View btnDraw1;
    private final View btnDraw3;
    private final int animTime;

    private View menuVisible;
    private boolean showingWinMenu;
    private boolean disableMenu;
    private boolean disableToggle;
    private Animation vibrateAnimation;


    public MenuController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        animTime = mainActivity.getResources().getInteger(android.R.integer.config_mediumAnimTime);
        menuView = mainActivity.findViewById(R.id.menuView);
        leftMenu = menuView.findViewById(R.id.menu_left);
        rightMenu = menuView.findViewById(R.id.menu_right);
        btnShuffle = (Button) menuView.findViewById(R.id.shuffle_btn);
        gameSubmenu = menuView.findViewById(R.id.game_submenu);

        btnReplay = menuView.findViewById(R.id.menu_replay);
        btnSettings = menuView.findViewById(R.id.menu_settings_btn);
        btnAutofinish = menuView.findViewById(R.id.menu_autofinish_btn);
        btnUndo = menuView.findViewById(R.id.menu_undo_btn);
        btnNewGame = (TextView) menuView.findViewById(R.id.menu_new_game);
        btnDraw1 = menuView.findViewById(R.id.menu_draw1);
        btnDraw3 = menuView.findViewById(R.id.menu_draw3);

        addListeners();
        updateDraw3().start();

        rightMenu.setTranslationY(rightMenu.getHeight());
        leftMenu.setTranslationX(-leftMenu.getWidth() - leftMenu.getX());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addListeners() {
        // attach to background
        final View gameBackground = mainActivity.findViewById(R.id.effectsView);
        gameBackground.setOnTouchListener(new TouchHandler2() {
            private final int SWIPE_TOLERANCE = 2* (int) mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin);
            private int dragStartX = -1;
            private int dragX;

            @Override
            protected void click(int x, int y) {
                if (showingWinMenu || disableMenu) {
                    return;
                }
                toggleMenu(rightMenu);
            }

            @Override
            protected void dragStart(int x, int y) {
                dragStartX = x;
            }

            @Override
            protected void drag(int x, int y) {
                dragX = x;
            }

            @Override
            protected void dragEnd() {
                if(!disableMenu && !showingWinMenu) {
                    if(dragStartX < SWIPE_TOLERANCE && dragX - dragStartX > SWIPE_TOLERANCE) { // swiped right
                        toggleMenu(leftMenu);
                    } else {
                        hideMenuNow();
                    }
                }
                dragStartX = -1;
            }
        });

        // create menu items
        btnSettings.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                mainActivity.getSettingsManager().showSettings();
            }
        });
        // create menu items
        btnShuffle.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                if(SHUFFLE_COUNT == 1){
                    SHUFFLE_COUNT--;
                    btnShuffle.setText("New Game");
                    newShuffleGame();

                }  else if(SHUFFLE_COUNT  < 1){
                    newGame();
                    SHUFFLE_COUNT= 2;
                    btnShuffle.setText("Shuffle ("+ SHUFFLE_COUNT+ ")");
                } else {
                    newShuffleGame();

                    SHUFFLE_COUNT--;
                    btnShuffle.setText("Shuffle ("+ SHUFFLE_COUNT+ ")");

                }

            }
        });
        btnAutofinish.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
//                autofinish();
                hintUser();
            }
        });
        btnUndo.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                Table table = mainActivity.getTable();
                if (table.getHistory().isEmpty()) {
                    return;
                }
                Animator anim = mainActivity.getMover().undo();
                anim.start();
                updateMenu();
            }
        });
        btnNewGame.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                newGame();
            }
        });
        btnReplay.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                replay();
            }
        });
        btnDraw1.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                setDrawThree(false);
            }
        });
        btnDraw3.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                setDrawThree(true);
            }
        });
    }

    private void setDrawThree(boolean drawThree) {
        boolean curDrawThree = mainActivity.getSettingsManager().getSettings().drawThree;
        if (drawThree == curDrawThree) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        Editor editor = prefs.edit();
        editor.putBoolean(mainActivity.getString(R.string.pref_draw_three), drawThree);
        editor.commit();

        updateDraw3().start();
    }

    private Animator updateDraw3() {

        boolean drawThree = mainActivity.getSettingsManager().getSettings().drawThree;
        List<Animator> anims = new ArrayList<Animator>();
        final int[] curDrawThreeImageId = new int[1]; // hack, but this has to be final
        if (drawThree) {
            anims.add(ObjectAnimator.ofFloat(btnDraw1, "alpha", btnDraw1.getAlpha(), INACTIVE_ALPHA));
            anims.add(ObjectAnimator.ofFloat(btnDraw3, "alpha", btnDraw3.getAlpha(), ACTIVE_ALPHA));
            curDrawThreeImageId[0] = R.drawable.draw3;
        } else {
            anims.add(ObjectAnimator.ofFloat(btnDraw1, "alpha", btnDraw1.getAlpha(), ACTIVE_ALPHA));
            anims.add(ObjectAnimator.ofFloat(btnDraw3, "alpha", btnDraw3.getAlpha(), INACTIVE_ALPHA));
            curDrawThreeImageId[0] = R.drawable.draw1;
        }

        AnimatorSet res = new AnimatorSet();
        res.playTogether(anims);
        res.setDuration(mainActivity.getAnimationTimeMs());
        return res;
    }

    public Animator showWinMenu() {
        final View winMenu = mainActivity.findViewById(R.id.menu_new_game_win);
        winMenu.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                newGame();
            }
        });
        winMenu.setTranslationY(0);
        winMenu.setAlpha(0);
        winMenu.setVisibility(View.VISIBLE);
        ObjectAnimator anim = ObjectAnimator.ofFloat(winMenu, "alpha", 0, 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuVisible = winMenu;
                showingWinMenu = true;
            }
        });
        return anim;
    }

    public void updateMenu() {
        Table table = mainActivity.getTable();
        if (table == null) {
            return;
        }
        btnSettings.setVisibility(View.VISIBLE);
        btnReplay.setVisibility(table.getHistory().isEmpty() ? View.GONE : View.VISIBLE);
        btnDraw1.setVisibility(View.VISIBLE);
        btnDraw3.setVisibility(View.VISIBLE);

//        if(mainActivity.getSolver().canAutoComplete(table)) {
            btnAutofinish.setAlpha(1);
            btnAutofinish.setVisibility(View.VISIBLE);
//        } else {
//            hideItemNow(btnAutofinish);
//        }
        if(!table.getHistory().isEmpty()) {
            btnUndo.setAlpha(1);
            btnUndo.setVisibility(View.VISIBLE);
        } else {
            hideItemNow(btnUndo);
        }
    }

    private void hideItemNow(final View item) {
        if(menuVisible == null) {
            item.setVisibility(View.GONE);
            return;
        }

        Animator fadeOut = ObjectAnimator.ofFloat(item, "alpha", 1, 0);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                item.setVisibility(View.GONE);
            }
        });
        fadeOut.setDuration(2*animTime);
        fadeOut.start();
    }

    public void toggleMenu(View menu) {
        if(disableToggle) {
            return;
        }

        Animator hideMenu = hideMenu();
        if (hideMenu != null) {
            hideMenu.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    disableToggle = false;
                    updateMenu();
                }
            });
            disableToggle = true;
            hideMenu.start();
        } else {
            updateMenu();
            showMenu(menu);
        }
    }

    public void showLeftMenu() {
        showMenu(leftMenu);
    }
    public void showShuffleBtn() {
        btnShuffle.setVisibility(View.VISIBLE);
    }

    public void showRightMenu() {
        showMenu(rightMenu);
    }

    public void showMenu(final View menu) {
        ObjectAnimator moveIn;
        if(menu.getId() == R.id.menu_left) {
            moveIn = ObjectAnimator.ofFloat(menu, "translationX", menu.getTranslationX(), 0);
        } else {
            moveIn = ObjectAnimator.ofFloat(menu, "translationY", menu.getHeight(), 0);
        }

        moveIn.setDuration(animTime);
        moveIn.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
        menu.bringToFront();
        menu.setVisibility(View.VISIBLE);
        menu.setAlpha(0);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(menu, "alpha", 0, 1);
        fadeIn.setDuration(animTime);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveIn, fadeIn);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                disableToggle = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                disableToggle = false;
            }
        });
        set.start();
        menuVisible = menu;
    }

    public void hideMenuNow() {
        Animator hide = hideMenu();
        if(hide != null) {
            hide.start();
        }
    }

    /**
     *
     * @return may return <tt>null</tt>
     */
    public Animator hideMenu() {
        return menuVisible == null ? null : hideMenu(menuVisible, false);
    }

    private Animator hideMenu(final View menu, final boolean hideScore) {
        //final View menu = rightMenu;

        ObjectAnimator moveOut;
        if(menu.getId() == R.id.menu_left) {
            moveOut = ObjectAnimator.ofFloat(menu, "translationX", 0, -menu.getWidth());
        } else {
            moveOut = ObjectAnimator.ofFloat(menu, "translationY", menu.getTranslationY(), menu.getHeight());
        }

        moveOut.setDuration(animTime);
        moveOut.setInterpolator(new AccelerateInterpolator(1.5f));

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(menu, "alpha", 1, 0);
        fadeOut.setDuration(animTime);

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuVisible = null;
                gameSubmenu.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                menu.setVisibility(View.GONE);
            }
        });
        set.playTogether(moveOut, fadeOut);

        return set;
    }

    public void showAutofinishMenu() {
        btnAutofinish.setVisibility(View.VISIBLE);
        btnShuffle.setVisibility(View.INVISIBLE);
        btnSettings.setVisibility(View.INVISIBLE);
        btnUndo.setVisibility(View.INVISIBLE);

        menuView.post(new Runnable() {
            @Override
            public void run() {
                showMenu(rightMenu);
            }
        });
    }

    private void autofinish() {
        hideMenu(rightMenu, true).start();
        final Table table = mainActivity.getTable();
        Animator autoFinish = mainActivity.getMover().animateAutoFinish();
        mainActivity.getStorage().saveTable(table);
        Whiteboard.post(Event.WON);

        autoFinish.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Utils.showGameWonStuff(mainActivity);
            }
        });
        disableMenu = true;
        autoFinish.start();
    }

    public void hintUser(){
        Deck[] foundationDeck = mainActivity.getTable().getFoundations();
        Deck[] table = mainActivity.getTable().getTableau();
        ArrayList listFoundation = mainActivity.getFaundationCardView();
        ArrayList<Integer> ordinalValues = new ArrayList<>();
        for(int deckIndex = 0;deckIndex < Table.TABLEAU_DECKS_COUNT; deckIndex++){
            for(int cardIndex = 0 ; cardIndex < table[deckIndex].getCardsCount();cardIndex++){
                Card card = table[deckIndex].getCardAt(cardIndex);
                boolean isTop = table[deckIndex].getCardAt(cardIndex) == table[deckIndex].getCardAt(0);
                // for the tableau
                for (int i = 0; i < 7; i++) {
                    Deck deck = table[i];
                    if (deck.getCardsCount() == 0) {
                        if (card.numberValue() == 13) {
                            // king opens new deck
                            ordinalValues.add(card.ordinal());
                        }
                        continue;
                    }

                    Card top = deck.getCardAt(0);
                    boolean differentIndex = deckIndex != i;
                    boolean sameType = card.type() == top.type();
                    if (mainActivity.isCardRevealed(card.ordinal()) && mainActivity.isCardRevealed(top.ordinal()) && differentIndex && top.numberValue() == card.numberValue() + 1 && sameType) {
                        ordinalValues.add(card.ordinal());

                    } else if(listFoundation.contains(card.ordinal())){
                        ordinalValues.add(card.ordinal());

                    }
                }

                for (int i = 0; i < 4; i++) {
                    Deck deck = foundationDeck[i];
                    if (mainActivity.isCardRevealed(card.ordinal()) && deck.getCardsCount() == 0 && card.numberValue() == 1 && isTop) {
                        ordinalValues.add(card.ordinal());
                        continue;
                    } else if (mainActivity.isCardRevealed(card.ordinal()) && deck.getCardsCount() > 0 && deck.getCardAt(0).getSuit() == card.getSuit()
                            && deck.getCardAt(0).numberValue() + 1 == card.numberValue() && isTop) {
                        ordinalValues.add(card.ordinal());
                    }
                }

            }

            for(Integer i : ordinalValues){
                ImageView lastImageView = mainActivity.getCardView(i);
                TranslateAnimation animate = new TranslateAnimation(0,0,0,-40);
                animate.setDuration(1000);
                animate.setRepeatMode(Animation.REVERSE);
                lastImageView.startAnimation(animate);
            }

        }



//        Animator rotate = mainActivity.getMover().hintUser(ordinalValues);
//        rotate.setInterpolator(new DecelerateInterpolator());
////        disableMenu = true;
//        rotate.start();

    }

    public void newGame() {
        Table table = mainActivity.getTable();
        showingWinMenu = false;
        btnShuffle.setVisibility(View.GONE);
        boolean lost = false;
        if (!table.isSolved()) {
            Whiteboard.post(Event.LOST);
            lost = true;
        }

        table.reset();
        if (lost && mainActivity.getStorage().loadOrCreateStats(table.isDrawThree()).getStrike() < -2) {
            // don't make the user sad and generate a wining game
            mainActivity.getSolver().initWinningGame(table);
        } else {
            table.init();
        }

        mainActivity.getTimer().pause();
        mainActivity.getTimer().setTime(0);
        mainActivity.resetFoundationCardView();
        JSONStorage storage = mainActivity.getStorage();
        storage.saveTable(table);
        Whiteboard.post(Event.GAME_STARTED);
        hideMenuNow();

        resetAndDeal();
    }


    public void newShuffleGame() {
        Table table = mainActivity.getTable();
        showingWinMenu = false;
        btnShuffle.setVisibility(View.GONE);

        boolean lost = false;
        if (!table.isSolved()) {
            Whiteboard.post(Event.LOST);
            lost = true;
        }

        table.shuffle();
//        if (lost && mainActivity.getStorage().loadOrCreateStats(table.isDrawThree()).getStrike() < -2) {
//            // don't make the user sad and generate a wining game
//            mainActivity.getSolver().initWinningGame(table);
//        } else {
            table.init();
//        }

        mainActivity.getTimer().pause();
        mainActivity.getTimer().setTime(0);
        JSONStorage storage = mainActivity.getStorage();
        storage.saveTable(table);
        Whiteboard.post(Event.GAME_STARTED);
        hideMenuNow();
        reshuffelAndDeal();
    }



    private void replay() {
        Table table = mainActivity.getTable();

        if (table.getHistory().isEmpty()) {
            return;
        }

        Deck hand = new Deck();
        List<Card> cardsToFlip = new ArrayList<Card>();
        while (!table.getHistory().isEmpty()) {
            IMove2 move = table.getHistory().pop();
            move.beginUndo(table, hand, cardsToFlip);
            move.completeUndo(table, hand, cardsToFlip);
        }
        table.setTime(mainActivity.getTimer().getTime());
        JSONStorage storage = mainActivity.getStorage();
        storage.saveTable(table);
        hideMenuNow();

        resetAndDeal();
    }

    private void resetAndDeal() {
        Animator reset = mainActivity.getMover().collectCards();
        reset.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Animator nu = mainActivity.getMover().deal();
                nu.setStartDelay(mainActivity.getAnimationTimeMs() / 3);
                // nu.setInterpolator(new AccelerateInterpolator());
                nu.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        disableMenu = false;
                    }
                });
                nu.start();

            }
        });
        reset.setInterpolator(new DecelerateInterpolator());
        disableMenu = true;
        reset.start();
    }

    private void reshuffelAndDeal() {
//        Animator reset = mainActivity.getMover().collectCards();
        Animator reset = mainActivity.getMover().collectDealCards();
        reset.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Animator nu = mainActivity.getMover().deal();
                nu.setStartDelay(mainActivity.getAnimationTimeMs() / 3);
                // nu.setInterpolator(new AccelerateInterpolator());
                nu.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        disableMenu = false;
                    }
                });
                nu.start();

            }
        });
        reset.setInterpolator(new DecelerateInterpolator());
        disableMenu = true;
        reset.start();
    }

    public boolean isShowingWinMenu() {
        return showingWinMenu;
    }
}