package com.chaotic_loom.game.core;

import com.chaotic_loom.game.registries.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public abstract class AbstractEngine implements Runnable {
    protected final Environment environment;
    private final Logger logger;
    private Path runPath;

    public AbstractEngine(Environment environment) {
        this.environment = environment;
        this.logger = LogManager.getLogger("Engine/" + environment);

        Registry.startRegistrationAnnotationCollection(Environment.COMMON);
        Registry.startRegistrationAnnotationCollection(environment);
    }

    // Abstract methods to be implemented by Client/Server specific engines
    protected abstract void init() throws Exception;
    protected abstract void gameLoop() throws Exception;
    protected abstract void cleanup();
    protected abstract Timer getTimer();

    public Logger getLogger() {
        return logger;
    }
    public Environment getEnvironment() {
        return environment;
    }

    public Path getRunPath() {
        return runPath;
    }

    public void setRunPath(Path runPath) {
        this.runPath = runPath;
    }

    @Override
    public void run() {
        try {
            logger.info("Initializing {} engine on {}...", environment, getRunPath());
            init();

            logger.info("Starting game loop");
            gameLoop();
        } catch (Exception exception) {
            logger.error("Critical engine error in {}:", environment, exception);
        } finally {
            logger.info("Cleanup started");
            cleanup();
        }
    }
}
