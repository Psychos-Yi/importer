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

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.io.IOUtil;
import com.norconex.commons.lang.io.TextReader;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Base class to facilitate creating filters based on text content, loading
 * text into {@link StringBuilder} for memory processing.
 * </p>
 *
 * <p><b>Since 2.2.0</b> this class limits the memory used for content
 * filtering by reading one section of text at a time.  Each
 * sections are sent for filtering once they are read until a match is found.
 * No two sections exists in memory at once.  Sub-classes should
 * respect this approach.  Each section have a maximum number of characters
 * equal to the maximum read size defined using {@link #setMaxReadSize(int)}.
 * When none is set, the default read size is defined by
 * {@link TextReader#DEFAULT_MAX_READ_SIZE}.
 * </p>
 *
 * <p>An attempt is made to break sections nicely after a paragraph, sentence,
 * or word.  When not possible, long text will be cut at a size equal
 * to the maximum read size.
 * </p>
 *
 * <p>
 * <b>Since 3.0.0</b> the
 * {@link #isStringContentMatching(HandlerDoc, StringBuilder, ParseState, int)}
 * method is invoked at least once, even if there is no content. This gives
 * subclasses a chance to act on metadata even if there is no content.
 * </p>
 *
 * <p>
 * Implementors should be conscious about memory when dealing with the string
 * builder.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *   maxReadSize="(max characters to read at once)"
 *   {@nx.include com.norconex.importer.handler.filter.AbstractCharStreamFilter#attributes}
 * }
 *
 * <p>
 * Subclasses inherit the above {@link IXMLConfigurable} attribute(s),
 * in addition to <a href="../AbstractImporterHandler.html#nx-xml-restrictTo">
 * &lt;restrictTo&gt;</a>.
 * </p>
 *
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public abstract class AbstractStringFilter
            extends AbstractCharStreamFilter {

    private int maxReadSize = TextReader.DEFAULT_MAX_READ_SIZE;

    @Override
    protected final boolean isTextDocumentMatching(
            HandlerDoc doc, Reader input, ParseState parseState)
            throws ImporterHandlerException {

        int sectionIndex = 0;
        StringBuilder b = new StringBuilder();
        String text = null;
        try (TextReader reader = new TextReader(
                IOUtil.toNonNullReader(input), maxReadSize)) {
            while ((text = reader.readText()) != null) {
                b.append(text);
                boolean matched = isStringContentMatching(
                        doc, b, parseState, sectionIndex);
                sectionIndex++;
                b.setLength(0);
                if (matched) {
                    return true;
                }
            }
            // should have been incremented at least once if there is content
            if (sectionIndex == 0) {
                return isStringContentMatching(
                        doc, b, parseState, sectionIndex);
            }
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Cannot filter text document.", e);
        }
        b.setLength(0);
        b = null;
        return false;
    }

    /**
     * Gets the maximum number of characters to read for filtering
     * at once. Default is {@link TextReader#DEFAULT_MAX_READ_SIZE}.
     * @return maximum read size
     */
    public int getMaxReadSize() {
        return maxReadSize;
    }
    /**
     * Sets the maximum number of characters to read for filtering
     * at once.
     * @param maxReadSize maximum read size
     */
    public void setMaxReadSize(int maxReadSize) {
        this.maxReadSize = maxReadSize;
    }

    protected abstract boolean isStringContentMatching(
            HandlerDoc doc, StringBuilder content,
            ParseState parseState, int sectionIndex)
                   throws ImporterHandlerException;


    @Override
    protected final void saveCharStreamFilterToXML(XML xml) {
        xml.setAttribute("maxReadSize", maxReadSize);
        saveStringFilterToXML(xml);
    }
    /**
     * Saves configuration settings specific to the implementing class.
     * The parent tag along with the "class" attribute are already written.
     * Implementors must not close the writer.
     *
     * @param xml the XML
     */
    protected abstract void saveStringFilterToXML(XML xml);

    @Override
    protected final void loadCharStreamFilterFromXML(XML xml) {
        setMaxReadSize(xml.getInteger("@maxReadSize", maxReadSize));
        loadStringFilterFromXML(xml);
    }
    /**
     * Loads configuration settings specific to the implementing class.
     * @param xml XML configuration
     */
    protected abstract void loadStringFilterFromXML(XML xml);

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