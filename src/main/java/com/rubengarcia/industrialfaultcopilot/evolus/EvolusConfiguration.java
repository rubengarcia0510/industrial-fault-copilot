package com.rubengarcia.industrialfaultcopilot.evolus;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EvolusProperties.class)
public class EvolusConfiguration {
}
