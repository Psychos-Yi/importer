/* Copyright 2014-2020 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;

public class CopyTaggerTest {

    @Test
        public void testWriteRead() {
        CopyTagger tagger = new CopyTagger();
        tagger.addCopyDetails(new TextMatcher("from1"), "to1",
                PropertySetter.OPTIONAL);
        tagger.addCopyDetails(new TextMatcher("from2"), "to2", null);
        XML.assertWriteRead(tagger, "handler");
    }
}
