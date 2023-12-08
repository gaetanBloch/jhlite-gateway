package com.mycompany.myapp.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import com.mycompany.myapp.UnitTest;

@UnitTest
class HeaderUtilTest {

  @Test
  void shouldCreateFailureAlertWithTranslation() {
    HttpHeaders headers = HeaderUtil.createFailureAlert("myApp", "User", "404");
    
    assertThat(headers.getFirst("X-myApp-error")).isEqualTo("error.404");
    assertThat(headers.getFirst("X-myApp-params")).isEqualTo("User");
  }
}
