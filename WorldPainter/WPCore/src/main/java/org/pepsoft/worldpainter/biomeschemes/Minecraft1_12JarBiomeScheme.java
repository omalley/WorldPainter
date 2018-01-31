/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.jnbt.CompoundTag;
import org.pepsoft.minecraft.MCInterface;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Checksum;
import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.BiomeScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * An abstract base class for {@link BiomeScheme}s which can invoke Minecraft
 * code from a Minecraft jar file for version 1.12 and later to calculate biomes.
 *
 * @author pepijn
 */
public abstract class Minecraft1_12JarBiomeScheme extends AbstractMinecraft1_7BiomeScheme implements MCInterface {
    public Minecraft1_12JarBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum, Map<Checksum, String[]> hashesToClassNames) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating biome scheme using Minecraft jar {}", minecraftJar);
        }
        if (md5Sum == null) {
            try {
                md5Sum = FileUtils.getMD5(minecraftJar);
            } catch (IOException e) {
                throw new RuntimeException("I/O error calculating hash for " + minecraftJar, e);
            }
        }
        try {
            init(hashesToClassNames.get(md5Sum), getClassLoader(minecraftJar, libDir));
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException("Not a valid minecraft.jar of the correct version", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access denied while trying to initialise Minecraft", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown while trying to initialise Minecraft", e);
        }
    }

    @Override
    public void setSeed(long seed) {
        if ((landscape == null) || (seed != this.seed)) {
            try {
                landscape = ((Object[]) getLandscapesMethod.invoke(null, seed, null, null))[1];
                this.seed = seed;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied while trying to set the seed", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Exception thrown while trying to set the seed", e);
            }
        }
    }
    
    @Override
    public final synchronized void getBiomes(int x, int y, int width, int height, int[] buffer) {
        try {
            int[] biomes = (int[]) getBiomesMethod.invoke(landscape, x, y, width, height);
            clearBuffersMethod.invoke(null);
            System.arraycopy(biomes, 0, buffer, 0, Math.min(biomes.length, buffer.length));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access denied while trying to calculate biomes", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown while trying to calculate biomes", e);
        }
    }

    @Override
    public final Material decodeStructureMaterial(CompoundTag tag) {
        return helper.decodeStructureMaterial(tag);
    }

    protected void init(String[] classNames, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        String landscapeClassName             = classNames[ 0];
        String bufferManagerClassName         = classNames[ 1];
        String worldGeneratorClassName        = classNames[ 2];
        String initClassName                  = classNames[ 3];
        String blockDataClassName             = classNames[ 4];
        String blockClassName                 = classNames[ 5];
        String nbtTagClassName                = classNames[ 6];
        String nbtCompoundTagClassName        = classNames[ 7];
        String nbtListTagClassName            = classNames[ 8];
        String nbtStringTagClassName          = classNames[ 9];
        String gameProfileSerializerClassName = classNames[10];
        String generatorSettingsClassName     = classNames[11];
        Class<?> landscapeClass = classLoader.loadClass(landscapeClassName);
        worldGeneratorClass = classLoader.loadClass(worldGeneratorClassName);
        Class<?> generatorSettingsClass = classLoader.loadClass(generatorSettingsClassName);
        getLandscapesMethod = landscapeClass.getMethod("a", long.class, worldGeneratorClass, generatorSettingsClass);
        getBiomesMethod = landscapeClass.getMethod("a", int.class, int.class, int.class, int.class);
        Class<?> bufferManagerClass = classLoader.loadClass(bufferManagerClassName);
        clearBuffersMethod = bufferManagerClass.getMethod("a");
        Class<?> blockDataClass = classLoader.loadClass(blockDataClassName);
        Class<?> blockClass = classLoader.loadClass(blockClassName);
        Class<?> nbtTagClass = classLoader.loadClass(nbtTagClassName);
        Class<?> nbtCompoundTagClass = classLoader.loadClass(nbtCompoundTagClassName);
        Class<?> nbtListTagClass = classLoader.loadClass(nbtListTagClassName);
        Class<?> nbtStringTagClass = classLoader.loadClass(nbtStringTagClassName);
        Class<?> gameProfileSerializerClass = classLoader.loadClass(gameProfileSerializerClassName);

        helper = new MC10InterfaceHelper(nbtCompoundTagClass,
                nbtCompoundTagClass.getMethod("a", String.class, nbtTagClass),
                nbtListTagClass,
                nbtListTagClass.getMethod("a", nbtTagClass),
                nbtStringTagClass.getConstructor(String.class),
                gameProfileSerializerClass.getMethod("d", nbtCompoundTagClass),
                blockDataClass.getMethod("u"),
                blockClass.getMethod("a", blockClass),
                blockClass.getMethod("e", blockDataClass));

        // Initialise Minecraft
        Class<?> initClass = classLoader.loadClass(initClassName);
        Method initMethod = initClass.getMethod("c");
        initMethod.invoke(null);
    }

    Class<?> worldGeneratorClass;
    Method getLandscapesMethod, getBiomesMethod, clearBuffersMethod;
    Object landscape;
    long seed = Long.MIN_VALUE;
    private MC10InterfaceHelper helper;

    private static final Logger logger = LoggerFactory.getLogger(Minecraft1_12JarBiomeScheme.class);
}