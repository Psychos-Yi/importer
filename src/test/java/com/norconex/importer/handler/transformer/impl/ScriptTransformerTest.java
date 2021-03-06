/* Copyright 2015-2020 Norconex Inc.
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.parser.ParseState;

public class ScriptTransformerTest {

    @Test
    public void testLua() throws ImporterHandlerException, IOException {
        testScriptTransformer("lua",
                "metadata:add('test', {'success'});"
              + "local text = content:gsub('Alice', 'Roger');"
              + "return text;"
        );
    }

    @Test
    public void testJavaScript()
            throws ImporterHandlerException, IOException {
        testScriptTransformer(ScriptRunner.DEFAULT_SCRIPT_ENGINE,
                "metadata.add('test', 'success');"
              + "text = content.replace(/Alice/g, 'Roger');"
              + "/*return*/ text;"
        );
    }

    // https://github.com/Norconex/collector-http/issues/665
    @Test
    public void testContentModify()
            throws ImporterHandlerException, UnsupportedEncodingException {

        ScriptTransformer t = new ScriptTransformer();
        t.setEngineName(ScriptRunner.DEFAULT_SCRIPT_ENGINE);
        t.setScript(
                "var ct = metadata.getString('document.contentType');\n"
              + "if (ct != null && ct == 'text/html') {\n"
              + "    if (content != null) {\n"
              + "        content = 'Hello ' + content;\n"
              + "    }\n"
              + "}\n");
        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream is = IOUtils.toInputStream(
                "World!", StandardCharsets.UTF_8);
        t.transformDocument(
                TestUtil.toHandlerDoc("N/A", is, metadata),
                is, out, ParseState.POST);
        String content = out.toString(StandardCharsets.UTF_8.toString());
        Assertions.assertEquals("Hello World!", content);
    }

    private void testScriptTransformer(String engineName, String script)
            throws ImporterHandlerException, IOException {
        ScriptTransformer t = new ScriptTransformer();
        t.setEngineName(engineName);
        t.setScript(script);

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        t.transformDocument(
                TestUtil.toHandlerDoc(htmlFile.getAbsolutePath(), is, metadata),
                is, out, ParseState.PRE);
        is.close();

        String successField = metadata.getString("test");

        Assertions.assertEquals("success", successField);
        String content = new String(out.toString());

        Assertions.assertEquals(0, StringUtils.countMatches(content, "Alice"));
        Assertions.assertEquals(34, StringUtils.countMatches(content, "Roger"));
    }

    @Test
    public void testWriteRead() {
        ScriptTransformer t = new ScriptTransformer();
        t.setScript("a script");
        t.setEngineName("an engine name");
        t.setMaxReadSize(256);
        XML.assertWriteRead(t, "handler");
    }
}
