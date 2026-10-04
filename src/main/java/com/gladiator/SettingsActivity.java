package com.gladiator;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final int MAX_W_DP = 720;

    private Prefs prefs;
    private TextView specView;
    private TextView qualityView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.styleWindow(this);
        prefs = new Prefs(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.addView(new Ui.GridBackground(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(14), dp(20), dp(14));

        View back = Ui.iconButton(this, Ui.G_BACK, Ui.CYAN, 44);
        back.setOnClickListener(v -> finish());
        top.addView(back);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.label(this, "AJUSTES", 20, Ui.TEXT, true, 0.22f);
        Ui.glow(title, Ui.CYAN, 10);
        titles.addView(title);
        titles.addView(Ui.vspace(this, 5));
        titles.addView(Ui.label(this, "PANTALLA // RENDIMIENTO", 10, Ui.MUTED, false, 0.25f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(14);
        top.addView(titles, tlp);
        screen.addView(top);

        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dlp.leftMargin = dp(20);
        dlp.rightMargin = dp(20);
        screen.addView(Ui.divider(this), dlp);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(18), dp(20), dp(20));

        LinearLayout summary = Ui.card(this, Ui.MAGENTA);
        summary.addView(Ui.label(this, "PERFIL DE PANTALLA", 10, Ui.MUTED, true, 0.2f));
        summary.addView(Ui.vspace(this, 10));
        specView = Ui.label(this, "", 26, Ui.CYAN, true, 0.04f);
        Ui.glow(specView, Ui.CYAN, 10);
        summary.addView(specView);
        summary.addView(Ui.vspace(this, 8));
        qualityView = Ui.label(this, "", 12, Ui.TEXT_DIM, true, 0.15f);
        summary.addView(qualityView);
        list.addView(summary, cardLp(0));
        updateSummary();

        LinearLayout resCard = section(list, "Resolución");
        resCard.addView(Ui.choices(this, Prefs.RESOLUTIONS,
                indexOf(Prefs.RESOLUTIONS, prefs.resolution()), false, i -> {
                    prefs.setResolution(Prefs.RESOLUTIONS[i]);
                    updateSummary();
                }));

        String[] depthLabels = new String[Prefs.DEPTHS.length];
        for (int i = 0; i < depthLabels.length; i++) depthLabels[i] = Prefs.DEPTHS[i] + " bit";
        LinearLayout depthCard = section(list, "Profundidad de color");
        depthCard.addView(Ui.choices(this, depthLabels,
                indexOf(Prefs.DEPTHS, String.valueOf(prefs.depth())), true, i -> {
                    prefs.setDepth(Integer.parseInt(Prefs.DEPTHS[i]));
                    updateSummary();
                }));

        LinearLayout qualCard = section(list, "Calidad gráfica");
        qualCard.addView(Ui.choices(this, Prefs.QUALITY,
                Math.max(0, Math.min(prefs.quality(), Prefs.QUALITY.length - 1)), true, i -> {
                    prefs.setQuality(i);
                    updateSummary();
                }));

        LinearLayout inputCard = section(list, "Modo de mouse");
        inputCard.addView(Ui.bodyText(this,
                "Botones volumen: subir = click izq, bajar = click der. " +
                "Gestos: 1 dedo tap = click izq, 2 dedos tap = click der, 2 dedos drag = scroll.",
                11, Ui.MUTED));
        inputCard.addView(Ui.vspace(this, 10));
        inputCard.addView(Ui.choices(this, Prefs.INPUT_MODES,
                Math.max(0, Math.min(prefs.inputMode(), Prefs.INPUT_MODES.length - 1)), true, i -> {
                    prefs.setInputMode(i);
                }));

        LinearLayout ctrlCard = Ui.card(this, Ui.MAGENTA);
        ctrlCard.addView(Ui.label(this, "CONTROLES EN PANTALLA", 10, Ui.MUTED, true, 0.2f));
        ctrlCard.addView(Ui.vspace(this, 10));
        Ui.NeonButton openEditor = new Ui.NeonButton(this, "Configurar controles", Ui.NeonButton.PRIMARY);
        openEditor.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(
                    SettingsActivity.this, ControlEditorActivity.class);
            startActivity(i);
        });
        ctrlCard.addView(openEditor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        LinearLayout.LayoutParams ctrlLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ctrlLp.topMargin = dp(24);
        list.addView(ctrlCard, ctrlLp);

        TextView hint = Ui.bodyText(this,
                "Los cambios se aplican al iniciar la próxima sesión.", 12, Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(22);
        list.addView(hint, hlp);

        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        screen.addView(Ui.divider(this), new LinearLayout.LayoutParams(dlp));
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(dp(16), dp(12), dp(16), dp(12));
        dock.setBackgroundColor(0xE6060A16);
        Ui.NeonButton done = new Ui.NeonButton(this, "Guardar y volver", Ui.NeonButton.PRIMARY);
        done.setOnClickListener(v -> finish());
        dock.addView(done, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        screen.addView(dock);

        int screenW = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                Math.min(screenW, dp(MAX_W_DP)), ViewGroup.LayoutParams.MATCH_PARENT);
        slp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(screen, slp);

        setContentView(root);
    }

    private void updateSummary() {
        specView.setText(prefs.screenSpec());
        int q = Math.max(0, Math.min(prefs.quality(), Prefs.QUALITY.length - 1));
        qualityView.setText("CALIDAD // " + Prefs.QUALITY[q].toUpperCase());
    }

    private LinearLayout section(LinearLayout list, String title) {
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = dp(24);
        tl.bottomMargin = dp(12);
        list.addView(Ui.sectionTitle(this, title, Ui.CYAN), tl);
        LinearLayout card = Ui.card(this, Ui.CYAN);
        list.addView(card, cardLp(0));
        return card;
    }

    private LinearLayout.LayoutParams cardLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 0;
    }

    private int dp(float v) { return Ui.dp(this, v); }
}
