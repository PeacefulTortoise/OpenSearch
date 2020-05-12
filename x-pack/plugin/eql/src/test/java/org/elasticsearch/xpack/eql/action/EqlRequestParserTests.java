/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.eql.action;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsString;

public class EqlRequestParserTests extends ESTestCase {

    private static NamedXContentRegistry registry =
        new NamedXContentRegistry(new SearchModule(Settings.EMPTY, false, Collections.emptyList()).getNamedXContents());
    public void testUnknownFieldParsingErrors() throws IOException {
        assertParsingErrorMessage("{\"key\" : \"value\"}", "unknown field [key]", EqlSearchRequest::fromXContent);
    }

    public void testSearchRequestParser() throws IOException {
        assertParsingErrorMessage("{\"filter\" : 123}", "filter doesn't support values of type: VALUE_NUMBER",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"timestamp_field\" : 123}", "timestamp_field doesn't support values of type: VALUE_NUMBER",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"event_category_field\" : 123}", "event_category_field doesn't support values of type: VALUE_NUMBER",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"implicit_join_key_field\" : 123}",
            "implicit_join_key_field doesn't support values of type: VALUE_NUMBER",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"search_after\" : 123}", "search_after doesn't support values of type: VALUE_NUMBER",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"size\" : \"foo\"}", "failed to parse field [size]", EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"query\" : 123}", "query doesn't support values of type: VALUE_NUMBER",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"query\" : \"whatever\", \"size\":\"abc\"}", "failed to parse field [size]",
            EqlSearchRequest::fromXContent);
        assertParsingErrorMessage("{\"case_sensitive\" : \"whatever\"}", "failed to parse field [case_sensitive]",
            EqlSearchRequest::fromXContent);

        boolean setIsCaseSensitive = randomBoolean();
        boolean isCaseSensitive = randomBoolean();
        EqlSearchRequest request = generateRequest("endgame-*", "{\"filter\" : {\"match\" : {\"foo\":\"bar\"}}, "
            + "\"timestamp_field\" : \"tsf\", "
            + "\"event_category_field\" : \"etf\","
            + "\"implicit_join_key_field\" : \"imjf\","
            + "\"search_after\" : [ 12345678, \"device-20184\", \"/user/local/foo.exe\", \"2019-11-26T00:45:43.542\" ],"
            + "\"size\" : \"101\","
            + "\"query\" : \"file where user != 'SYSTEM' by file_path\""
            + (setIsCaseSensitive ? (",\"case_sensitive\" : " + isCaseSensitive) : "")
            + "}", EqlSearchRequest::fromXContent);
        assertArrayEquals(new String[]{"endgame-*"}, request.indices());
        assertNotNull(request.query());
        assertTrue(request.filter() instanceof MatchQueryBuilder);
        MatchQueryBuilder filter = (MatchQueryBuilder)request.filter();
        assertEquals("foo", filter.fieldName());
        assertEquals("bar", filter.value());
        assertEquals("tsf", request.timestampField());
        assertEquals("etf", request.eventCategoryField());
        assertEquals("imjf", request.implicitJoinKeyField());
        assertArrayEquals(new Object[]{12345678, "device-20184", "/user/local/foo.exe", "2019-11-26T00:45:43.542"}, request.searchAfter());
        assertEquals(101, request.fetchSize());
        assertEquals("file where user != 'SYSTEM' by file_path", request.query());
        assertEquals(setIsCaseSensitive && isCaseSensitive, request.isCaseSensitive());
    }

    private EqlSearchRequest generateRequest(String index, String json, Function<XContentParser, EqlSearchRequest> fromXContent)
            throws IOException {
        XContentParser parser = parser(json);
        return fromXContent.apply(parser).indices(new String[]{index});
    }

    private void assertParsingErrorMessage(String json, String errorMessage, Consumer<XContentParser> consumer) throws IOException {
        XContentParser parser = parser(json);
        Exception e = expectThrows(IllegalArgumentException.class, () -> consumer.accept(parser));
        assertThat(e.getMessage(), containsString(errorMessage));
    }

    private XContentParser parser(String content) throws IOException {
        XContentType xContentType = XContentType.JSON;

        return xContentType.xContent().createParser(registry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content);
    }
}
