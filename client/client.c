#include "client.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <unistd.h>

#include <SDL2/SDL.h>

#include "keymap.c"

#define FORWARDING_PORT 6969;

void client_log(char *message) {
  printf("[client] "); printf("%s", message); printf("\n");
}

#define SERVER_UNKNOWN 0
#define SERVER_FAILED 1
int server_status = SERVER_UNKNOWN;

void *server_entry(void *argp) {
  client_log("Running server");
  char command[128];
  strcpy(command, "adb shell CLASSPATH=/data/local/tmp/classes.dex app_process / lackhoa.akb.Server");
  int return_code = system(command);
  if (return_code == -1 || return_code == 127) {
    client_log("[server_thread] Can't run the server");
    server_status = SERVER_FAILED;
  }

  client_log("[server_thread] Server has stopped");
  server_status = SERVER_FAILED;
  return NULL;
}

// Big-Endian conversion
void int_to_bytes(int i, unsigned char bytes[]) {
  bytes[0] = (i >> 24);
  bytes[1] = (i >> 16);
  bytes[2] = (i >> 8);
  bytes[3] = (i);
}

int main(int argc, char *argv[]) {
  int rc;
  char command[128];
  int return_code;

  client_log("adb push");
  strcpy(command, "adb push out/classes.dex /data/local/tmp/");
  return_code = system(command);
  if (return_code == -1 || return_code == 127) {
    client_log("adb push failed");
    return 1;
  }

  client_log("adb reverse");
  strcpy(command, "adb reverse localabstract:akb tcp:6969");
  return_code = system(command);
  if (return_code == -1 || return_code == 127) {
    client_log("adb forward failed");
    return 1;
  }

  int listen_sd = socket(AF_INET, SOCK_STREAM, 0);
  // #defer: close(listen_sd)
  if (listen_sd == -1) {
    client_log("Failed to create socket");
    return 1;
  }

  struct sockaddr_in sin;
  sin.sin_family = AF_INET;
#define IPV4_LOCALHOST ;
  sin.sin_addr.s_addr = htonl(0x7F000001);
  sin.sin_port = htons(6969);

  int on = 1;
  setsockopt(listen_sd, SOL_SOCKET, SO_REUSEADDR, (const void *) &on, sizeof(on));
  if (rc < 0) {
    client_log("ERROR: setsockopt() failed");
    close(listen_sd);
    return 1;
  }

  if (bind(listen_sd, (struct sockaddr *) &sin, sizeof(sin)) == -1) {
    client_log("ERROR: bind");
    close(listen_sd);
    return 1;
  }
  // backlog=1 since there's only ever one server connecting
  if (listen(listen_sd, 1) == -1) {
    client_log("ERROR: listen");
    close(listen_sd);
    return 1;
  }

  pthread_t server_thread;
  pthread_create(&server_thread, NULL, server_entry, NULL);

  // Blocking until server initialized & open connection request
  // TODO: Possible that server crashes -> we'll wait forever
  struct fd_set my_fd_set;
  FD_ZERO(&my_fd_set);
  FD_SET(listen_sd, &my_fd_set);

  struct timeval timeout;
  timeout.tv_sec = 3;  // seconds
  timeout.tv_usec = 0;

  client_log("INFO: Waiting for server to connect...");
  rc = select(listen_sd+1, &my_fd_set, NULL, NULL, &timeout);
  if (rc < 0) {
    client_log("ERROR: select() failed");
    close(listen_sd);
    return 1;
  }

  if (rc == 0) {
    client_log("select() timed out, something wrong with the server. Exiting.\n");
    close(listen_sd);
    return 1;
  }

  assert(rc == 1);
  int rx_sd = accept(listen_sd, NULL, NULL);
  // #defer: close(rx_sd)
  if (rx_sd < 0) {
    client_log("ERROR: accept");
    close(listen_sd);
    return 1;
  }
  client_log("DEBUG: Connection accepted");
  close(listen_sd);

  //Screen dimension constants
  const int SCREEN_WIDTH = 640;
  const int SCREEN_HEIGHT = 480;

  SDL_Window *window = NULL;
  SDL_Surface *screenSurface = NULL;
  if (SDL_Init(SDL_INIT_VIDEO) < 0) {
    printf("SDL could not initialize! SDL error: %s\n", SDL_GetError());
    close(rx_sd);
    return 1;
  }
  // #defer: SDL_Quit()  #note: not really sure

  window = SDL_CreateWindow("SDL Tutorial", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED,
                            SCREEN_WIDTH, SCREEN_HEIGHT, SDL_WINDOW_SHOWN);
  // #defer: SDL_DestroyWindow()
  if (window == NULL) {
    printf("ERROR: Window could not be created! SDL error: %s\n", SDL_GetError());
    SDL_Quit();
    close(rx_sd);
    return 1;
  }

  screenSurface = SDL_GetWindowSurface(window);
  SDL_FillRect(screenSurface, NULL, SDL_MapRGB(screenSurface->format, 0x0, 0x99, 0x99));
  SDL_UpdateWindowSurface(window);

  SDL_Event event;
  int quit = 0;

  while (!quit) {
    if (server_status == SERVER_FAILED) {
      client_log("ERROR: Server has stopped");
      SDL_DestroyWindow(window);
      SDL_Quit();
      close(rx_sd);
      return 1;
    }
    while (SDL_PollEvent(&event)) {
      if (event.type == SDL_QUIT) {
        quit = 1;
      }
      else if ((event.type == SDL_KEYDOWN || event.type == SDL_KEYUP)
               && !event.key.repeat) {
        Point point = key_to_point(event.key.keysym.sym);

        if (point.x != -1) {
          unsigned char event_type_bytes[4];
          unsigned char x_bytes[4];
          unsigned char y_bytes[4];

          int_to_bytes(event.type, event_type_bytes);
          int_to_bytes(point.x, x_bytes);
          int_to_bytes(point.y, y_bytes);

          // TODO: We need timeouts for these
          send(rx_sd, event_type_bytes, 4, 0);
          send(rx_sd, x_bytes,          4, 0);
          send(rx_sd, y_bytes,          4, 0);
        }
      }
    }
  }

  client_log("INFO: Quitting normally");
  close(rx_sd);
  SDL_DestroyWindow(window);
  SDL_Quit();

  return 0;
}
