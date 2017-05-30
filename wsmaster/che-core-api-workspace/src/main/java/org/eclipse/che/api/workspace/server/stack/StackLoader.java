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
package org.eclipse.che.api.workspace.server.stack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.api.workspace.shared.stack.Stack;
import org.eclipse.che.core.db.DBInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.che.core.db.DBInitializer.BARE_DB_INIT_PROPERTY_NAME;

/**
 * Class for loading list predefined {@link Stack} to the {@link StackDao}
 * and set {@link StackIcon} to the predefined stack.
 *
 * @author Alexander Andrienko
 * @author Sergii Leshchenko
 */
@Singleton
public class StackLoader {
    private static final Logger LOG = LoggerFactory.getLogger(StackLoader.class);

    public static final String CHE_PREDEFINED_STACKS = "che.predefined.stacks";
    public static final String CHE_PREDEFINED_STACKS_IMAGES = "che.predefined.stacks.images";

    protected final Path     stackIconFolderPath;
    protected final StackDao stackDao;

    private final Gson          GSON;
    private final String        stacksPath;
    private final DBInitializer dbInitializer;

    @Inject
    @SuppressWarnings("unused")
    public StackLoader(@Named(CHE_PREDEFINED_STACKS) String stacksPath,
                       @Named(CHE_PREDEFINED_STACKS_IMAGES) String stackIconFolder,
                       StackDao stackDao,
                       DBInitializer dbInitializer) {
        this.stacksPath = stacksPath;
        this.stackIconFolderPath = Paths.get(stackIconFolder);
        this.stackDao = stackDao;
        this.dbInitializer = dbInitializer;
        GSON = new GsonBuilder().create();
    }

    /**
     * Load predefined stacks with their icons to the {@link StackDao}.
     */
    @PostConstruct
    public void start() {
        if (Boolean.parseBoolean(dbInitializer.getInitProperties().get(BARE_DB_INIT_PROPERTY_NAME))) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResource(stacksPath)))) {
                List<StackImpl> stacks = GSON.fromJson(reader, new TypeToken<List<StackImpl>>() {}.getType());
                stacks.forEach(this::loadStack);
                LOG.info("Stacks successfully initialized");
            } catch (Exception ex) {
                LOG.error("Failed to store stacks ", ex);
            }
        }
    }

    protected void loadStack(StackImpl stack) {
        setIconData(stack, stackIconFolderPath);

        try {
            stackDao.update(stack);
        } catch (NotFoundException | ConflictException | ServerException e) {
            try {
                stackDao.create(stack);
            } catch (Exception ex) {
                LOG.error(format("Failed to load stack with id '%s' ", stack.getId()), ex.getMessage());
            }
        }
    }

    protected void setIconData(StackImpl stack, Path stackIconFolderPath) {
        StackIcon stackIcon = stack.getStackIcon();
        if (stackIcon == null) {
            return;
        }
        try {
            Path stackIconPath = stackIconFolderPath.resolve(stackIcon.getName());
            InputStream stackIconStream = getResource(stackIconPath.toString());
            stackIcon = new StackIcon(stackIcon.getName(), stackIcon.getMediaType(), IOUtils.toByteArray(stackIconStream));
            stack.setStackIcon(stackIcon);
        } catch (IOException e) {
            stack.setStackIcon(null);
            LOG.error(format("Failed to load stack icon data for the stack with id '%s'", stack.getId()), e);
        }
    }

    /**
     * Searches for resource by given path.
     *
     * @param resource
     *         path to resource
     * @return resource InputStream
     * @throws IOException
     *         when specified resource was not found
     */
    private InputStream getResource(String resource) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (is == null) {
            throw new IOException(String.format("Not found resource: %s", resource));
        }
        return is;
    }
}
