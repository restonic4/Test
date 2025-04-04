package com.chaotic_loom.game.core;

import com.chaotic_loom.game.core.util.ArgsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public abstract class AbstractLauncher {
    private static AbstractEngine engine;

    protected static void launch(AbstractEngine engine, String[] args) {
        Loggers.LAUNCHER.info("Launching {} engine", engine.getEnvironment());

        AbstractLauncher.engine = engine;

        if (Loggers.LAUNCHER.isDebugEnabled()) {
            Loggers.LAUNCHER.debug("Launch arguments:");

            for (String arg : args) {
                Loggers.LAUNCHER.debug(arg);
            }
        }

        try {
            ArgsManager argsManager = new ArgsManager(args);

            argsManager.throwIfMissing("gamedir");

            engine.setArgsManager(argsManager);
            engine.run();
        } catch (Exception e) {
            Loggers.LAUNCHER.error("Critical Launcher error:", e);
            System.exit(-1);
        }
    }

    public static AbstractEngine getEngine() {
        return engine;
    }
}
