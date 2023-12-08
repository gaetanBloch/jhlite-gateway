package com.mycompany.myapp.shared.error.infrastructure.primary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Utility class for HTTP headers creation.
 */
public final class HeaderUtil {

  private static final Logger log = LoggerFactory.getLogger(HeaderUtil.class);

  private HeaderUtil() {}

   /**
   * <p>createFailureAlert.</p>
   *
   * @param applicationName a {@link String} object.
   * @param entityName a {@link String} object.
   * @param errorKey a {@link String} object.
   * @return a {@link HttpHeaders} object.
   */
  public static HttpHeaders createFailureAlert(
    String applicationName,
    String entityName,
    String errorKey
  ) {
    String message = "error." + errorKey;
    log.error("Entity processing failed, {}", message);

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-" + applicationName + "-error", message);
    headers.add("X-" + applicationName + "-params", entityName);
    
    return headers;
  }
}
