package com.mystyle.portfolio.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final int code;
  private final HttpStatus status;

  public ApiException(HttpStatus status, int code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }

  public static ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, 40404, message);
  }

  public static ApiException badRequest(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, 40000, message);
  }
}
