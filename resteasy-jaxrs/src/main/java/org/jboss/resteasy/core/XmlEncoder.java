/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.core;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) we may need to move this or make it package private
public class XmlEncoder implements Encoder {
    // https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html

    /*
    RULE #2
    Except for alphanumeric characters, encode all characters with ASCII values less than 256 with the "&#xHH;" format
    (or a named entity if available) to prevent switching out of the attribute.
     */

    @Override
    public String encode(final String value) {
        if (value == null) {
            return null;
        }
        // TODO (jrp) check performance here
        final StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    result.append("&quot;");
                    break;
                case '\'':
                    result.append("&#39;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
}
