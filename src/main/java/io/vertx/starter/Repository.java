package io.vertx.starter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {

  private String name;
  private String description;
  @JsonProperty("html_url")
  private String htmlUrl;

  public String getName() {
    return name;
  }

  public Repository setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Repository setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getHtmlUrl() {
    return htmlUrl;
  }

  public Repository setHtmlUrl(String htmlUrl) {
    this.htmlUrl = htmlUrl;
    return this;
  }

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }
}
