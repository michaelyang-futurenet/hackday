package com.future.hackday.rest;

import com.future.hackday.rest.ocrmatch.FindURLsOfPagesWithTextRESTController;
import com.google.common.collect.ImmutableSet;
import io.swagger.jaxrs.config.BeanConfig;
import javax.ws.rs.core.Application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

public class HackdayRestApplication extends Application {
    public HackdayRestApplication() {
        final BeanConfig beanConfig = new BeanConfig();

        try {
            beanConfig.setHost("hackday." + InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException uhe) {
        }

        beanConfig.setBasePath("/hackday/rest");
        beanConfig.setContextId("hackday.1");

        beanConfig.setScan(true);
    }

    @Override
    public Set<Class<?>> getClasses() {

            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
            Set<Class<?>> serviceClasses = ImmutableSet.of(FindURLsOfPagesWithTextRESTController.class);

        return ImmutableSet.<Class<?>>builder().addAll(serviceClasses).build();
    }
}
