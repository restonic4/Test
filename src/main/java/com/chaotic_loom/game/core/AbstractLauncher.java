package com.chaotic_loom.game.core;

import com.chaotic_loom.game.core.util.ArgsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public abstract class AbstractLauncher {
    private static final Logger logger = LogManager.getLogger("Launcher");

    protected static void launch(AbstractEngine engine, String[] args) {
        logger.info("Launching {} engine", engine.getEnvironment());

        if (logger.isDebugEnabled()) {
            logger.debug("Launch arguments:");

            for (String arg : args) {
                logger.debug(arg);
            }
        }

        try {
            ArgsManager argsManager = new ArgsManager(args);

            argsManager.throwIfMissing("gamedir");

            engine.setRunPath(Path.of(argsManager.getValue("gamedir")));
            engine.run();
        } catch (Exception e) {
            logger.error("Critical Launcher error:", e);
            System.exit(-1);
        }
    }
}
