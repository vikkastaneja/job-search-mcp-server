package com.jobsearch.mcpserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

 @JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
   private String jsonrpc;
   private Object id;
   private JsonNode result;
   private ErrorObject error;

   public String getJsonrpc() {
      return jsonrpc;
   }

   public void setJsonrpc(String jsonrpc) {
      this.jsonrpc = jsonrpc;
   }

   public Object getId() {
      return id;
   }

   public void setId(Object id) {
      this.id = id;
   }

   public JsonNode getResult() {
      return result;
   }

   public void setResult(JsonNode result) {
      this.result = result;
   }

   public ErrorObject getError() {
      return error;
   }

   public void setError(ErrorObject error) {
      this.error = error;
   }
}
