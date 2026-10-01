package com.google.android.exoplayer2.upstream;

import java.io.IOException;

/** Checks an HTTP request before a transport opens its URL or follows a redirect. */
public interface RequestUrlGate {
  void checkRequest(String url, String method) throws IOException;
}
