package ca.denisab85.telegram.restapi.server.domain.model.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetStateResponse {

  private boolean running;

  private boolean authenticated;

}