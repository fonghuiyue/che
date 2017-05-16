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
package org.eclipse.che.plugin.testing.ide.messages;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import org.eclipse.che.api.testing.shared.Constants;
import org.eclipse.che.api.testing.shared.messages.TestingMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 */
public class ClientTestingMessage implements TestingMessage {

    static final Map<String, Supplier<? extends ClientTestingMessage>> messageConstructors = new HashMap<>();

    private String name;
    private Map<String, String> attributes = new HashMap<>();

    ClientTestingMessage() {
    }

    public static ClientTestingMessage parse(String json) {
        try {
            JsonObject jsonObject = Json.parse(json);
            String name = jsonObject.getString(Constants.NAME);
            Supplier<? extends ClientTestingMessage> supplier = messageConstructors.get(name);
            if (supplier == null) {
                supplier = ClientTestingMessage::new;
            }

            ClientTestingMessage message = supplier.get();
            message.init(name, jsonObject.getObject(Constants.ATTRIBUTES));

            return message;
        } catch (JsonException e) {
            return null;
        }
    }

    private void init(String name, JsonObject object) {
        this.name = name;
        for (String key : object.keys()) {
            attributes.put(key, object.getString(key));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void visit(TestingMessageVisitor visitor) {
        visitor.visitTestingMessage(this);
    }

    protected String getAttributeValue(String attribute) {
        return getAttributes().get(attribute);
    }
}
