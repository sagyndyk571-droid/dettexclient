package neverlose.util.config.impl;

import neverlose.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class ConfigFileHandler {

    private final ReentrantReadWriteLock lock;

    public ConfigFileHandler() {
        this.lock = new ReentrantReadWriteLock();
    }

    public void createDirectories() {
        try {
            Files.createDirectories(ConfigPath.getConfigDirectory());
        } catch (IOException e) {
            Logger.error("AutoConfiguration: Failed to create directories!");
        }
    }

    public boolean write(String content) {
        return write(ConfigPath.getConfigFile(), content);
    }

    public boolean write(String name, String content) {
        return write(ConfigPath.getConfigFile(name), content);
    }

    public boolean write(Path configFile, String content) {
        lock.writeLock().lock();
        try {
            Files.createDirectories(configFile.getParent());
            Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");

            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return true;
        } catch (IOException e) {
            Logger.error("AutoConfiguration: Write failed! " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String read() {
        return read(ConfigPath.getConfigFile());
    }

    public String read(String name) {
        return read(ConfigPath.getConfigFile(name));
    }

    public String read(Path configFile) {
        lock.readLock().lock();
        try {
            if (!Files.exists(configFile)) {
                return null;
            }

            return Files.readString(configFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("AutoConfiguration: Read failed! " + e.getMessage());
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists() {
        return Files.exists(ConfigPath.getConfigFile());
    }

    public boolean exists(String name) {
        return Files.exists(ConfigPath.getConfigFile(name));
    }

    public boolean exists(Path configFile) {
        return Files.exists(configFile);
    }
}
