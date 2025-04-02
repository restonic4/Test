package com.chaotic_loom.game.core;

import com.chaotic_loom.game.world.components.Block;
import com.chaotic_loom.game.core.util.ArgsManager;
import com.chaotic_loom.game.networking.NetworkingManager;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.built_in.RegistryKeys;
import com.chaotic_loom.game.registries.components.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Map;

public abstract class AbstractEngine implements Runnable {
    protected final Environment environment;
    private final Logger logger;
    private final NetworkingManager networkingManager;

    private ArgsManager argsManager;
    private Path runPath;

    public AbstractEngine(Environment environment) {
        this.environment = environment;
        this.logger = LogManager.getLogger("Engine/" + environment);
        this.networkingManager = new NetworkingManager();

        Registry.startRegistrationAnnotationCollection(Environment.COMMON);
        Registry.startRegistrationAnnotationCollection(environment);

        Map<Identifier, Block> registries = Registry.getRegistrySet(RegistryKeys.BLOCK);
        for (Block block : registries.values()) {
            System.out.println(block.getIdentifier() + " -> " + block.getInternalMappedRegistryID());
        }
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

    public NetworkingManager getNetworkingManager() {
        return networkingManager;
    }

    public Path getRunPath() {
        return runPath;
    }

    public void setArgsManager(ArgsManager argsManager) {
        this.argsManager = argsManager;
    }

    public ArgsManager getArgsManager() {
        return argsManager;
    }

    @Override
    public void run() {
        try {
            runPath = Path.of(argsManager.getValue("gamedir"));

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
