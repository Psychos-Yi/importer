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
package com.norconex.importer.handler.transformer.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.impl.StripBetweenTransformer.StripBetweenDetails;
import com.norconex.importer.parser.ParseState;

public class StripBetweenTransformerTest {

    @Test
    public void testTransformTextDocument()
            throws ImporterHandlerException, IOException {
        StripBetweenTransformer t = new StripBetweenTransformer();
        addEndPoints(t, "<h2>", "</h2>");
        addEndPoints(t, "<P>", "</P>");
        addEndPoints(t, "<head>", "</hEad>");
        addEndPoints(t, "<Pre>", "</prE>");

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        t.transformDocument(
                TestUtil.toHandlerDoc(htmlFile.getAbsolutePath(), is, metadata),
                is, os, ParseState.PRE);

        Assertions.assertEquals(458, os.toString().length(),
                "Length of doc content after transformation is incorrect.");

        is.close();
        os.close();
    }


    @Test
    public void testCollectorHttpIssue237()
            throws ImporterHandlerException, IOException {
        StripBetweenTransformer t = new StripBetweenTransformer();
        addEndPoints(t, "<body>", "<\\!-- START -->");
        addEndPoints(t, "<\\!-- END -->", "<\\!-- START -->");
        addEndPoints(t, "<\\!-- END -->", "</body>");

        String html = "<html><body>"
                + "ignore this text"
                + "<!-- START -->extract me 1<!-- END -->"
                + "ignore this text"
                + "<!-- START -->extract me 2<!-- END -->"
                + "ignore this text"
                + "</body></html>";

        ByteArrayInputStream is = new ByteArrayInputStream(html.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        t.transformDocument(TestUtil.toHandlerDoc("fake.html", is, metadata),
                is, os, ParseState.PRE);
        String output = os.toString();
        is.close();
        os.close();
        //System.out.println(output);
        Assertions.assertEquals(
                "<html>extract me 1extract me 2</html>", output);
    }


    @Test
    public void testWriteRead() {
        StripBetweenTransformer t = new StripBetweenTransformer();
        addEndPoints(t, "<!-- NO INDEX", "/NOINDEX -->");
        addEndPoints(t, "<!-- HEADER START", "HEADER END -->");
        XML.assertWriteRead(t, "handler");
    }

    private void addEndPoints(
            StripBetweenTransformer t, String start, String end) {
        StripBetweenDetails d = new StripBetweenDetails(
                TextMatcher.regex(start).setIgnoreCase(true),
                TextMatcher.regex(end).setIgnoreCase(true));
        d.setInclusive(true);
        t.addStripBetweenDetails(d);
    }
}
