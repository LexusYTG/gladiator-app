package com.gladiator;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class DebugRunner {
    private static final String TAG = "GladiatorDebug";

    public static void runDiagnostics(Context ctx) {
        File prefix = BootstrapInstaller.prefixDir(ctx);
        File bash = new File(prefix, "bin/bash");

        String[] tests = {
            "id",
            "uname -a",
            "echo $PREFIX",
            "ls -la " + prefix.getAbsolutePath() + "/bin/proot",
            "ls -la " + prefix.getAbsolutePath() + "/bin/bash",
            "ls -la " + prefix.getAbsolutePath() + "/var/lib/proot-distro/containers/ubuntu/rootfs/usr/bin/bash",
            "ls -la " + prefix.getAbsolutePath() + "/var/lib/proot-distro/containers/ubuntu/rootfs/usr/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1",
            "ls -ldZ " + prefix.getAbsolutePath() + "/bin",
            "ls -ldZ " + prefix.getAbsolutePath() + "/var/lib/proot-distro/containers/ubuntu/rootfs/usr/bin",
            prefix.getAbsolutePath() + "/bin/proot --version",
            "cd " + prefix.getAbsolutePath() + " && TMPDIR=" + prefix.getAbsolutePath() + "/tmp PROOT_TMP_DIR=" + prefix.getAbsolutePath() + "/tmp PROOT_NO_SECCOMP=1 ./bin/proot -r ./var/lib/proot-distro/containers/ubuntu/rootfs /bin/bash -c 'echo proot-ok'",
        };

        for (String cmd : tests) {
            GladiatorLog.log(TAG, "$ " + cmd);
            try {
                ProcessBuilder pb = new ProcessBuilder(bash.getAbsolutePath(), "-c", cmd);
                pb.environment().put("PREFIX", prefix.getAbsolutePath());
                pb.environment().put("PATH", prefix.getAbsolutePath() + "/bin:" + prefix.getAbsolutePath() + "/bin/applets");
                pb.environment().put("HOME", new File(prefix, "home").getAbsolutePath());
                pb.environment().put("TMPDIR", new File(prefix, "tmp").getAbsolutePath());
                pb.environment().put("PROOT_TMP_DIR", new File(prefix, "tmp").getAbsolutePath());
                pb.environment().put("PROOT_NO_SECCOMP", "1");
                pb.environment().put("LD_LIBRARY_PATH", new File(prefix, "lib").getAbsolutePath());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    int n = 0;
                    while ((line = r.readLine()) != null && n++ < 20) {
                        sb.append(line).append('\n');
                    }
                }
                int rc = p.waitFor();
                GladiatorLog.log(TAG, "rc=" + rc);
                GladiatorLog.log(TAG, sb.toString());
            } catch (Throwable t) {
                GladiatorLog.err(TAG, "falló: " + cmd, t);
            }
            GladiatorLog.log(TAG, "---");
        }
    }
}
