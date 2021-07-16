// Returns [-1, -1] if the key is not in the map
Point key_to_point(int key) {
  Point result;
  result.x = -1;
  result.y = -1;

  switch(key) {
    // Movement
  case SDLK_w:
    client_log("DEBUG: up");
    result.x = 226;
    result.y = 439;
    break;
  case SDLK_a:
    client_log("DEBUG: left");
    result.x = 116;
    result.y = 542;
    break;
  case SDLK_s:
    client_log("DEBUG: down");
    result.x = 212;
    result.y = 643;
    break;
  case SDLK_d:
    client_log("DEBUG: right");
    result.x = 308;
    result.y = 558;
    break;

    // Fighting
  case SDLK_j:
    client_log("DEBUG: fist");
    result.x = 1321;
    result.y = 536;
    break;
  case SDLK_k:
    client_log("DEBUG: kick");
    result.x = 1248;
    result.y = 603;
    break;
  case SDLK_l:
    client_log("DEBUG: star");
    result.x = 1259;
    result.y = 375;
    break;
  case SDLK_SPACE:
    client_log("DEBUG: ult");
    result.x = 1192;
    result.y = 522;
    break;
  }

  return result;
}
