/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server.recipe;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.spi.RecipeDao;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.core.db.DBInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptySet;
import static org.eclipse.che.core.db.DBInitializer.BARE_DB_INIT_PROPERTY_NAME;

/**
 * Loads predefined recipes.
 *
 * <p>It's used for machine template selection during
 * creation of workspace or creation any machine in workspace.
 *
 * @author Anton Korneta
 */
@Singleton
public class RecipeLoader {

    private static final Logger LOG  = LoggerFactory.getLogger(RecipeLoader.class);
    private static final Gson   GSON = new GsonBuilder().create();


    protected final RecipeDao recipeDao;

    private final Set<String>   recipesPaths;
    private final DBInitializer dbInitializer;


    @Inject
    @SuppressWarnings("unused")
    public RecipeLoader(@Nullable @Named("predefined.recipe.path") Set<String> recipesPaths,
                        RecipeDao recipeDao,
                        DBInitializer dbInitializer) {
        this.recipesPaths = firstNonNull(recipesPaths, emptySet());
        this.recipeDao = recipeDao;
        this.dbInitializer = dbInitializer;
    }

    @PostConstruct
    public void start() {
        if (Boolean.parseBoolean(dbInitializer.getInitProperties().get(BARE_DB_INIT_PROPERTY_NAME))) {
            for (String recipesPath : recipesPaths) {
                if (recipesPath != null && !recipesPath.isEmpty()) {
                    loadRecipes(recipesPath).forEach(this::doCreate);
                }
            }
        }
    }

    protected void doCreate(RecipeImpl recipe) {
        try {
            try {
                recipeDao.update(recipe);
            } catch (NotFoundException ex) {
                recipeDao.create(recipe);
            }
        } catch (ServerException | ConflictException ex) {
            LOG.error("Failed to store recipe {} ", recipe.getId(), ex.getMessage());
        }
    }

    /**
     * Loads recipes by specified path.
     *
     * @param recipesPath
     *         path to recipe file
     * @return list of predefined recipes or empty list
     * when failed to obtain recipes by given path
     */
    private List<RecipeImpl> loadRecipes(String recipesPath) {
        final List<RecipeImpl> recipes = new ArrayList<>();
        try (Reader reader = getResourceReader(recipesPath)) {
            recipes.addAll(GSON.fromJson(reader, new TypeToken<List<RecipeImpl>>() {}.getType()));
        } catch (IOException | JsonParseException ex) {
            LOG.error("Failed to deserialize recipes from specified path " + recipesPath, ex);
        }
        return recipes;
    }

    /**
     * Searches for resource by given path.
     *
     * @param resource
     *         path to resource
     * @return resource InputStream
     * @throws IOException
     *         when problem occurs during resource getting
     */
    private Reader getResourceReader(String resource) throws IOException {
        final Path path = Paths.get(resource);
        if (Files.isRegularFile(path)) {
            return Files.newBufferedReader(path);
        } else {
            final InputStream is = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream(resource);
            if (is == null) {
                throw new IOException(String.format("Not found resource: %s", resource));
            }
            return new BufferedReader(new InputStreamReader(is));
        }
    }

}
