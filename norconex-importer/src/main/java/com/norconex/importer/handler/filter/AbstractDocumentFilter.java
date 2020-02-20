/* Copyright 2010-2020 Norconex Inc.
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
package com.norconex.importer.handler.filter;

import java.io.InputStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * <p>Base class for document filters.  Subclasses can be set an attribute
 * called "onMatch".  The logic whether to include or exclude a document
 * upon matching it is handled by this class.  Subclasses only
 * need to focus on whether the document gets matched or not by
 * implementing the
 * {@link #isDocumentMatched(String, InputStream, Properties, boolean)}
 * method.</p>
 *
 * <h3 id="logic">Inclusion/exclusion logic:</h3>
 * <p>The logic for accepting or rejecting documents when a subclass condition
 * is met ("matches") is as follow:</p>
 * <table border="1" summary="Inclusion/exclusion logic">
 *  <tr>
 *   <td><b>Matches?</b></td>
 *   <td><b>On match</b></td>
 *   <td><b>Expected behavior</b></td>
 *  </tr>
 *  <tr>
 *   <td>yes</td><td>exclude</td><td>Document is rejected.</td>
 *  </tr>
 *  <tr>
 *   <td>yes</td><td>include</td><td>Document is accepted.</td>
 *  </tr>
 *  <tr>
 *   <td>no</td><td>exclude</td><td>Document is accepted.</td>
 *  </tr>
 *  <tr>
 *   <td>no</td><td>include</td>
 *   <td>Document is accepted if it was accepted by at least one filter with
 *       onMatch="include". If no other one exists or if none matched,
 *       the document is rejected.</td>
 *  </tr>
 * </table>
 * <p>
 * When multiple filters are defined and a combination of both "include" and
 * "exclude" are possible, the "exclude" will always take precedence.
 * In other words, it only take one matching "exclude" to reject a document,
 * not matter how many matching "include" were triggered.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *  onMatch="[include|exclude]"
 * }
 *
 * <p>
 * Subclasses inherit the above {@link IXMLConfigurable} attribute(s),
 * in addition to <a href="../AbstractImporterHandler.html#nx-xml-restrictTo">
 * &lt;restrictTo&gt;</a>.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public abstract class AbstractDocumentFilter extends AbstractImporterHandler
            implements IDocumentFilter, IOnMatchFilter {

    private OnMatch onMatch = OnMatch.INCLUDE;

    @Override
    public OnMatch getOnMatch() {
        return onMatch;
    }

    public final void setOnMatch(OnMatch onMatch) {
        this.onMatch = onMatch;
    }

    @Override
    public boolean acceptDocument(String reference,
            InputStream input, Properties metadata,
            boolean parsed) throws ImporterHandlerException {

        if (!isApplicable(reference, metadata, parsed)) {
            return true;
        }

        boolean matched = isDocumentMatched(reference, input, metadata, parsed);

        OnMatch safeOnMatch = OnMatch.includeIfNull(onMatch);
        if (matched) {
            return safeOnMatch == OnMatch.INCLUDE;
        } else {
            return safeOnMatch == OnMatch.EXCLUDE;
        }
    }

    protected abstract boolean isDocumentMatched(
            String reference, InputStream input,
            Properties metadata, boolean parsed)
                    throws ImporterHandlerException;

    @Override
    protected final void saveHandlerToXML(XML xml) {
        xml.setAttribute("onMatch", onMatch);
        saveFilterToXML(xml);
    }
    protected abstract void saveFilterToXML(XML xml);

    @Override
    protected final void loadHandlerFromXML(XML xml) {
        setOnMatch(xml.getEnum("@onMatch", OnMatch.class, onMatch));
        loadFilterFromXML(xml);
    }
    protected abstract void loadFilterFromXML(XML xml);

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}