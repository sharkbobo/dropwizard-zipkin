/**
 * Copyright 2017 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.dropwizard.zipkin;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.net.InetAddresses;
import brave.Tracing;
import brave.context.slf4j.MDCCurrentTraceContext;
import brave.jaxrs2.TracingFeature;
import brave.sampler.Sampler;
import io.dropwizard.setup.Environment;
import io.dropwizard.validation.PortRange;
import zipkin.Endpoint;
import zipkin.Span;
import zipkin.reporter.Reporter;

/**
 * @see ConsoleZipkinFactory
 * @see EmptyZipkinFactory
 * @see HttpZipkinFactory
 * @see KafkaZipkinFactory
 * @see LoggingZipkinFactory
 * @see ScribeZipkinFactory
 */
public abstract class AbstractZipkinFactory implements ZipkinFactory {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractZipkinFactory.class);
    private static final int DEFAULT_DW_PORT = 8080;

    private boolean enabled = true;

    private String serviceName;

    @NotEmpty
    private String serviceHost = "127.0.0.1";

    @PortRange
    private int servicePort = DEFAULT_DW_PORT;

    @Min(0)
    @Max(1)
    private float sampleRate = 1.0f;

    private Sampler sampler = null;

    private boolean traceId128Bit = false;

    @JsonProperty
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    @JsonProperty
    public String getServiceName() {
        return serviceName;
    }

    @Override
    @JsonProperty
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @JsonProperty
    public String getServiceHost() {
        return serviceHost;
    }

    @JsonProperty
    public void setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
    }

    @JsonProperty
    public int getServicePort() {
        return servicePort;
    }

    @JsonProperty
    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    @JsonProperty
    public float getSampleRate() {
        return sampleRate;
    }

    @JsonProperty
    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    @JsonIgnore
    public Sampler getSampler() {
        if (sampler == null) {
            return Sampler.create(sampleRate);
        }
        return sampler;
    }

    @JsonIgnore
    public void setSampler(Sampler sampler) {
        this.sampler = sampler;
    }

    @JsonProperty
    public boolean getTraceId128Bit() {
        return traceId128Bit;
    }

    @JsonProperty
    public void setTraceId128Bit(boolean traceId128Bit) {
        this.traceId128Bit = traceId128Bit;
    }

    private static int toInt(final String ip) {
        return InetAddresses.coerceToInteger(InetAddresses.forString(ip));
    }

    /**
     * Build a new {@link Tracing} instance for interfacing with Zipkin
     *
     * @param environment
     *            Environment
     * @param reporter
     *            reporter
     * @return Tracing instance
     */
    protected Optional<Tracing> buildTracing(
            @Nonnull final Environment environment,
            @Nonnull final Reporter<Span> reporter) {

        LOGGER.info("Registering Zipkin service ({}) at <{}:{}>", serviceName,
                serviceHost, servicePort);

        final Endpoint endpoint = Endpoint.builder().ipv4(toInt(serviceHost))
                .port(servicePort).serviceName(serviceName).build();

        final Tracing tracing = Tracing.newBuilder()
                .currentTraceContext(MDCCurrentTraceContext.create())
                .localServiceName(serviceName).localEndpoint(endpoint)
                .reporter(reporter).sampler(getSampler())
                .traceId128Bit(traceId128Bit).build();

        // Register the tracing feature for client and server requests
        environment.jersey().register(TracingFeature.create(tracing));

        return Optional.of(tracing);
    }
}
