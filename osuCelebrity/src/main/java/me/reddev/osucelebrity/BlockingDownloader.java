package me.reddev.osucelebrity;

import com.google.gson.JsonElement;

import lombok.extern.slf4j.Slf4j;

import org.tillerino.osuApiModel.Downloader;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public class BlockingDownloader extends Downloader {
  Map<String, CompletableFuture<JsonElement>> requests = new ConcurrentHashMap<>();

  public BlockingDownloader() {
    super();
  }

  public BlockingDownloader(String key) {
    super(key);
  }

  @Override
  public JsonElement get(String command, String... parameters) throws IOException {
    String request = formURL(false, command, parameters).toString();
    final boolean doRequest;
    CompletableFuture<JsonElement> fut;
    synchronized (this) {
      fut = requests.get(request);
      if (fut != null) {
        doRequest = false;
      } else {
        requests.put(request, fut = new CompletableFuture<>());
        doRequest = true;
      }
    }
    if (doRequest) {
      try {
        log.debug("downloading {}", request);
        fut.complete(super.get(command, parameters));
      } catch (Exception e) {
        fut.completeExceptionally(e);
        throw e;
      } finally {
        requests.remove(request);
      }
    }
    try {
      return fut.get();
    } catch (InterruptedException e) {
      throw new InterruptedIOException();
    } catch (ExecutionException e) {
      try {
        throw e.getCause();
      } catch (IOException e1) {
        throw e1;
      } catch (RuntimeException e1) {
        throw e1;
      } catch (Throwable e1) {
        throw new RuntimeException(e1);
      }
    }
  }
}
