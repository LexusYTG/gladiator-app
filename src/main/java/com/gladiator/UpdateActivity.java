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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UpdateActivity extends Activity {
    private static final String TAG = "Update";
    private static final String INDEX_URL =
            "https://raw.githubusercontent.com/LexusYTG/Gladiator-Store/main/index.json";
    private static final int TIMEOUT_MS = 15000;
    private static final long MAX_BYTES = 64L * 1024 * 1024;

    private static final int ST_UNKNOWN  = 0;
    private static final int ST_CURRENT  = 1;
    private static final int ST_OLD      = 2;
    private static final int ST_MISSING  = 3;

    private static class Item {
        String id, component, category, version, file, sha256, desc;
        long size;
        List<String[]> dests = new ArrayList<>();
        int status = ST_UNKNOWN;
        String installedVersion, installedSha;
    }

    private static final String[] TOOLS = {"spatha", "scutum", "lorica", "init-setup", "session", "sesar"};
    private static final String[] CATS  = {"icd", "json", "bionic", "glibc", "shell"};
    private final List<Item> items = new ArrayList<>();
    private final Map<String, String> installedVersions = new LinkedHashMap<>();
    private final Map<String, String> installedShas = new LinkedHashMap<>();
    private String stubsRoot;
    private String activeTool = "spatha";
    private String activeCategory = "icd";
    private Ui.Chip[] tabs;

    private LinearLayout listBox, tabRow, catCol, contentCol;
    private TextView summary;
    private Ui.NeonButton updateBtn;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.styleWindow(this);

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
        TextView title = Ui.label(this, "ACTUALIZACIONES", 20, Ui.TEXT, true, 0.22f);
        Ui.glow(title, Ui.CYAN, 10);
        titles.addView(title);
        titles.addView(Ui.vspace(this, 5));
        titles.addView(Ui.label(this, "STORE // COMPONENTES", 10, Ui.MUTED, false, 0.25f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(14);
        top.addView(titles, tlp);

        View refresh = Ui.iconButton(this, Ui.G_PLAY, Ui.GREEN, 44);
        refresh.setOnClickListener(v -> reload());
        top.addView(refresh);
        screen.addView(top);

        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dlp.leftMargin = dp(20);
        dlp.rightMargin = dp(20);
        screen.addView(Ui.divider(this), dlp);

        // Tabs
        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(dp(20), dp(14), dp(20), dp(4));
        String[] names = {"SPATHA", "SCUTUM", "LORICA", "INIT", "SESS", "SESAR"};
        tabs = new Ui.Chip[names.length];
        for (int i = 0; i < names.length; i++) {
            final String key = TOOLS[i];
            Ui.Chip chip = new Ui.Chip(this, names[i]);
            chip.setChosen(key.equals(activeTool));
            chip.setOnClickListener(v -> selectTool(key));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) clp.leftMargin = dp(6);
            tabRow.addView(chip, clp);
            tabs[i] = chip;
        }
        screen.addView(tabRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20), dp(10), dp(20), dp(20));

        LinearLayout sumCard = Ui.card(this, Ui.CYAN);
        sumCard.addView(Ui.label(this, "ESTADO", 10, Ui.MUTED, true, 0.2f));
        sumCard.addView(Ui.vspace(this, 10));
        summary = Ui.label(this, "Consultando store…", 16, Ui.CYAN, true, 0.04f);
        Ui.glow(summary, Ui.CYAN, 8);
        sumCard.addView(summary);
        inner.addView(sumCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(20);
        slp.bottomMargin = dp(12);
        inner.addView(Ui.sectionTitle(this, "Componentes", Ui.CYAN), slp);

        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setBaselineAligned(false);

        catCol = new LinearLayout(this);
        catCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(
                dp(92), ViewGroup.LayoutParams.WRAP_CONTENT);
        contentRow.addView(catCol, catLp);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        listLp.leftMargin = dp(12);
        contentRow.addView(listBox, listLp);

        inner.addView(contentRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scroll.addView(inner, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        screen.addView(Ui.divider(this), new LinearLayout.LayoutParams(dlp));
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(dp(16), dp(12), dp(16), dp(12));
        dock.setBackgroundColor(0xE6060A16);

        updateBtn = new Ui.NeonButton(this, "Actualizar todo", Ui.NeonButton.PRIMARY);
        updateBtn.setOnClickListener(v -> startUpdate());
        dock.addView(updateBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        screen.addView(dock);

        int sw = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams slp2 = new FrameLayout.LayoutParams(
                Math.min(sw, dp(720)), ViewGroup.LayoutParams.MATCH_PARENT);
        slp2.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(screen, slp2);

        setContentView(root);
        loadInstalled();
        reload();
    }

    private void selectTool(String key) {
        activeTool = key;
        // si la categoria activa no existe en este tool, pasar a la primera con contenido
        boolean has = false;
        for (Item it : items)
            if (it.component.equals(activeTool) && it.category.equals(activeCategory)) { has = true; break; }
        if (!has) {
            for (String c : CATS) {
                for (Item it : items)
                    if (it.component.equals(activeTool) && it.category.equals(c)) {
                        activeCategory = c; has = true; break;
                    }
                if (has) break;
            }
        }
        for (int i = 0; i < tabs.length; i++)
            tabs[i].setChosen(TOOLS[i].equals(activeTool));
        renderCategoryColumn();
        renderContent();
    }

    // ============================================================ installed.json

    private void loadInstalled() {
        installedVersions.clear();
        installedShas.clear();
        File f = new File(BootstrapInstaller.prefixDir(this),
                "tmp/updates/installed.json");
        if (!f.exists()) return;
        try {
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int r = in.read(buf);
            in.close();
            if (r <= 0) return;
            JSONObject root = new JSONObject(new String(buf, "UTF-8"));
            JSONObject applied = root.optJSONObject("applied");
            if (applied == null) return;
            for (java.util.Iterator<String> it = applied.keys(); it.hasNext(); ) {
                String k = it.next();
                JSONObject o = applied.getJSONObject(k);
                installedVersions.put(k, o.optString("version", "0.0.0"));
                installedShas.put(k, o.optString("sha256", ""));
            }
        } catch (Throwable t) {
            GladiatorLog.err(TAG, "loadInstalled", t);
        }
    }

    // ============================================================ semver

    /** Devuelve >0 si a>b, <0 si a<b, 0 si iguales. Acepta "1.3.1" o "1.3". */
    private static int compareVersions(String a, String b) {
        if (a == null) a = "0";
        if (b == null) b = "0";
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int va = i < pa.length ? safeInt(pa[i]) : 0;
            int vb = i < pb.length ? safeInt(pb[i]) : 0;
            if (va != vb) return va - vb;
        }
        return 0;
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Throwable t) { return 0; }
    }

    // ============================================================ carga remota

    private void reload() {
        if (busy) return;
        busy = true;
        updateBtn.setEnabled(false);
        summary.setText("Consultando store…");
        summary.setTextColor(Ui.CYAN);
        listBox.removeAllViews();
        items.clear();

        new Thread(() -> {
            try {
                String json = httpGet(INDEX_URL);
                JSONObject root = new JSONObject(json);
                JSONObject stubs = root.getJSONObject("stubs");
                String scutumStub = stubs.getString("scutum");
                this.stubsRoot = scutumStub.substring(0, scutumStub.lastIndexOf('/'));

                JSONArray comps = root.getJSONArray("components");
                for (int i = 0; i < comps.length(); i++) {
                    JSONObject o = comps.getJSONObject(i);
                    Item it = new Item();
                    it.id = o.getString("id");
                    it.component = o.getString("component");
                    it.category  = o.optString("category", "glibc");
                    it.version = o.getString("version");
                    it.file = o.getString("file");
                    it.sha256 = o.getString("sha256");
                    it.size = o.optLong("size", 0);
                    it.desc = o.optString("desc", "");
                    JSONArray ds = o.getJSONArray("dests");
                    for (int j = 0; j < ds.length(); j++) {
                        JSONObject d = ds.getJSONObject(j);
                        it.dests.add(new String[]{
                                d.getString("path"), d.optString("mode", "0755")});
                    }
                    it.installedVersion = installedVersions.get(it.id);
                    it.installedSha = installedShas.get(it.id);

                    if (it.installedVersion == null) {
                        // primera vez: chequear sha local para no marcar todo como "falta"
                        File local = new File(BootstrapInstaller.prefixDir(this),
                                it.dests.get(0)[0]);
                        if (local.exists()) {
                            String ls = sha256(local);
                            it.status = (ls != null && ls.equalsIgnoreCase(it.sha256))
                                    ? ST_CURRENT : ST_OLD;
                        } else {
                            it.status = ST_MISSING;
                        }
                    } else {
                        int cmp = compareVersions(it.version, it.installedVersion);
                        File local = new File(BootstrapInstaller.prefixDir(this),
                                it.dests.get(0)[0]);
                        if (!local.exists()) it.status = ST_MISSING;
                        else if (cmp > 0) it.status = ST_OLD;
                        else it.status = ST_CURRENT;
                    }
                    items.add(it);
                }
                runOnUiThread(() -> { renderCategoryColumn(); renderContent(); });
            } catch (Throwable t) {
                GladiatorLog.err(TAG, "reload", t);
                runOnUiThread(() -> {
                    summary.setText("Error: " + t.getMessage());
                    summary.setTextColor(Ui.RED);
                    busy = false;
                    updateBtn.setEnabled(true);
                });
            }
        }, "store-fetch").start();
    }

    private Ui.Chip[] catChips;

    private void renderCategoryColumn() {
        catCol.removeAllViews();
        catChips = new Ui.Chip[CATS.length];
        for (int i = 0; i < CATS.length; i++) {
            final String c = CATS[i];
            boolean has = false;
            for (Item it : items)
                if (it.component.equals(activeTool) && it.category.equals(c)) { has = true; break; }
            Ui.Chip chip = new Ui.Chip(this, c.toUpperCase());
            chip.setChosen(c.equals(activeCategory));
            if (!has) chip.setAlpha(0.35f);
            chip.setOnClickListener(v -> {
                activeCategory = c;
                for (int j = 0; j < catChips.length; j++)
                    catChips[j].setChosen(CATS[j].equals(activeCategory));
                renderContent();
            });
            catChips[i] = chip;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            catCol.addView(chip, lp);
        }
    }

    private void renderContent() {
        listBox.removeAllViews();
        List<Item> shown = new ArrayList<>();
        for (Item it : items)
            if (it.component.equals(activeTool) && it.category.equals(activeCategory))
                shown.add(it);
        Collections.sort(shown, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) {
                int c = compareVersions(b.version, a.version);
                if (c != 0) return c;
                return a.id.compareToIgnoreCase(b.id);
            }
        });

        if (shown.isEmpty()) {
            TextView t = Ui.bodyText(this, "Sin componentes en esta categoría.", 13, Ui.MUTED);
            t.setGravity(Gravity.CENTER);
            listBox.addView(t);
        } else {
            for (Item it : shown) {
                View row = itemRow(it);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(10);
                listBox.addView(row, lp);
            }
        }

        int outdated = 0, missing = 0;
        for (Item it : items) {
            if (it.status == ST_OLD) outdated++;
            else if (it.status == ST_MISSING) missing++;
        }
        if (outdated == 0 && missing == 0) {
            summary.setText("Todo al día");
            summary.setTextColor(Ui.GREEN);
        } else {
            summary.setText(outdated + " actualizables · " + missing + " faltantes");
            summary.setTextColor(Ui.AMBER);
        }
        boolean anyPending = false;
        for (Item it : items) if (it.status != ST_CURRENT) { anyPending = true; break; }
        updateBtn.setEnabled(anyPending && !busy);
    }

    private View itemRow(Item it) {
        int accent = it.status == ST_CURRENT ? Ui.GREEN
                : it.status == ST_OLD ? Ui.AMBER : Ui.RED;
        Ui.NeonCard card = new Ui.NeonCard(this, accent);
        card.setClickable(false);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = Ui.label(this, it.id, 15, Ui.TEXT, true, 0.03f);
        head.addView(name, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String st = it.status == ST_CURRENT ? "AL DÍA"
                : it.status == ST_OLD ? "ACTUALIZABLE" : "FALTA";
        head.addView(Ui.badge(this, st, accent));
        card.addView(head);

        card.addView(Ui.vspace(this, 8));
        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.addView(Ui.badge(this, it.component, Ui.CYAN));
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.leftMargin = dp(6);
        meta.addView(Ui.badge(this, "v" + it.version, Ui.MAGENTA), mlp);
        if (it.installedVersion != null
                && !it.installedVersion.equals(it.version)) {
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ilp.leftMargin = dp(6);
            meta.addView(Ui.badge(this, "inst. " + it.installedVersion, Ui.MUTED), ilp);
        }
        card.addView(meta);

        if (it.desc != null && !it.desc.isEmpty()) {
            card.addView(Ui.vspace(this, 8));
            card.addView(Ui.bodyText(this, it.desc, 12, Ui.TEXT_DIM));
        }
        return card;
    }

    // ============================================================ apply

    private void startUpdate() {
        if (busy) return;
        List<Item> pending = new ArrayList<>();
        for (Item it : items) if (it.status != ST_CURRENT) pending.add(it);
        if (pending.isEmpty()) return;

        busy = true;
        updateBtn.setEnabled(false);
        summary.setText("Descargando 0/" + pending.size() + "…");
        summary.setTextColor(Ui.CYAN);

        new Thread(() -> doUpdate(pending), "store-update").start();
    }

    private void doUpdate(List<Item> pending) {
        File prefix = BootstrapInstaller.prefixDir(this);
        File updates = new File(prefix, "tmp/updates");
        File staging = new File(updates, "staging");
        deleteRecursive(staging);
        staging.mkdirs();

        StringBuilder apply = new StringBuilder();
        apply.append("#!/data/data/com.glads1/files/usr/bin/bash\n");
        apply.append("set -e\n");
        apply.append("PREFIX=\"${PREFIX:-/data/data/com.glads1/files/usr}\"\n");
        apply.append("STAGING=\"$PREFIX/tmp/updates/staging\"\n");
        apply.append("ROOTFS_LIB=\"$PREFIX/var/lib/proot-distro/containers/ubuntu/rootfs/usr/lib/aarch64-linux-gnu\"\n");
        apply.append("apply_one() {\n");
        apply.append("  local src=\"$1\" dst=\"$2\" mode=\"$3\"\n");
        apply.append("  [ -f \"$src\" ] || return 0\n");
        apply.append("  mkdir -p \"$(dirname \"$dst\")\"\n");
        apply.append("  mv -f \"$src\" \"$dst\"\n");
        apply.append("  chmod \"$mode\" \"$dst\"\n");
        apply.append("}\n");
        apply.append("mirror_lib() {\n");
        apply.append("  local f=\"$1\"\n");
        apply.append("  [ -f \"$f\" ] || return 0\n");
        apply.append("  mkdir -p \"$ROOTFS_LIB\"\n");
        apply.append("  cp -f \"$f\" \"$ROOTFS_LIB/$(basename \"$f\")\"\n");
        apply.append("}\n");

        List<Item> appliedOk = new ArrayList<>();
        int fail = 0;
        for (int i = 0; i < pending.size(); i++) {
            Item it = pending.get(i);
            final int pct = i;
            runOnUiThread(() -> summary.setText(
                    "Descargando " + (pct + 1) + "/" + pending.size() + " — " + it.id));
            try {
                String url = stubsRoot + "/" + it.component + "/" + it.file;
                File dst = new File(staging, it.id + ".bin");
                httpDownload(url, dst);
                String h = sha256(dst);
                if (!h.equalsIgnoreCase(it.sha256)) {
                    dst.delete(); fail++; continue;
                }
                for (String[] pair : it.dests) {
                    apply.append("apply_one \"$STAGING/").append(it.id).append(".bin\" ")
                         .append("\"$PREFIX/").append(pair[0]).append("\" ")
                         .append("\"").append(pair[1]).append("\"\n");
                }
                if (it.id.endsWith(".so") || it.id.contains(".so.")) {
                    apply.append("mirror_lib \"$PREFIX/")
                         .append(it.dests.get(0)[0]).append("\"\n");
                }
                appliedOk.add(it);
            } catch (Throwable t) {
                GladiatorLog.err(TAG, "dl " + it.id, t);
                fail++;
            }
        }

        // installed.json: versiones nuevas aplicadas
        apply.append("INSTALLED=\"$PREFIX/tmp/updates/installed.json\"\n");
        apply.append("cat > \"$INSTALLED\" << 'INSTEOF'\n");
        apply.append(serializeInstalled(appliedOk));
        apply.append("INSTEOF\n");
        apply.append("chmod 0644 \"$INSTALLED\"\n");
        apply.append("rm -rf \"$STAGING\"\n");
        apply.append("rm -f \"$PREFIX/tmp/updates/apply.sh\"\n");

        try {
            File applyFile = new File(updates, "apply.sh");
            FileOutputStream fos = new FileOutputStream(applyFile);
            fos.write(apply.toString().getBytes("UTF-8"));
            fos.close();
            applyFile.setExecutable(true, false);
        } catch (Throwable t) {
            GladiatorLog.err(TAG, "write apply.sh", t);
            fail++;
        }

        final int fOk = appliedOk.size(), fFail = fail;
        runOnUiThread(() -> {
            busy = false;
            updateBtn.setEnabled(true);
            if (fFail == 0) {
                summary.setText(fOk + " listo(s). Reiniciá el contenedor.");
                summary.setTextColor(Ui.GREEN);
                new Ui.GameDialog(this, "Actualización lista", Ui.GREEN)
                        .message(fOk + " componente(s) descargado(s) y verificados.\n\n"
                                + "Cerrá la sesión actual y volvé a abrir el contenedor.")
                        .button("OK", Ui.NeonButton.PRIMARY, d -> { d.dismiss(); finish(); })
                        .show();
            } else {
                summary.setText(fOk + " ok · " + fFail + " fallo(s)");
                summary.setTextColor(Ui.RED);
            }
        });
    }

    private String serializeInstalled(List<Item> ok) {
        // Mezcla: lo ya instalado + lo nuevo aplicado
        Map<String, String[]> merged = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : installedVersions.entrySet()) {
            merged.put(e.getKey(), new String[]{e.getValue(),
                    installedShas.containsKey(e.getKey()) ? installedShas.get(e.getKey()) : ""});
        }
        for (Item it : ok) merged.put(it.id, new String[]{it.version, it.sha256});

        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"applied\": {\n");
        int i = 0, n = merged.size();
        for (Map.Entry<String, String[]> e : merged.entrySet()) {
            sb.append("    \"").append(e.getKey()).append("\": {")
              .append("\"version\": \"").append(e.getValue()[0]).append("\", ")
              .append("\"sha256\": \"").append(e.getValue()[1]).append("\"}");
            if (++i < n) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n}\n");
        return sb.toString();
    }

    // ============================================================ helpers

    private String httpGet(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("User-Agent", "Gladiator/1.3.1");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new RuntimeException("HTTP " + code);
            InputStream in = c.getInputStream();
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            return bos.toString("UTF-8");
        } finally { c.disconnect(); }
    }

    private void httpDownload(String url, File dst) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("User-Agent", "Gladiator/1.3.1");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new RuntimeException("HTTP " + code);
            InputStream in = c.getInputStream();
            FileOutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                total += n;
                if (total > MAX_BYTES) throw new RuntimeException("archivo demasiado grande");
            }
            out.close(); in.close();
        } finally { c.disconnect(); }
    }

    private static String sha256(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            in.close();
            byte[] h = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable t) { return null; }
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] ks = f.listFiles();
            if (ks != null) for (File k : ks) deleteRecursive(k);
        }
        f.delete();
    }

    private int dp(float v) { return Ui.dp(this, v); }
}
