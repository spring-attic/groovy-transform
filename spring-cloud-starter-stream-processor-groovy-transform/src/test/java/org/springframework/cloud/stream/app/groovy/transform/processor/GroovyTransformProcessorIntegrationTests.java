/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.groovy.transform.processor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MimeType;

/**
 * Integration Tests for the Groovy Transform Processor.
 *
 * @author Eric Bottard
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 * @author Haytham Mohamed
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public abstract class GroovyTransformProcessorIntegrationTests {

	@Autowired
	protected Processor channels;

	@Autowired
	protected MessageCollector collector;

	@TestPropertySource(properties = {
			"groovy-transformer.script=script.groovy",
			"groovy-transformer.variables=limit=5\\n foo=\\\\\40WORLD" })
	public static class UsingScriptIntegrationTests extends GroovyTransformProcessorIntegrationTests {

		@Test
		public void test() {
			channels.input().send(new GenericMessage<Object>("hello world"));
			assertThat(collector.forChannel(channels.output()), receivesPayloadThat(is("hello WORLD")));
		}

	}

	@TestPropertySource(properties = {
			"groovy-transformer.script=script-convert-outbound-payload-type-to-string.groovy",
			"spring.cloud.stream.bindings.output.contentType=text/plain" })
	public static class MisalignedOutboundPayloadAndContentTypeTypes extends GroovyTransformProcessorIntegrationTests {

		@Test
		public void test() throws InterruptedException {
			// The inbound message with byte-array payload and content-type=octet-stream and outbound payload is String
			Map<String, Object> inboundHeaders = Collections
					.singletonMap(MessageHeaders.CONTENT_TYPE, "application/octet-stream");
			channels.input().send(new GenericMessage<Object>("hello world".getBytes(), inboundHeaders));

			Message<?> outboundMessage = collector.forChannel(channels.output()).take();
			assertThat(outboundMessage.getPayload(), is("HELLO WORLD"));
			// Outbound Header contentType should match the spring.cloud.stream.bindings.output.contentType property
			// or defaults to application/json if not set explicitly.
			assertThat("Outbound Header contentType should match the spring.cloud.stream.bindings.output.contentType",
					outboundMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE), is(MimeType.valueOf("text/plain")));
		}

	}

	@TestPropertySource(properties = { "groovy-transformer.script=script-with-grab.groovy" })
	public static class UsingScriptWithGrabIntegrationTests extends GroovyTransformProcessorIntegrationTests {

		@Test
		public void test() {
			channels.input().send(new GenericMessage<Object>("def age=18"));
			assertThat(collector.forChannel(channels.output()),
					receivesPayloadThat(is("var age = 18;" + System.lineSeparator())));
		}

	}

	@SpringBootApplication
	public static class GroovyTransformProcessorApplication {

	}

}
