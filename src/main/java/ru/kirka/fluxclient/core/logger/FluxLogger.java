package ru.kirka.fluxclient.core.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FluxLogger {
   private static final Logger LOGGER = LoggerFactory.getLogger("FluxClient");

   public static void info(String message) {
      LOGGER.info("[Flux] " + message);
   }

   public static void warn(String message) {
      LOGGER.warn("[Flux] " + message);
   }

   public static void error(String message) {
      LOGGER.error("[Flux] " + message);
   }

   public static void error(String message, Throwable t) {
      LOGGER.error("[Flux] " + message, t);
   }
}
