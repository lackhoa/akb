# About

This is an app which allows me to play Android games with a real keyboard
on my PC (not sure about you :>).

Works by having a client running on the PC send messages to a server
running on the phone.

Bits and pieces derived from
[scrcpy](https://github.com/Genymobile/scrcpy).  Without referencing that
work, this project wouldn't be possible.

# Usage

Adjust key binding in [client/keymap.c](client/keymap.c).

- Basically you have to bind SDL key code to phone coordinates.
- SDL key codes reference [here](https://wiki.libsdl.org/SDL_Keycode).
- How to figure out the coordinate?
  I did that by modifying `scrcpy` to give me debug info about the coordinate.

USB cable connection to the phone (I don't think you can do wireless
because of the latency).

Build both client and server:

    ./build.sh

Launch the client and start playing:

    out/client

# Limitation

Only support tap inputs.

Have to rebuild the client, and relaunch the app to change the key binding.
